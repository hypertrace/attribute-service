package org.hypertrace.core.attribute.service.model;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.hypertrace.core.attribute.service.v1.AttributeDefinition;
import org.hypertrace.core.attribute.service.v1.AttributeKind;
import org.hypertrace.core.attribute.service.v1.AttributeMetadata;
import org.hypertrace.core.attribute.service.v1.AttributeScope;
import org.hypertrace.core.attribute.service.v1.AttributeSource;
import org.hypertrace.core.attribute.service.v1.AttributeSourceMetadata;
import org.hypertrace.core.attribute.service.v1.AttributeType;
import org.hypertrace.core.attribute.service.v1.Projection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Test cases for {@link AttributeMetadataModel} */
public class AttributeMetadataModelTest {

  @Test
  public void testAttributeMetadataModelJsonSerDes() throws IOException {
    AttributeMetadataModel attributeMetadataModel = new AttributeMetadataModel();
    attributeMetadataModel.setLabels(Lists.newArrayList("item1"));
    attributeMetadataModel.setFqn("fqn");
    attributeMetadataModel.setKey("key");
    attributeMetadataModel.setDisplayName("Some Name");
    attributeMetadataModel.setMaterialized(true);
    attributeMetadataModel.setGroupable(true);
    attributeMetadataModel.setScope(AttributeScope.EVENT);
    attributeMetadataModel.setType(AttributeType.ATTRIBUTE);
    attributeMetadataModel.setUnit("ms");
    attributeMetadataModel.setValueKind(AttributeKind.TYPE_STRING);
    attributeMetadataModel.setTenantId("tenantId");
    attributeMetadataModel.setDefinition(
        AttributeDefinition.newBuilder()
            .setProjection(Projection.newBuilder().setAttributeId("test"))
            .build());

    String json = attributeMetadataModel.toJson();
    String expectedJson =
        "{"
            + "\"fqn\":\"fqn\","
            + "\"key\":\"key\","
            + "\"scope\":\"EVENT\","
            + "\"materialized\":true,"
            + "\"unit\":\"ms\","
            + "\"type\":\"ATTRIBUTE\","
            + "\"labels\":[\"item1\"],"
            + "\"groupable\":true,"
            + "\"supportedAggregations\":[],"
            + "\"onlyAggregationsAllowed\":false,"
            + "\"sources\":[],"
            + "\"definition\":{\"projection\":{\"attributeId\":\"test\"}},"
            + "\"id\":\"EVENT.key\","
            + "\"value_kind\":\"TYPE_STRING\","
            + "\"display_name\":\"Some Name\","
            + "\"tenant_id\":\"tenantId\""
            + "}";
    Assertions.assertEquals(expectedJson, json);
    AttributeMetadataModel deserializedModel = AttributeMetadataModel.fromJson(json);
    Assertions.assertEquals(attributeMetadataModel, deserializedModel);
  }

  @Test
  public void testAttributeMetaModelToFromDto() {
    AttributeMetadata attributeMetadata =
        AttributeMetadata.newBuilder()
            .addAllLabels(Lists.newArrayList("item1"))
            .setFqn("fqn")
            .setId(AttributeScope.EVENT + ".key")
            .setKey("key")
            .setDisplayName("Some Name")
            .setMaterialized(true)
            .setScope(AttributeScope.EVENT)
            .setType(AttributeType.ATTRIBUTE)
            .setUnit("ms")
            .setValueKind(AttributeKind.TYPE_STRING)
            .setDefinition(AttributeDefinition.getDefaultInstance())
            .putAllMetadata(
                Collections.singletonMap(
                    AttributeSource.EDS.name(),
                    AttributeSourceMetadata.newBuilder()
                        .putAllSourceMetadata(Map.of("fqn", "some_internal_mapping"))
                        .build()))
            .build();

    AttributeMetadata attributeMetadata1 =
        AttributeMetadataModel.fromDTO(attributeMetadata).toDTO();
    Assertions.assertEquals(attributeMetadata, attributeMetadata1);
  }

