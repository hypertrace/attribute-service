package org.hypertrace.core.attribute.service;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

import io.grpc.Deadline;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.health.v1.HealthGrpc.HealthBlockingStub;
import io.grpc.protobuf.services.HealthStatusManager;
import java.io.IOException;
import org.hypertrace.core.grpcutils.client.GrpcChannelRegistry;
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
  private HealthBlockingStub healthClient;
  private final HealthStatusManager healthStatusManager = new HealthStatusManager();
  private final GrpcChannelRegistry grpcChannelRegistry = new GrpcChannelRegistry();

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
    healthClient =
        HealthGrpc.newBlockingStub(this.grpcChannelRegistry.forPlaintextAddress("localhost", port));
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
    grpcChannelRegistry.shutdown(Deadline.after(10, SECONDS));
    ServerManagementUtil.shutdownServer(
        this.server, this.getServiceName(), Deadline.after(1, MINUTES));
  }

  @Override
  public boolean healthCheck() {
    try {
      return healthClient
          .withDeadlineAfter(2, SECONDS)
          .check(HealthCheckRequest.getDefaultInstance())
          .getStatus()
          .equals(ServingStatus.SERVING);
    } catch (Exception e) {
      LOGGER.debug("health check error", e);
      return false;
    }
  }

  @Override
  public String getServiceName() {
    return serviceName;
  }
}
