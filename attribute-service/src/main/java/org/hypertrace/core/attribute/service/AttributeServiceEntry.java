package org.hypertrace.core.attribute.service;

import java.util.List;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.grpc.GrpcPlatformServerDefinition;
import org.hypertrace.core.serviceframework.grpc.StandAloneGrpcPlatformServiceContainer;

public class AttributeServiceEntry extends StandAloneGrpcPlatformServiceContainer {
  static final String PORT_PATH = "attributes.type.server.port";

  public AttributeServiceEntry(ConfigClient configClient) {
    super(configClient);
  }

  @Override
  protected List<GrpcPlatformServerDefinition> getServerDefinitions() {
    return List.of(
        GrpcPlatformServerDefinition.builder()
            .name(this.getServiceName())
            .port(this.getAppConfig().getInt(PORT_PATH))
            .serviceFactory(new AttributeServiceFactory())
            .build());
  }
}
