package org.hypertrace.core.attribute.service.delegate;

import static org.hypertrace.core.attribute.service.v1.AggregateFunction.DISTINCT_COUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.util.Optional;
import org.hypertrace.core.attribute.service.v1.AttributeDefinition;
import org.hypertrace.core.attribute.service.v1.AttributeKind;
import org.hypertrace.core.attribute.service.v1.AttributeMetadata;
import org.hypertrace.core.attribute.service.v1.AttributeScope;
import org.hypertrace.core.attribute.service.v1.AttributeType;
import org.hypertrace.core.attribute.service.v1.Update;
import org.hypertrace.core.attribute.service.v1.UpdateMetadataRequest;
import org.hypertrace.core.attribute.service.v1.UpdateMetadataResponse;
import org.hypertrace.core.documentstore.Collection;
import org.hypertrace.core.documentstore.JSONDocument;
import org.hypertrace.core.documentstore.model.options.UpdateOptions;
import org.hypertrace.core.documentstore.query.Query;
import org.hypertrace.core.grpcutils.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttributeUpdaterImplTest {
  private static final String TEST_TENANT_ID = "test-tenant-id";

  private Collection mockCollection;
  private RequestContext mockContext;

  private AttributeUpdater attributeUpdaterImpl;

  @BeforeEach
  void setUp() {
    mockCollection = mock(Collection.class);
    mockContext = RequestContext.forTenantId(TEST_TENANT_ID);
    attributeUpdaterImpl = new AttributeUpdaterImpl(mockCollection);
  }

  @Test
  void testUpdate() throws Exception {
    final UpdateMetadataRequest request =
        UpdateMetadataRequest.newBuilder()
            .setAttributeId("attributeId")
            .addUpdates(Update.newBuilder().setDisplayName("display_name_updated").build())
            .build();
    final AttributeMetadata attribute =
        AttributeMetadata.newBuilder()
            .addAllLabels(Lists.newArrayList("item1"))
            .setFqn("fqn")
            .setId(AttributeScope.EVENT + ".key")
            .setKey("key")
            .setDisplayName("display_name_updated")
            .setMaterialized(true)
            .setScope(AttributeScope.EVENT)
            .setScopeString(AttributeScope.EVENT.name())
            .setType(AttributeType.ATTRIBUTE)
            .setUnit("ms")
            .setValueKind(AttributeKind.TYPE_STRING)
            .addSupportedAggregations(DISTINCT_COUNT)
            .setDefinition(AttributeDefinition.getDefaultInstance())
            .setGroupable(true)
            .setInternal(true)
            .setCustom(true)
            .build();
    final UpdateMetadataResponse expectedResult =
        UpdateMetadataResponse.newBuilder().setAttribute(attribute).build();
    final String jsonAttribute =
        "{"
            + "\"fqn\":\"fqn\","
            + "\"key\":\"key\","
            + "\"materialized\":true,"
            + "\"unit\":\"ms\","
            + "\"type\":\"ATTRIBUTE\","
            + "\"labels\":[\"item1\"],"
            + "\"groupable\":true,"
            + "\"supportedAggregations\":[],"
            + "\"onlyAggregationsAllowed\":false,"
            + "\"sources\":[],"
            + "\"internal\":true,"
            + "\"id\":\"EVENT.key\","
            + "\"value_kind\":\"TYPE_STRING\","
            + "\"display_name\":\"display_name_updated\","
            + "\"scope_string\":\"EVENT\","
            + "\"tenant_id\":\"tenantId\""
            + "}";
    when(mockCollection.update(any(Query.class), anyList(), any(UpdateOptions.class)))
        .thenReturn(Optional.of(new JSONDocument(jsonAttribute)));

    final UpdateMetadataResponse result = attributeUpdaterImpl.update(request, mockContext);
    assertEquals(expectedResult, result);
  }

  @Test
  void testUpdate_AttributeIdAbsent() {
    final UpdateMetadataRequest request =
        UpdateMetadataRequest.newBuilder()
            .addUpdates(Update.newBuilder().setDisplayName("another_name").build())
            .build();
    assertThrows(
        StatusRuntimeException.class, () -> attributeUpdaterImpl.update(request, mockContext));
  }

  @Test
  void testUpdate_UpdatesAreEmpty() {
    final UpdateMetadataRequest request =
        UpdateMetadataRequest.newBuilder().setAttributeId("attributeId").build();
    assertThrows(
        StatusRuntimeException.class, () -> attributeUpdaterImpl.update(request, mockContext));
  }

  @Test
  void testUpdate_UpdateFieldIsAbsent() {
    final UpdateMetadataRequest request =
        UpdateMetadataRequest.newBuilder()
            .setAttributeId("attributeId")
            .addUpdates(Update.newBuilder().build())
            .build();
    assertThrows(
        StatusRuntimeException.class, () -> attributeUpdaterImpl.update(request, mockContext));
  }

  @Test
  void testUpdate_CollectionReturnsAbsent() throws Exception {
    final UpdateMetadataRequest request =
        UpdateMetadataRequest.newBuilder()
            .setAttributeId("attributeId")
            .addUpdates(Update.newBuilder().setDisplayName("another_name").build())
            .build();
    when(mockCollection.update(any(Query.class), anyList(), any(UpdateOptions.class)))
        .thenReturn(Optional.empty());
    assertThrows(
        StatusRuntimeException.class, () -> attributeUpdaterImpl.update(request, mockContext));
  }

  @Test
  void testUpdate_CollectionThrowsIOException() throws Exception {
    final UpdateMetadataRequest request =
        UpdateMetadataRequest.newBuilder()
            .setAttributeId("attributeId")
            .addUpdates(Update.newBuilder().setDisplayName("some other display name").build())
            .build();
    when(mockCollection.update(any(Query.class), anyList(), any(UpdateOptions.class)))
        .thenThrow(IOException.class);
    assertThrows(IOException.class, () -> attributeUpdaterImpl.update(request, mockContext));
  }
}
