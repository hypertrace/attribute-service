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
import org.hypertrace.core.grpcutils.context.ContextualKey;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.hypertrace.core.serviceframework.metrics.PlatformMetricsRegistry;

@Slf4j
public class AttributeServiceCachedClient {
  private final LoadingCache<ContextualKey<Void>, Table<String, String, AttributeMetadata>> cache;
  private final Cache<String, AttributeScopeAndKey> scopeAndKeyLookup;
  private final AttributeServiceBlockingStub attributeServiceBlockingStub;
  private final long deadlineMs;
  private final ClientCallCredentialsProvider callCredentialsProvider;

  public AttributeServiceCachedClient(
      Channel channel, AttributeServiceCachedClientConfig clientConfig) {
    this(
        channel,
        clientConfig,
        RequestContextClientCallCredsProviderFactory.getClientCallCredsProvider());
  }

  public AttributeServiceCachedClient(
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
            .expireAfterAccess(clientConfig.getExpireAfterAccess())
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
      @Nonnull String attributeKey) {
    return Optional.ofNullable(getTable(requestContext).get(attributeScope, attributeKey));
  }

  public Optional<AttributeMetadata> getById(
      @Nonnull RequestContext requestContext, @Nonnull String attributeId) {
    Table<String, String, AttributeMetadata> table = getTable(requestContext);
    return Optional.ofNullable(scopeAndKeyLookup.getIfPresent(attributeId))
        .map(scopeAndKey -> table.get(scopeAndKey.getScope(), scopeAndKey.getKey()));
  }

  public List<AttributeMetadata> getAllInScope(
      @Nonnull RequestContext requestContext, @Nonnull String attributeScope) {
    return List.copyOf(getTable(requestContext).row(attributeScope).values());
  }

  private Table<String, String, AttributeMetadata> getTable(RequestContext requestContext) {
    return cache.getUnchecked(requestContext.buildInternalContextualKey());
  }

  private Table<String, String, AttributeMetadata> loadTable(ContextualKey<Void> contextualKey) {
    List<AttributeMetadata> attributeMetadataList =
        contextualKey
            .getContext()
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
