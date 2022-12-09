package org.hypertrace.core.attribute.service.validator;

import static org.hypertrace.core.attribute.service.util.AttributeScopeUtil.resolveScopeString;
import static org.hypertrace.core.attribute.service.utils.tenant.TenantUtils.ROOT_TENANT_ID;

import com.google.common.base.Strings;
import com.typesafe.config.Config;
import io.grpc.Status;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import org.hypertrace.core.attribute.service.v1.AttributeCreateRequest;
import org.hypertrace.core.attribute.service.v1.AttributeKind;
import org.hypertrace.core.attribute.service.v1.AttributeMetadata;
import org.hypertrace.core.attribute.service.v1.AttributeMetadataFilter;
import org.hypertrace.core.attribute.service.v1.AttributeScope;
import org.hypertrace.core.attribute.service.v1.AttributeType;
import org.hypertrace.core.grpcutils.context.RequestContext;

/** Validates {@link AttributeCreateRequest} */
public class AttributeMetadataValidator {
  private static final String MAX_CUSTOM_ATTRIBUTES_PER_TENANT = "max.custom.attributes.per.tenant";
  private static final int MAX_STRING_LENGTH = 1000;
  private static final StringLengthValidator stringLengthValidator =
      new StringLengthValidatorImpl();
  ;

  private final long maxCustomAttributesPerTenant;

  public AttributeMetadataValidator() {
    this.maxCustomAttributesPerTenant = 5;
  }

  public AttributeMetadataValidator(final Config config) {
    this.maxCustomAttributesPerTenant = config.getInt(MAX_CUSTOM_ATTRIBUTES_PER_TENANT);
  }

  public void validate(
      AttributeCreateRequest attributeCreateRequest,
      final String tenantId,
      final LongSupplier customAttributeCountSupplier) {
    attributeCreateRequest.getAttributesList().forEach(AttributeMetadataValidator::validate);

    // Ensure Scope + Key is unique
    List<Map.Entry<String, String>> duplicateScopeKeys =
        attributeCreateRequest.getAttributesList().stream()
            .collect(
                Collectors.groupingBy(
                    attributeMetadata ->
                        new AbstractMap.SimpleEntry<>(
                            resolveScopeString(attributeMetadata), attributeMetadata.getKey())))
            .entrySet()
            .stream()
            .filter(attributeMetadataList -> attributeMetadataList.getValue().size() > 1)
            .map(Entry::getKey)
            .collect(Collectors.toList());
    if (!duplicateScopeKeys.isEmpty()) {
      throw new IllegalArgumentException(
          String.format("Duplicate scope + key found for:%s", duplicateScopeKeys));
    }

    // Ensure Scope + FQN is unique
    List<Map.Entry<String, String>> duplicateScopeFQNs =
        attributeCreateRequest.getAttributesList().stream()
            .collect(
                Collectors.groupingBy(
                    attributeMetadata ->
                        new AbstractMap.SimpleEntry<>(
                            resolveScopeString(attributeMetadata), attributeMetadata.getFqn())))
            .entrySet()
            .stream()
            .filter(attributeMetadataList -> attributeMetadataList.getValue().size() > 1)
            .map(Entry::getKey)
            .collect(Collectors.toList());
    if (!duplicateScopeFQNs.isEmpty()) {
      throw new IllegalArgumentException(
          String.format("Duplicate scope + FQN found for:%s", duplicateScopeFQNs));
    }

    verifyCustomAttributeLimitNotReached(
        tenantId, customAttributeCountSupplier, attributeCreateRequest.getAttributesCount());
  }

  public static AttributeMetadataFilter validateAndUpdateDeletionFilter(
      final AttributeMetadataFilter attributeMetadataFilter) {
    if (attributeMetadataFilter.hasCustom()) {
      if (!attributeMetadataFilter.getCustom()) {
        throw Status.INVALID_ARGUMENT
            .withDescription("Can only delete custom attributes")
            .asRuntimeException();
      }

      return attributeMetadataFilter;
    } else {
      return attributeMetadataFilter.toBuilder().setCustom(true).build();
    }
  }

  public static String validateContextAndGetTenantId(final RequestContext context) {
    return context
        .getTenantId()
        .orElseThrow(
            () ->
                Status.UNAUTHENTICATED
                    .withDescription("Tenant id is missing in the request.")
                    .asRuntimeException());
  }

  private static void validate(AttributeMetadata attributeMetadata) {
    if (resolveScopeString(attributeMetadata).equals(AttributeScope.SCOPE_UNDEFINED.name())
        || Strings.isNullOrEmpty(attributeMetadata.getKey())
        || Strings.isNullOrEmpty(attributeMetadata.getFqn())
        || attributeMetadata.getValueKind().equals(AttributeKind.KIND_UNDEFINED)
        || attributeMetadata.getValueKind().equals(AttributeKind.UNRECOGNIZED)
        || attributeMetadata.getType().equals(AttributeType.UNRECOGNIZED)
        || attributeMetadata.getType().equals(AttributeType.TYPE_UNDEFINED)) {
      throw new IllegalArgumentException(String.format("Invalid attribute:%s", attributeMetadata));
    }

    stringLengthValidator.validate(attributeMetadata);
  }

  private void verifyCustomAttributeLimitNotReached(
      final String tenantId,
      final LongSupplier customAttributeCountSupplier,
      final int newAttributeCount) {
    if (ROOT_TENANT_ID.equals(tenantId)) {
      // No limit validation for system attributes
      return;
    }

    final long numCustomAttributes = customAttributeCountSupplier.getAsLong();
    if (numCustomAttributes + newAttributeCount > maxCustomAttributesPerTenant) {
      throw Status.RESOURCE_EXHAUSTED
          .withDescription(
              String.format(
                  "%d custom attributes are present and %d custom attributes could not be registered because it exceeds the limit (%d)",
                  numCustomAttributes, newAttributeCount, maxCustomAttributesPerTenant))
          .asRuntimeException();
    }
  }
}
