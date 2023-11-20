package org.hypertrace.core.attribute.service.cachingclient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Optional;
import org.hypertrace.core.attribute.service.v1.AttributeMetadata;
import org.hypertrace.core.attribute.service.v1.AttributeMetadataFilter;
import org.hypertrace.core.attribute.service.v1.AttributeScope;
import org.hypertrace.core.attribute.service.v1.AttributeServiceGrpc;
import org.hypertrace.core.attribute.service.v1.GetAttributesResponse;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CachingClientTestUtils {
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
  List<AttributeMetadata> responseMetadata = List.of(this.metadata1, this.metadata2);

  @Mock RequestContext mockContext;

  @Mock AttributeServiceGrpc.AttributeServiceImplBase mockAttributeService;

  Server grpcServer;
  ManagedChannel grpcChannel;
  Context grpcTestContext;
  Optional<Throwable> responseError;
  AttributeMetadataFilter metadataFilter;

  void setup() throws Exception {
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
    when(this.mockContext.getTenantId()).thenReturn(Optional.of("default tenant"));
    this.grpcTestContext = Context.current().withValue(RequestContext.CURRENT, this.mockContext);
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

  void tearDown() {
    this.grpcServer.shutdownNow();
    this.grpcChannel.shutdownNow();
  }
}
