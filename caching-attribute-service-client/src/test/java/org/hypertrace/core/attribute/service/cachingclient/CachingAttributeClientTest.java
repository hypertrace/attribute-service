package org.hypertrace.core.attribute.service.cachingclient;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.grpc.Context;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.hypertrace.core.attribute.service.v1.AttributeCreateRequest;
import org.hypertrace.core.attribute.service.v1.AttributeMetadata;
import org.hypertrace.core.attribute.service.v1.AttributeMetadataFilter;
import org.hypertrace.core.attribute.service.v1.Empty;
import org.hypertrace.core.attribute.service.v1.GetAttributesRequest;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CachingAttributeClientTest extends CachingClientTestUtils {

  CachingAttributeClient attributeClient;

  @BeforeEach
  void beforeEach() throws Exception {
    setup();
    this.attributeClient = CachingAttributeClient.builder(this.grpcChannel).build();
  }

  @AfterEach
  void afterEach() {
    tearDown();
  }

  @Test
  void cachesConsecutiveGetAllCallsInSameContext() throws Exception {
    assertSame(
        this.metadata1,
        this.grpcTestContext.call(() -> this.attributeClient.get("EVENT", "first").blockingGet()));
    verify(this.mockAttributeService, times(1)).getAttributes(any(), any());
    verifyNoMoreInteractions(this.mockAttributeService);
    assertSame(
        this.metadata2,
        this.grpcTestContext.call(() -> this.attributeClient.get("EVENT", "second").blockingGet()));
  }

  @Test
  void throwsErrorIfNoKeyMatch() {
    assertThrows(
        NoSuchElementException.class,
        () ->
            this.grpcTestContext.run(
                () -> this.attributeClient.get("EVENT", "fake").blockingGet()));
  }

  @Test
  void supportsMultipleConcurrentCacheKeys() throws Exception {
    AttributeMetadata defaultRetrieved =
        this.grpcTestContext.call(() -> this.attributeClient.get("EVENT", "first").blockingGet());
    assertSame(this.metadata1, defaultRetrieved);
    verify(this.mockAttributeService, times(1)).getAttributes(any(), any());

    RequestContext otherMockContext = mock(RequestContext.class);
    when(otherMockContext.getTenantId()).thenReturn(Optional.of("other tenant"));
    Context otherGrpcContext =
        Context.current().withValue(RequestContext.CURRENT, otherMockContext);
    AttributeMetadata otherContextMetadata = AttributeMetadata.newBuilder(this.metadata1).build();

    this.responseMetadata = List.of(otherContextMetadata);

    AttributeMetadata otherRetrieved =
        otherGrpcContext.call(() -> this.attributeClient.get("EVENT", "first").blockingGet());
    assertSame(otherContextMetadata, otherRetrieved);
    assertNotSame(defaultRetrieved, otherRetrieved);
    verify(this.mockAttributeService, times(2)).getAttributes(any(), any());
    verifyNoMoreInteractions(this.mockAttributeService);

    assertSame(
        defaultRetrieved,
        this.grpcTestContext.call(() -> this.attributeClient.get("EVENT", "first").blockingGet()));

    assertSame(
        otherRetrieved,
        otherGrpcContext.call(() -> this.attributeClient.get("EVENT", "first").blockingGet()));
  }

  @Test
  void retriesOnError() throws Exception {
    this.responseError = Optional.of(new UnsupportedOperationException());

    assertThrows(
        StatusRuntimeException.class,
        () ->
            this.grpcTestContext.call(
                () -> this.attributeClient.get("EVENT", "first").blockingGet()));
    verify(this.mockAttributeService, times(1)).getAttributes(any(), any());

    this.responseError = Optional.empty();
    assertSame(
        this.metadata1,
        this.grpcTestContext.call(() -> this.attributeClient.get("EVENT", "first").blockingGet()));
    verify(this.mockAttributeService, times(2)).getAttributes(any(), any());
  }

  @Test
  void hasConfigurableCacheSize() throws Exception {
    this.attributeClient =
        CachingAttributeClient.builder(this.grpcChannel).withMaximumCacheContexts(1).build();

    RequestContext otherMockContext = mock(RequestContext.class);
    when(otherMockContext.getTenantId()).thenReturn(Optional.of("other tenant"));
    this.grpcTestContext.call(() -> this.attributeClient.get("EVENT", "first").blockingGet());

    // This call should evict the original call
    Context.current()
        .withValue(RequestContext.CURRENT, otherMockContext)
        .call(() -> this.attributeClient.get("EVENT", "first").blockingGet());

    // Rerunning this call now fire again, a third server call
    this.grpcTestContext.call(() -> this.attributeClient.get("EVENT", "first").blockingGet());
    verify(this.mockAttributeService, times(3)).getAttributes(any(), any());
  }

  @Test
  void supportsAppliedFilter() throws Exception {
    AttributeMetadataFilter attributeMetadataFilter =
        AttributeMetadataFilter.newBuilder().addScopeString("EVENT").build();
    this.attributeClient =
        CachingAttributeClient.builder(this.grpcChannel)
            .withAttributeFilter(attributeMetadataFilter)
            .build();
    this.grpcTestContext.call(() -> this.attributeClient.get("EVENT", "first").blockingGet());
    verify(this.mockAttributeService, times(1))
        .getAttributes(
            eq(GetAttributesRequest.newBuilder().setFilter(attributeMetadataFilter).build()),
            any());
  }

  @Test
  void supportsCachedLookupById() throws Exception {
    assertSame(
        this.metadata1,
        this.grpcTestContext.call(() -> this.attributeClient.get("first-id").blockingGet()));
    verify(this.mockAttributeService, times(1)).getAttributes(any(), any());
    verifyNoMoreInteractions(this.mockAttributeService);
    assertSame(
        this.metadata2,
        this.grpcTestContext.call(() -> this.attributeClient.get("second-id").blockingGet()));
  }

  @Test
  void sharesIdAndKeyCache() throws Exception {
    assertSame(
        this.metadata1,
        this.grpcTestContext.call(() -> this.attributeClient.get("first-id").blockingGet()));
    verify(this.mockAttributeService, times(1)).getAttributes(any(), any());
    verifyNoMoreInteractions(this.mockAttributeService);
    assertSame(
        this.metadata1,
        this.grpcTestContext.call(() -> this.attributeClient.get("EVENT", "first").blockingGet()));
  }

  @Test
  void throwsErrorIfNoIdMatch() {
    assertThrows(
        NoSuchElementException.class,
        () -> this.grpcTestContext.run(() -> this.attributeClient.get("fakeId").blockingGet()));
  }

  @Test
  void getsAllAttributesInScope() throws Exception {
    assertEquals(
        this.responseMetadata,
        this.grpcTestContext.call(() -> this.attributeClient.getAllInScope("EVENT").blockingGet()));

    assertEquals(
        emptyList(),
        this.grpcTestContext.call(
            () -> this.attributeClient.getAllInScope("DOESNT_EXIST").blockingGet()));
  }

  @Test
  void createInvalidatesCache() throws Exception {
    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              final StreamObserver<Empty> observer =
                  invocation.getArgument(1, StreamObserver.class);
              responseError.ifPresentOrElse(
                  observer::onError,
                  () -> {
                    observer.onNext(Empty.getDefaultInstance());
                    observer.onCompleted();
                  });
              return null;
            })
        .when(this.mockAttributeService)
        .create(any(), any());

    this.grpcTestContext.call(() -> this.attributeClient.getAllInScope("EVENT").blockingGet());
    this.grpcTestContext.call(
        () -> this.attributeClient.create(List.of(metadata3)).blockingAwait(1, TimeUnit.SECONDS));
    verify(this.mockAttributeService, times(1))
        .create(eq(AttributeCreateRequest.newBuilder().addAttributes(metadata3).build()), any());
    this.grpcTestContext.call(() -> this.attributeClient.getAllInScope("EVENT").blockingGet());

    // Called once before create and once after
    verify(this.mockAttributeService, times(2)).getAttributes(any(), any());
    verifyNoMoreInteractions(this.mockAttributeService);
  }

  @Test
  void deleteInvalidatesCache() throws Exception {
    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              final StreamObserver<Empty> observer =
                  invocation.getArgument(1, StreamObserver.class);
              responseError.ifPresentOrElse(
                  observer::onError,
                  () -> {
                    observer.onNext(Empty.getDefaultInstance());
                    observer.onCompleted();
                  });
              return null;
            })
        .when(this.mockAttributeService)
        .delete(any(), any());

    this.grpcTestContext.call(() -> this.attributeClient.getAllInScope("EVENT").blockingGet());
    this.grpcTestContext.call(
        () -> this.attributeClient.delete(metadataFilter).blockingAwait(1, TimeUnit.SECONDS));
    verify(this.mockAttributeService, times(1)).delete(same(metadataFilter), any());
    this.grpcTestContext.call(() -> this.attributeClient.getAllInScope("EVENT").blockingGet());

    // Called once before delete and once after
    verify(this.mockAttributeService, times(2)).getAttributes(any(), any());
    verifyNoMoreInteractions(this.mockAttributeService);
  }
}
