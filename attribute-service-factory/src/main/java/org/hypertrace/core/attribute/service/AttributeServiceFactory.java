package org.hypertrace.core.attribute.service;

import java.util.List;
import org.hypertrace.core.serviceframework.grpc.GrpcPlatformService;
import org.hypertrace.core.serviceframework.grpc.GrpcPlatformServiceFactory;
import org.hypertrace.core.serviceframework.grpc.GrpcServiceContainerEnvironment;

public class AttributeServiceFactory implements GrpcPlatformServiceFactory {
  private static final String SERVICE_NAME = "attribute-service";

  @Override
  public List<GrpcPlatformService> buildServices(GrpcServiceContainerEnvironment environment) {
    return List.of(
        new GrpcPlatformService(
            new AttributeServiceImpl(
                environment.getConfig(SERVICE_NAME), environment.getLifecycle())));
  }
}
