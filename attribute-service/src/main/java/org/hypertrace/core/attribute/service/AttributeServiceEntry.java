package org.hypertrace.core.attribute.service;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.HealthStatusManager;
import java.io.IOException;
import java.time.Duration;
import org.hypertrace.core.grpcutils.server.InterceptorUtil;
import org.hypertrace.core.grpcutils.server.ServerManagementUtil;
import org.hypertrace.core.serviceframework.PlatformService;
import org.hypertrace.core.serviceframework.config.ConfigClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributeServiceEntry extends PlatformService {
  private static final String SERVICE_NAME_CONFIG = "service.name";
  private static final Logger LOGGER = LoggerFactory.getLogger(AttributeServiceEntry.class);
  static final String PORT_PATH = "attributes.type.server.port";

  private String serviceName;
  private Server server;
  private final HealthStatusManager healthStatusManager = new HealthStatusManager();

  public AttributeServiceEntry(ConfigClient configClient) {
    super(configClient);
  }

  @Override
  protected void doInit() {
    serviceName = getAppConfig().getString(SERVICE_NAME_CONFIG);
    int port = getAppConfig().getInt(PORT_PATH);
    server =
        ServerBuilder.forPort(port)
            .addService(InterceptorUtil.wrapInterceptors(new AttributeServiceImpl(getAppConfig())))
            .addService(healthStatusManager.getHealthService())
            .build();
  }

  @Override
  protected void doStart() {
    try {
      try {
        server.start();
      } catch (IOException e) {
        LOGGER.error("Unable to start server");
        throw new RuntimeException(e);
      }
      server.awaitTermination();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void doStop() {
    healthStatusManager.enterTerminalState();
    ServerManagementUtil.shutdownServer(this.server, this.getServiceName(), Duration.ofMinutes(1));
  }

  @Override
  public boolean healthCheck() {
    throw new UnsupportedOperationException("Please use grpc health check");
  }

  @Override
  public String getServiceName() {
    return serviceName;
  }
}
