package org.hypertrace.core.attribute.service;

import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.hypertrace.core.serviceframework.grpc.GrpcPlatformServiceFactory;
import org.hypertrace.core.serviceframework.grpc.StandAloneGrpcPlatformServiceContainer;

public class AttributeServiceEntry extends StandAloneGrpcPlatformServiceContainer {
  static final String PORT_PATH = "attributes.type.server.port";

  public AttributeServiceEntry(ConfigClient configClient) {
    super(configClient);
  }

  @Override
  public GrpcPlatformServiceFactory getServiceFactory() {
    return new AttributeServiceFactory();
  }

  @Override
  protected int getServicePort() {
    return getAppConfig().getInt(PORT_PATH);
  }
}
