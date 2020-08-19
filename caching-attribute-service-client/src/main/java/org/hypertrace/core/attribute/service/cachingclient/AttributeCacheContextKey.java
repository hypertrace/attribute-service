package org.hypertrace.core.attribute.service.cachingclient;

import static java.util.Objects.isNull;

import io.grpc.stub.StreamObserver;
import io.reactivex.rxjava3.core.Observable;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.hypertrace.core.grpcutils.client.GrpcClientRequestContextUtil;
import org.hypertrace.core.grpcutils.context.RequestContext;

class AttributeCacheContextKey {
  static AttributeCacheContextKey forCurrentContext() {
    return forContext(RequestContext.CURRENT.get());
  }

  static AttributeCacheContextKey forContext(RequestContext context) {
    assert !isNull(context) : "RequestContext must be set to use Attribute Client";
    return new AttributeCacheContextKey(context);
  }

  private static final String DEFAULT_IDENTITY = "default";

  private final RequestContext requestContext;
  private final String identity;

  private AttributeCacheContextKey(@Nonnull RequestContext requestContext) {
    this.requestContext = requestContext;
    this.identity = requestContext.getTenantId().orElse(DEFAULT_IDENTITY);
  }

  public <T> Observable<T> streamInContext(Consumer<StreamObserver<T>> requestExecutor) {
    return Observable.create(
        emitter ->
            this.runInContext(
                () -> requestExecutor.accept(new StreamingClientResponseObserver<>(emitter))));
  }

  private void runInContext(Runnable runnable) {
    GrpcClientRequestContextUtil.executeWithHeadersContext(
        requestContext.getRequestHeaders(), runnable);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AttributeCacheContextKey that = (AttributeCacheContextKey) o;
    return identity.equals(that.identity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identity);
  }
}
