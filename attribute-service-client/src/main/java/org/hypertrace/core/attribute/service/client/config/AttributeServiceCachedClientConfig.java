package org.hypertrace.core.attribute.service.client.config;

import com.typesafe.config.Config;
import java.time.Duration;
import lombok.Value;
import org.hypertrace.core.attribute.service.client.AttributeServiceCachedClient;

@Value
public class AttributeServiceCachedClientConfig {
  private static final String DEADLINE_CONFIG_KEY = "deadline";
  private static final String CACHE_MAX_SIZE_CONFIG_KEY = "maxSize";
  private static final String CACHE_REFRESH_AFTER_WRITE_CONFIG_KEY = "refreshAfterWriteDuration";
  private static final String CACHE_EXPIRE_AFTER_ACCESS_CONFIG_KEY = "expireAfterAccessDuration";
  private static final String CACHE_EXECUTOR_THREADS_CONFIG_KEY = "executorThreads";

  Duration deadline;
  long maxSize;
  Duration refreshAfterWrite;
  Duration expireAfterAccess;
  int executorThreads;
  String cacheMetricsName;

  public static AttributeServiceCachedClientConfig from(Config attributeServiceConfig) {
    return from(attributeServiceConfig, AttributeServiceCachedClient.class.getName());
  }

  public static AttributeServiceCachedClientConfig from(
      Config attributeServiceConfig, String cacheMetricsName) {
    Duration deadline =
        attributeServiceConfig.hasPath(DEADLINE_CONFIG_KEY)
            ? attributeServiceConfig.getDuration(DEADLINE_CONFIG_KEY)
            : Duration.ofSeconds(30);
    long maxSize =
        attributeServiceConfig.hasPath(CACHE_MAX_SIZE_CONFIG_KEY)
            ? attributeServiceConfig.getLong(CACHE_MAX_SIZE_CONFIG_KEY)
            : 1000;
    Duration refreshAfterWrite =
        attributeServiceConfig.hasPath(CACHE_REFRESH_AFTER_WRITE_CONFIG_KEY)
            ? attributeServiceConfig.getDuration(CACHE_REFRESH_AFTER_WRITE_CONFIG_KEY)
            : Duration.ofMinutes(15);
    Duration expireAfterWrite =
        attributeServiceConfig.hasPath(CACHE_EXPIRE_AFTER_ACCESS_CONFIG_KEY)
            ? attributeServiceConfig.getDuration(CACHE_EXPIRE_AFTER_ACCESS_CONFIG_KEY)
            : Duration.ofHours(1);
    int executorThreads =
        attributeServiceConfig.hasPath(CACHE_EXECUTOR_THREADS_CONFIG_KEY)
            ? attributeServiceConfig.getInt(CACHE_EXECUTOR_THREADS_CONFIG_KEY)
            : 1;
    return new AttributeServiceCachedClientConfig(
        deadline, maxSize, refreshAfterWrite, expireAfterWrite, executorThreads, cacheMetricsName);
  }
}
