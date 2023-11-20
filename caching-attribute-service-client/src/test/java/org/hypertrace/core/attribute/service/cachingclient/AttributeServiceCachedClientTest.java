package org.hypertrace.core.attribute.service.cachingclient;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttributeServiceCachedClientTest extends CachingClientTestUtils {
  AttributeServiceCachedClient attributeClient;

  @BeforeEach
  void beforeEach() throws Exception {
    setup();
    attributeClient = new AttributeServiceCachedClient(grpcChannel, ConfigFactory.empty());
  }

  @AfterEach
  void afterEach() {
    tearDown();
  }

  @Test
  void get() {
    assertEquals(
        this.metadata1,
        this.attributeClient.get(mockContext.getTenantId().get(), "EVENT", "first").get());
    verify(this.mockAttributeService, times(1)).getAttributes(any(), any());
    verifyNoMoreInteractions(this.mockAttributeService);
    assertEquals(
        this.metadata2,
        this.attributeClient.get(mockContext.getTenantId().get(), "EVENT", "second").get());
  }

  @Test
  void getById() {
    assertEquals(
        this.metadata1,
        this.attributeClient.getById(mockContext.getTenantId().get(), "first-id").get());
    verify(this.mockAttributeService, times(1)).getAttributes(any(), any());
    verifyNoMoreInteractions(this.mockAttributeService);
    assertEquals(
        this.metadata2,
        this.attributeClient.getById(mockContext.getTenantId().get(), "second-id").get());
  }

  @Test
  void getAllInScope() {
    assertEquals(
        this.responseMetadata,
        this.attributeClient.getAllInScope(mockContext.getTenantId().get(), "EVENT").get());

    assertEquals(
        emptyList(),
        this.attributeClient.getAllInScope(mockContext.getTenantId().get(), "DOESNT_EXIST").get());
  }
}
