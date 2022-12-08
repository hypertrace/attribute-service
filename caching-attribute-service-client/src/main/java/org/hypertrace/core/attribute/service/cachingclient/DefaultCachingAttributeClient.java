package org.hypertrace.core.attribute.service.cachingclient;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.RateLimiter;
import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hypertrace.core.attribute.service.v1.AttributeCreateRequest;
import org.hypertrace.core.attribute.service.v1.AttributeMetadata;
import org.hypertrace.core.attribute.service.v1.AttributeMetadataFilter;
import org.hypertrace.core.attribute.service.v1.AttributeServiceGrpc;
import org.hypertrace.core.attribute.service.v1.AttributeServiceGrpc.AttributeServiceStub;
import org.hypertrace.core.attribute.service.v1.Empty;
import org.hypertrace.core.attribute.service.v1.GetAttributesRequest;
import org.hypertrace.core.attribute.service.v1.GetAttributesResponse;
import org.hypertrace.core.attribute.service.v1.Update;
import org.hypertrace.core.attribute.service.v1.UpdateMetadataRequest;
import org.hypertrace.core.attribute.service.v1.UpdateMetadataResponse;

@Slf4j
class DefaultCachingAttributeClient implements CachingAttributeClient {
  // One log a minute
  private static final RateLimiter LOGGING_LIMITER = RateLimiter.create(1 / 60d);
  private final AttributeServiceStub attributeServiceClient;
  private final LoadingCache<
          AttributeCacheContextKey, Single<Table<String, String, AttributeMetadata>>>
      cache;
  private final Cache<String, AttributeScopeAndKey> scopeAndKeyLookup;
  private final AttributeMetadataFilter attributeFilter;

  DefaultCachingAttributeClient(
      @Nonnull Channel channel,
      @Nonnull CallCredentials credentials,
      int maxCacheContexts,
      @Nonnull Duration cacheExpiration,
      @Nonnull AttributeMetadataFilter attributeFilter) {

    this.attributeFilter = attributeFilter;
    this.attributeServiceClient =
        AttributeServiceGrpc.newStub(channel).withCallCredentials(credentials);
    this.cache =
        CacheBuilder.newBuilder()
            .maximumSize(maxCacheContexts)
            .expireAfterWrite(cacheExpiration)
            .build(CacheLoader.from(this::loadTable));
    this.scopeAndKeyLookup = CacheBuilder.newBuilder().expireAfterWrite(cacheExpiration).build();
  }

  @Override
  public Single<AttributeMetadata> get(String scope, String key) {
    return this.getOrInvalidate(AttributeCacheContextKey.forCurrentContext())
        .mapOptional(table -> Optional.ofNullable(table.get(scope, key)))
        .switchIfEmpty(
            buildAndLogErrorLazily(
                "No attribute available for scope '%s' and key '%s'", scope, key));
  }

  @Override
  public Single<AttributeMetadata> get(String attributeId) {
    return this.getOrInvalidate(AttributeCacheContextKey.forCurrentContext())
        .mapOptional(
            table ->
                Optional.ofNullable(this.scopeAndKeyLookup.getIfPresent(attributeId))
                    .map(scopeAndKey -> table.get(scopeAndKey.scope, scopeAndKey.key)))
        .switchIfEmpty(buildAndLogErrorLazily("No attribute available for id '%s'", attributeId));
  }

  @Override
  public Single<List<AttributeMetadata>> getAllInScope(String scope) {
    return this.getOrInvalidate(AttributeCacheContextKey.forCurrentContext())
        .map(table -> List.copyOf(table.row(scope).values()));
  }

  @Override
  public Single<List<AttributeMetadata>> getAll() {
    return this.getOrInvalidate(AttributeCacheContextKey.forCurrentContext())
        .map(table -> List.copyOf(table.values()));
  }

  @Override
  public Completable create(final Collection<AttributeMetadata> attributeMetadata) {
    final AttributeCacheContextKey key = AttributeCacheContextKey.forCurrentContext();
    return key.getExecutionContext().<Empty>stream(
            streamObserver ->
                this.attributeServiceClient.create(
                    AttributeCreateRequest.newBuilder().addAllAttributes(attributeMetadata).build(),
                    streamObserver))
        .doOnNext(empty -> cache.invalidate(key))
        .ignoreElements();
  }

  @Override
  public Completable delete(final AttributeMetadataFilter filter) {
    final AttributeCacheContextKey key = AttributeCacheContextKey.forCurrentContext();
    return key.getExecutionContext().<Empty>stream(
            streamObserver -> this.attributeServiceClient.delete(filter, streamObserver))
        .doOnNext(empty -> cache.invalidate(key))
        .ignoreElements();
  }

  @Override
  public Single<AttributeMetadata> update(final String id, final Collection<Update> updates) {
    final UpdateMetadataRequest request =
        UpdateMetadataRequest.newBuilder().setAttributeId(id).addAllUpdates(updates).build();
    final AttributeCacheContextKey key = AttributeCacheContextKey.forCurrentContext();
    return key.getExecutionContext().<UpdateMetadataResponse>stream(
            streamObserver -> this.attributeServiceClient.updateMetadata(request, streamObserver))
        .doOnNext(response -> cache.invalidate(key))
        .map(UpdateMetadataResponse::getAttribute)
        .firstOrError();
  }

  private Single<Table<String, String, AttributeMetadata>> loadTable(AttributeCacheContextKey key) {
    return key.getExecutionContext().<GetAttributesResponse>stream(
            streamObserver ->
                this.attributeServiceClient.getAttributes(
                    GetAttributesRequest.newBuilder().setFilter(this.attributeFilter).build(),
                    streamObserver))
        .flatMapIterable(GetAttributesResponse::getAttributesList)
        .doOnNext(this::loadScopeAndKeyCache)
        .toList()
        .map(this::buildTable)
        .cache();
  }

  private Table<String, String, AttributeMetadata> buildTable(List<AttributeMetadata> attributes) {
    return attributes.stream()
        .collect(
            ImmutableTable.toImmutableTable(
                AttributeMetadata::getScopeString, AttributeMetadata::getKey, Function.identity()));
  }

  private Single<Table<String, String, AttributeMetadata>> getOrInvalidate(
      AttributeCacheContextKey key) {
    return this.cache.getUnchecked(key).doOnError(x -> this.cache.invalidate(key));
  }

  private void loadScopeAndKeyCache(AttributeMetadata attributeMetadata) {
    this.scopeAndKeyLookup.put(
        attributeMetadata.getId(),
        new AttributeScopeAndKey(attributeMetadata.getScopeString(), attributeMetadata.getKey()));
  }

  private <T> Single<T> buildAndLogErrorLazily(String message, Object... args) {
    return Single.error(
        () -> {
          if (LOGGING_LIMITER.tryAcquire()) {
            log.error(String.format(message, args));
          }
          return new NoSuchElementException(String.format(message, args));
        });
  }

  private static final class AttributeScopeAndKey {
    private final String scope;
    private final String key;

    private AttributeScopeAndKey(String scope, String key) {
      this.scope = scope;
      this.key = key;
    }
  }
}