  @Test
  public void testAttributeMetaModelGroupableFromJson() throws IOException {
    String json =
        "{"
            + "\"fqn\":\"fqn\","
            + "\"key\":\"key\","
            + "\"scope\":\"EVENT\","
            + "\"materialized\":true,"
            + "\"unit\":\"ms\","
            + "\"type\":\"ATTRIBUTE\","
            + "\"labels\":[\"item1\"],"
            + "\"supportedAggregations\":[],"
            + "\"onlyAggregationsAllowed\":false,"
            + "\"sources\":[],"
            + "\"id\":\"EVENT.key\","
            + "\"value_kind\":\"TYPE_BOOL\","
            + "\"display_name\":\"Some Name\","
            + "\"tenant_id\":\"tenantId\""
            + "}";

    // backward compatibility test, no groupable field, BOOL type
    AttributeMetadataModel deserializedModel = AttributeMetadataModel.fromJson(json);
    Assertions.assertFalse(deserializedModel.isGroupable());
    AttributeMetadata metadata = deserializedModel.toDTO();
    Assertions.assertFalse(metadata.getGroupable());

    json =
        "{"
            + "\"fqn\":\"fqn\","
            + "\"key\":\"key\","
            + "\"scope\":\"EVENT\","
            + "\"materialized\":true,"
            + "\"unit\":\"ms\","
            + "\"type\":\"ATTRIBUTE\","
            + "\"labels\":[\"item1\"],"
            + "\"supportedAggregations\":[],"
            + "\"onlyAggregationsAllowed\":false,"
            + "\"sources\":[],"
            + "\"id\":\"EVENT.key\","
            + "\"value_kind\":\"TYPE_STRING\","
            + "\"display_name\":\"Some Name\","
            + "\"tenant_id\":\"tenantId\""
            + "}";

    // backward compatibility test, no groupable field, STRING type
    deserializedModel = AttributeMetadataModel.fromJson(json);
    Assertions.assertTrue(deserializedModel.isGroupable());
    metadata = deserializedModel.toDTO();
    Assertions.assertTrue(metadata.getGroupable());

    json =
        "{"
            + "\"fqn\":\"fqn\","
            + "\"key\":\"key\","
            + "\"scope\":\"EVENT\","
            + "\"materialized\":true,"
            + "\"unit\":\"ms\","
            + "\"type\":\"ATTRIBUTE\","
            + "\"labels\":[\"item1\"],"
            + "\"groupable\":true,"
            + "\"supportedAggregations\":[],"
            + "\"onlyAggregationsAllowed\":false,"
            + "\"sources\":[],"
            + "\"id\":\"EVENT.key\","
            + "\"value_kind\":\"TYPE_BOOL\","
            + "\"display_name\":\"Some Name\","
            + "\"tenant_id\":\"tenantId\""
            + "}";

    // override default, BOOL type
    deserializedModel = AttributeMetadataModel.fromJson(json);
    Assertions.assertTrue(deserializedModel.isGroupable());
    metadata = deserializedModel.toDTO();
    Assertions.assertTrue(metadata.getGroupable());

    json =
        "{"
            + "\"fqn\":\"fqn\","
            + "\"key\":\"key\","
            + "\"scope\":\"EVENT\","
            + "\"materialized\":true,"
            + "\"unit\":\"ms\","
            + "\"type\":\"ATTRIBUTE\","
            + "\"labels\":[\"item1\"],"
            + "\"groupable\":false,"
            + "\"supportedAggregations\":[],"
            + "\"onlyAggregationsAllowed\":false,"
            + "\"sources\":[],"
            + "\"id\":\"EVENT.key\","
            + "\"value_kind\":\"TYPE_STRING\","
            + "\"display_name\":\"Some Name\","
            + "\"tenant_id\":\"tenantId\""
            + "}";

    // override default, STRING type
    deserializedModel = AttributeMetadataModel.fromJson(json);
    Assertions.assertFalse(deserializedModel.isGroupable());
    metadata = deserializedModel.toDTO();
    Assertions.assertFalse(metadata.getGroupable());
  }

  @Test
  public void testAttributeDefinitionBackwardsCompatibility() throws IOException {
    String json =
        "{"
            + "\"fqn\":\"fqn\","
            + "\"key\":\"key\","
            + "\"scope\":\"EVENT\","
            + "\"materialized\":true,"
            + "\"unit\":\"ms\","
            + "\"type\":\"ATTRIBUTE\","
            + "\"labels\":[\"item1\"],"
            + "\"groupable\":true,"
            + "\"supportedAggregations\":[],"
            + "\"onlyAggregationsAllowed\":false,"
            + "\"sources\":[],"
            + "\"id\":\"EVENT.key\","
            + "\"value_kind\":\"TYPE_BOOL\","
            + "\"display_name\":\"Some Name\","
            + "\"tenant_id\":\"tenantId\""
            + "}";

    AttributeMetadataModel deserializedModel = AttributeMetadataModel.fromJson(json);
    Assertions.assertEquals(
        AttributeDefinition.getDefaultInstance(), deserializedModel.getDefinition());
    AttributeMetadata metadata = deserializedModel.toDTO();
    Assertions.assertEquals(AttributeDefinition.getDefaultInstance(), metadata.getDefinition());

    AttributeMetadataModel modelFromMetadataWithoutDefinition =
        AttributeMetadataModel.fromDTO(
            AttributeMetadata.newBuilder()
                .setFqn("fqn")
                .setId("id")
                .setKey("key")
                .setDisplayName("Display")
                .setMaterialized(true)
                .setScope(AttributeScope.EVENT)
                .setType(AttributeType.ATTRIBUTE)
                .setUnit("ms")
                .setValueKind(AttributeKind.TYPE_STRING)
                .build());

    String expectedJson =
        "{"
            + "\"fqn\":\"fqn\","
            + "\"key\":\"key\","
            + "\"scope\":\"EVENT\","
            + "\"materialized\":true,"
            + "\"unit\":\"ms\","
            + "\"type\":\"ATTRIBUTE\","
            + "\"labels\":[],"
            + "\"groupable\":false,"
            + "\"supportedAggregations\":[],"
            + "\"onlyAggregationsAllowed\":false,"
            + "\"sources\":[],"
            + "\"definition\":{},"
            + "\"id\":\"EVENT.key\","
            + "\"value_kind\":\"TYPE_STRING\","
            + "\"display_name\":\"Display\","
            + "\"tenant_id\":null}";
    Assertions.assertEquals(expectedJson, modelFromMetadataWithoutDefinition.toJson());
  }
}
