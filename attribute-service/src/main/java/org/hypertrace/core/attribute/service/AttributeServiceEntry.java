package org.hypertrace.core.attribute.service;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.health.v1.HealthGrpc.HealthBlockingStub;
import io.grpc.protobuf.services.HealthStatusManager;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
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
  private final Clock clock = Clock.systemUTC();
  private final HealthStatusManager healthStatusManager = new HealthStatusManager();
  private final GrpcChannelRegistry grpcChannelRegistry = new GrpcChannelRegistry(this.clock);

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
    grpcChannelRegistry.shutdown(this.clock.instant().plus(10L, ChronoUnit.SECONDS));
    ServerManagementUtil.shutdownServer(this.server, this.getServiceName(), Duration.ofMinutes(1));
  }

  @Override
  public boolean healthCheck() {
    return healthClient
        .check(HealthCheckRequest.getDefaultInstance())
        .getStatus()
        .equals(ServingStatus.SERVING);
  }

  @Override
  public String getServiceName() {
    return serviceName;
  }
}
