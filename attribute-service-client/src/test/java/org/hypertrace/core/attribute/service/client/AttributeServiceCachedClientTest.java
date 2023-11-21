package org.hypertrace.core.attribute.service.client;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.typesafe.config.ConfigFactory;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.hypertrace.core.attribute.service.client.config.AttributeServiceCachedClientConfig;
import org.hypertrace.core.attribute.service.v1.AttributeMetadata;
import org.hypertrace.core.attribute.service.v1.AttributeMetadataFilter;
import org.hypertrace.core.attribute.service.v1.AttributeScope;
import org.hypertrace.core.attribute.service.v1.AttributeServiceGrpc;
import org.hypertrace.core.attribute.service.v1.GetAttributesResponse;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttributeServiceCachedClientTest {
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

  @Mock RequestContext requestContext;

  @Mock AttributeServiceGrpc.AttributeServiceImplBase mockAttributeService;
  Server grpcServer;
  ManagedChannel grpcChannel;
  List<AttributeMetadata> responseMetadata;
  Optional<Throwable> responseError;
  AttributeMetadataFilter metadataFilter;
  AttributeServiceCachedClient attributeServiceCachedClient;

  @BeforeEach
  void beforeEach() throws IOException {
    when(mockAttributeService.bindService())
        .thenReturn(AttributeServiceGrpc.bindService(mockAttributeService));
    String uniqueName = InProcessServerBuilder.generateName();
    this.grpcServer =
        InProcessServerBuilder.forName(uniqueName)
            .directExecutor() // directExecutor is fine for unit tests
            .addService(mockAttributeService)
            .build()
            .start();
    this.grpcChannel = InProcessChannelBuilder.forName(uniqueName).directExecutor().build();
    this.attributeServiceCachedClient =
        new AttributeServiceCachedClient(
            grpcChannel, AttributeServiceCachedClientConfig.from(ConfigFactory.empty()));
    lenient().when(this.requestContext.getTenantId()).thenReturn(Optional.of("default tenant"));
    this.responseMetadata = List.of(this.metadata1, this.metadata2);
    this.responseError = Optional.empty();
    this.metadataFilter = AttributeMetadataFilter.getDefaultInstance();

    lenient()
        .doAnswer(
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
        this.attributeServiceCachedClient.get(requestContext, "EVENT", "first").get());
    verify(this.mockAttributeService, times(1)).getAttributes(any(), any());
    verifyNoMoreInteractions(this.mockAttributeService);
    assertSame(
        this.metadata2,
        this.attributeServiceCachedClient.get(requestContext, "EVENT", "second").get());
  }

  @Test
  void returnEmptyIfNoMatch() throws Exception {
    assertTrue(this.attributeServiceCachedClient.get(requestContext, "EVENT", "fake").isEmpty());
  }

  @Test
  void throwsErrorIfNoTenantIdInContext() {
    assertThrows(
        ExecutionException.class,
        () -> this.attributeServiceCachedClient.get(new RequestContext(), "EVENT", "fake"));
  }

  @Test
  void supportsMultipleConcurrentCacheKeys() throws Exception {
    AttributeMetadata defaultRetrieved =
        this.attributeServiceCachedClient.get(requestContext, "EVENT", "first").get();
    assertSame(this.metadata1, defaultRetrieved);
    verify(this.mockAttributeService, times(1)).getAttributes(any(), any());

    RequestContext otherRequestContext = mock(RequestContext.class);
    when(otherRequestContext.getTenantId()).thenReturn(Optional.of("other tenant"));
    AttributeMetadata otherContextMetadata = AttributeMetadata.newBuilder(this.metadata1).build();

    this.responseMetadata = List.of(otherContextMetadata);

    AttributeMetadata otherRetrieved =
        this.attributeServiceCachedClient.get(otherRequestContext, "EVENT", "first").get();
    assertSame(otherContextMetadata, otherRetrieved);
    assertNotSame(defaultRetrieved, otherRetrieved);
    verify(this.mockAttributeService, times(2)).getAttributes(any(), any());
    verifyNoMoreInteractions(this.mockAttributeService);

    assertSame(
        defaultRetrieved,
        this.attributeServiceCachedClient.get(requestContext, "EVENT", "first").get());

    assertSame(
        otherRetrieved,
        this.attributeServiceCachedClient.get(otherRequestContext, "EVENT", "first").get());
  }

  @Test
  void supportsCachedLookupById() throws Exception {
    assertSame(
        this.metadata1,
        this.attributeServiceCachedClient.getById(requestContext, "first-id").get());
    verify(this.mockAttributeService, times(1)).getAttributes(any(), any());
    verifyNoMoreInteractions(this.mockAttributeService);
    assertSame(
        this.metadata2,
        this.attributeServiceCachedClient.getById(requestContext, "second-id").get());
  }

  @Test
  void sharesIdAndKeyCache() throws Exception {
    assertSame(
        this.metadata1,
        this.attributeServiceCachedClient.getById(requestContext, "first-id").get());
    verify(this.mockAttributeService, times(1)).getAttributes(any(), any());
    verifyNoMoreInteractions(this.mockAttributeService);
    assertSame(
        this.metadata1,
        this.attributeServiceCachedClient.get(requestContext, "EVENT", "first").get());
  }

  @Test
  void emptyIfNoIdMatch() throws Exception {
    assertTrue(this.attributeServiceCachedClient.getById(requestContext, "fakeId").isEmpty());
  }

  @Test
  void getsAllAttributesInScope() throws Exception {
    assertEquals(
        this.responseMetadata,
        this.attributeServiceCachedClient.getAllInScope(requestContext, "EVENT"));

    assertEquals(
        emptyList(),
        this.attributeServiceCachedClient.getAllInScope(requestContext, "DOESNT_EXIST"));
  }
}
