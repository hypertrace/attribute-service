package org.hypertrace.core.attribute.service.validator;

import static org.hypertrace.core.attribute.service.utils.tenant.TenantUtils.ROOT_TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.typesafe.config.ConfigFactory;
import java.util.Map;
import org.hypertrace.core.attribute.service.v1.AttributeCreateRequest;
import org.hypertrace.core.attribute.service.v1.AttributeKind;
import org.hypertrace.core.attribute.service.v1.AttributeMetadata;
import org.hypertrace.core.attribute.service.v1.AttributeScope;
import org.hypertrace.core.attribute.service.v1.AttributeType;
import org.junit.jupiter.api.Test;

public class AttributeMetadataValidatorTest {

  @Test
  public void testAttributeCreateRequestValid() {
    AttributeCreateRequest attributeCreateRequest =
        AttributeCreateRequest.newBuilder()
            .addAttributes(
                AttributeMetadata.newBuilder()
                    .setScope(AttributeScope.EVENT)
                    .setKey("name")
                    .setFqn("EVENT.name")
                    .setValueKind(AttributeKind.TYPE_STRING)
                    .setType(AttributeType.ATTRIBUTE)
                    .build())
            .build();
    new AttributeMetadataValidator().validate(attributeCreateRequest, "someTenantId", () -> 4);
  }

  @Test
  public void testAttributeMetadataValidatorInvalidAttributeMetadata() {
    // Don't set Scope and verify there is a validation error
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          AttributeCreateRequest attributeCreateRequest =
              AttributeCreateRequest.newBuilder()
                  .addAttributes(
                      AttributeMetadata.newBuilder()
                          .setKey("name")
                          .setFqn("EVENT.name")
                          .setValueKind(AttributeKind.TYPE_STRING)
                          .setType(AttributeType.ATTRIBUTE)
                          .build())
                  .build();
          new AttributeMetadataValidator()
              .validate(attributeCreateRequest, "someTenantId", () -> 4);
        });
  }

  @Test
  public void testAttributeMetadataValidatorCustomAttributesWithinLimit() {
    assertDoesNotThrow(
        () -> {
          AttributeCreateRequest attributeCreateRequest =
              AttributeCreateRequest.newBuilder()
                  .addAttributes(
                      AttributeMetadata.newBuilder()
                          .setScope(AttributeScope.EVENT)
                          .setKey("name")
                          .setFqn("EVENT.name")
                          .setValueKind(AttributeKind.TYPE_STRING)
                          .setType(AttributeType.ATTRIBUTE)
                          .build())
                  .build();
          new AttributeMetadataValidator(
                  ConfigFactory.parseMap(Map.of("max.custom.attributes.per.tenant", "7")))
              .validate(attributeCreateRequest, "someTenantId", () -> 6);
        });
  }

  @Test
  public void testAttributeMetadataValidatorCustomAttributesExceedingLimit() {
    assertThrows(
        RuntimeException.class,
        () -> {
          AttributeCreateRequest attributeCreateRequest =
              AttributeCreateRequest.newBuilder()
                  .addAttributes(
                      AttributeMetadata.newBuilder()
                          .setScope(AttributeScope.EVENT)
                          .setKey("name")
                          .setFqn("EVENT.name")
                          .setValueKind(AttributeKind.TYPE_STRING)
                          .setType(AttributeType.ATTRIBUTE)
                          .build())
                  .build();
          new AttributeMetadataValidator(
                  ConfigFactory.parseMap(Map.of("max.custom.attributes.per.tenant", "7")))
              .validate(attributeCreateRequest, "someTenantId", () -> 7);
        });
  }

  @Test
  public void testAttributeMetadataValidatorCustomAttributesExceedingLimitForSystemTenant() {
    assertDoesNotThrow(
        () -> {
          AttributeCreateRequest attributeCreateRequest =
              AttributeCreateRequest.newBuilder()
                  .addAttributes(
                      AttributeMetadata.newBuilder()
                          .setScope(AttributeScope.EVENT)
                          .setKey("name")
                          .setFqn("EVENT.name")
                          .setValueKind(AttributeKind.TYPE_STRING)
                          .setType(AttributeType.ATTRIBUTE)
                          .build())
                  .build();
          new AttributeMetadataValidator(
                  ConfigFactory.parseMap(Map.of("max.custom.attributes.per.tenant", "7")))
              .validate(attributeCreateRequest, ROOT_TENANT_ID, () -> 7);
        });
  }
}
