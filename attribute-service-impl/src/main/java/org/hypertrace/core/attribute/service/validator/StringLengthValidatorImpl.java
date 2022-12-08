package org.hypertrace.core.attribute.service.validator;

import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.STRING;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;
import io.grpc.Status;
import java.util.List;
import java.util.function.Supplier;

public class StringLengthValidatorImpl implements StringLengthValidator {
  private static final int MAX_STRING_LENGTH = 1000;
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Printer PRINTER = JsonFormat.printer();

  @Override
  public void validate(final Message message) {
    final Descriptor descriptor = message.getDescriptorForType();
    try {
      final JsonNode node = MAPPER.readTree(PRINTER.print(message));
      validateJsonNode(node, () -> descriptor);
    } catch (final JsonProcessingException | InvalidProtocolBufferException e) {
      throw Status.INTERNAL.withDescription("Unable to validate request").asRuntimeException();
    }
  }

  private void validateAllStringsWithinBounds(
      final JsonNode node, final FieldDescriptor descriptor) {
    if (node.isObject()) {
      validateJsonNode(node, descriptor::getMessageType);
      return;
    }

    if (descriptor.getJavaType() == STRING) {
      if (node.asText().length() > MAX_STRING_LENGTH) {
        throw Status.INVALID_ARGUMENT
            .withDescription(
                String.format(
                    "String value greater than %d characters is not allowed", MAX_STRING_LENGTH))
            .asRuntimeException();
      }
    }
  }

  private void validateJsonNode(
      final JsonNode node, final Supplier<Descriptor> descriptorSupplier) {
    final List<FieldDescriptor> fieldDescriptors = descriptorSupplier.get().getFields();
    for (final FieldDescriptor descriptor : fieldDescriptors) {
      final String key = descriptor.getJsonName();
      if (node.has(key)) {
        final JsonNode value = node.get(key);
        validateAllStringsWithinBounds(value, descriptor);
      }
    }
  }
}
