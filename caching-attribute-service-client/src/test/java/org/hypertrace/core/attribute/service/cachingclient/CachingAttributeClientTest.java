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
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.hypertrace.core.attribute.service.v1.AttributeCreateRequest;
import org.hypertrace.core.attribute.service.v1.AttributeMetadata;
import org.hypertrace.core.attribute.service.v1.AttributeMetadataFilter;
import org.hypertrace.core.attribute.service.v1.AttributeScope;
import org.hypertrace.core.attribute.service.v1.AttributeServiceGrpc.AttributeServiceImplBase;
import org.hypertrace.core.attribute.service.v1.Empty;
import org.hypertrace.core.attribute.service.v1.GetAttributesRequest;
import org.hypertrace.core.attribute.service.v1.GetAttributesResponse;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CachingAttributeClientTest {

  AttributeMetadata metadata1 =
      AttributeMetadata.newBuilder()
          .setScopeString(AttributeScope.EVENT.name())
          .setKey("first")
          .setId("first-id")
          .build();
  AttributeMetadata metadata2 =
      AttributeMetadata.newBuilder()
          .setScopeString(AttributeScope.EVENT.name())
          .setKey("second")
          .setId("second-id")
          .build();
  AttributeMetadata metadata3 =
      AttributeMetadata.newBuilder()
          .setScopeString(AttributeScope.EVENT.name())
          .setKey("second")
          .setId("second-id")
          .build();

  @Mock RequestContext mockContext;

  @Mock AttributeServiceImplBase mockAttributeService;

  CachingAttributeClient attributeClient;

  Server grpcServer;
  ManagedChannel grpcChannel;
  Context grpcTestContext;
  List<AttributeMetadata> responseMetadata;
  Optional<Throwable> responseError;
  AttributeMetadataFilter metadataFilter;

  @BeforeEach
  void beforeEach() throws IOException {
    String uniqueName = InProcessServerBuilder.generateName();
    this.grpcServer =
        InProcessServerBuilder.forName(uniqueName)
            .directExecutor() // directExecutor is fine for unit tests
            .addService(mockAttributeService)
            .build()
            .start();
    this.grpcChannel = InProcessChannelBuilder.forName(uniqueName).directExecutor().build();
    this.attributeClient = CachingAttributeClient.builder(this.grpcChannel).build();
    when(this.mockContext.getTenantId()).thenReturn(Optional.of("default tenant"));
    this.grpcTestContext = Context.current().withValue(RequestContext.CURRENT, this.mockContext);
    this.responseMetadata = List.of(this.metadata1, this.metadata2);
    this.responseError = Optional.empty();
    this.metadataFilter = AttributeMetadataFilter.getDefaultInstance();

    doAnswer(
            invocation -> {
              StreamObserver<GetAttributesResponse> observer =
                  invocation.getArgument(1, StreamObserver.class);
              responseError.ifPresentOrElse(
                  observer::onError,
                  () -> {
                    observer.onNext(
                        GetAttributesResponse.newBuilder()
                            .addAllAttributes(responseMetadata)
                            .build());
                    observer.onCompleted();
                  });
              return null;
            })
        .when(this.mockAttributeService)
        .getAttributes(any(), any());
  }

  @AfterEach
  void afterEach() {
    this.grpcServer.shutdownNow();
    this.grpcChannel.shutdownNow();
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
