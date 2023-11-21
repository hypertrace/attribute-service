package org.hypertrace.core.attribute.service.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.Channel;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.core.attribute.service.client.config.AttributeServiceCachedClientConfig;
import org.hypertrace.core.attribute.service.v1.AttributeMetadata;
import org.hypertrace.core.attribute.service.v1.AttributeServiceGrpc;
import org.hypertrace.core.attribute.service.v1.AttributeServiceGrpc.AttributeServiceBlockingStub;
import org.hypertrace.core.attribute.service.v1.GetAttributesRequest;
import org.hypertrace.core.grpcutils.client.ClientCallCredentialsProvider;
import org.hypertrace.core.grpcutils.client.RequestContextClientCallCredsProviderFactory;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.hypertrace.core.serviceframework.metrics.PlatformMetricsRegistry;

@Slf4j
public class AttributeServiceCachedClient {
  private final LoadingCache<String, Table<String, String, AttributeMetadata>> cache;
  private final Cache<String, AttributeScopeAndKey> scopeAndKeyLookup;
  private final AttributeServiceBlockingStub attributeServiceBlockingStub;
  private final long deadlineMs;
  private final ClientCallCredentialsProvider callCredentialsProvider;

  AttributeServiceCachedClient(Channel channel, AttributeServiceCachedClientConfig clientConfig) {
    this(
        channel,
        clientConfig,
        RequestContextClientCallCredsProviderFactory.getClientCallCredsProvider());
  }

  AttributeServiceCachedClient(
      Channel channel,
      AttributeServiceCachedClientConfig clientConfig,
      ClientCallCredentialsProvider callCredentialsProvider) {
    this.callCredentialsProvider = callCredentialsProvider;
    this.attributeServiceBlockingStub = AttributeServiceGrpc.newBlockingStub(channel);
    deadlineMs = clientConfig.getDeadline().toMillis();
    cache =
        CacheBuilder.newBuilder()
            .maximumSize(clientConfig.getMaxSize())
            .refreshAfterWrite(clientConfig.getRefreshAfterWrite())
            .expireAfterWrite(clientConfig.getExpireAfterAccess())
            .build(
                CacheLoader.asyncReloading(
                    CacheLoader.from(this::loadTable),
                    Executors.newFixedThreadPool(
                        clientConfig.getExecutorThreads(), this.buildThreadFactory())));
    PlatformMetricsRegistry.registerCache(
        clientConfig.getCacheMetricsName(), cache, Collections.emptyMap());
    scopeAndKeyLookup =
        CacheBuilder.newBuilder().expireAfterWrite(clientConfig.getExpireAfterAccess()).build();
  }

  public Optional<AttributeMetadata> get(
      @Nonnull RequestContext requestContext,
      @Nonnull String attributeScope,
      @Nonnull String attributeKey)
      throws ExecutionException {
    try {
      return getTableForRequestContext(requestContext)
          .map(table -> table.get(attributeScope, attributeKey));
    } catch (NullPointerException e) {
      log.debug("No attribute available for scope {} and key {}", attributeScope, attributeKey);
      return Optional.empty();
    }
  }

  public Optional<AttributeMetadata> getById(
      @Nonnull RequestContext requestContext, @Nonnull String attributeId)
      throws ExecutionException {
    try {
      return getTableForRequestContext(requestContext)
          .map(
              table -> {
                AttributeScopeAndKey scopeAndKey = scopeAndKeyLookup.getIfPresent(attributeId);
                return table.get(scopeAndKey.getScope(), scopeAndKey.getKey());
              });
    } catch (NullPointerException e) {
      log.debug("No attribute available for id {}", attributeId);
      return Optional.empty();
    }
  }

  public List<AttributeMetadata> getAllInScope(
      @Nonnull RequestContext requestContext, @Nonnull String attributeScope)
      throws ExecutionException {
    return getTableForRequestContext(requestContext)
        .map(table -> List.copyOf(table.row(attributeScope).values()))
        .orElse(Collections.emptyList());
  }

  private Optional<Table<String, String, AttributeMetadata>> getTableForRequestContext(
      RequestContext requestContext) throws ExecutionException {
    return Optional.of(
        cache.get(
            requestContext
                .getTenantId()
                .orElseThrow(
                    () ->
                        new ExecutionException(
                            "No tenant id present in request context",
                            new UnsupportedOperationException()))));
  }

  private Table<String, String, AttributeMetadata> loadTable(String tenantId) {
    List<AttributeMetadata> attributeMetadataList =
        RequestContext.forTenantId(tenantId)
            .call(
                () ->
                    attributeServiceBlockingStub
                        .withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
                        .withCallCredentials(callCredentialsProvider.get())
                        .getAttributes(GetAttributesRequest.getDefaultInstance()))
            .getAttributesList();
    attributeMetadataList.forEach(
        attributeMetadata ->
            scopeAndKeyLookup.put(
                attributeMetadata.getId(),
                new AttributeScopeAndKey(
                    attributeMetadata.getScopeString(), attributeMetadata.getKey())));
    return attributeMetadataList.stream()
        .collect(
            ImmutableTable.toImmutableTable(
                AttributeMetadata::getScopeString, AttributeMetadata::getKey, Function.identity()));
  }

  private ThreadFactory buildThreadFactory() {
    return new ThreadFactoryBuilder()
        .setDaemon(true)
        .setNameFormat("attribute-service-cached-client-%d")
        .build();
  }

  @Value
  private static class AttributeScopeAndKey {
    String scope;
    String key;
  }
}
