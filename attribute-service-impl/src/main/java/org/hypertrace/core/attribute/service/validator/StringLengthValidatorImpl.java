package org.hypertrace.core.attribute.service.validator;

import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.MESSAGE;
import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.STRING;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import io.grpc.Status;
import java.util.List;

public class StringLengthValidatorImpl implements StringLengthValidator {
  private static final int MAX_STRING_LENGTH = 1000;

  @Override
  public void validate(final Message message) {
    final Descriptor descriptor = message.getDescriptorForType();
    validateProto(message, descriptor);
  }

  private void validateProto(final Message message, final Descriptor descriptor) {
    final List<FieldDescriptor> fieldDescriptors = descriptor.getFields();

    for (final FieldDescriptor fieldDescriptor : fieldDescriptors) {
      if (fieldDescriptor.getJavaType() == MESSAGE) {
        validateProto(message, fieldDescriptor);
      } else if (fieldDescriptor.getJavaType() == STRING) {
        validateStringLength(message.getField(fieldDescriptor).toString());
      }
    }
  }

  private void validateProto(final Message message, final FieldDescriptor fieldDescriptor) {
    if (fieldDescriptor.isRepeated()) {
      validateRepeatedProto(message, fieldDescriptor);
    } else {
      validateSingleProto(message, fieldDescriptor);
    }
  }

  private void validateRepeatedProto(final Message message, final FieldDescriptor fieldDescriptor) {
    final Object object = message.getField(fieldDescriptor);

    if (object instanceof List) {
      final List<?> values = (List<?>) object;
      values.stream()
          .filter(value -> value instanceof Message)
          .map(value -> (Message) value)
          .forEach(value -> validateProto(value, value.getDescriptorForType()));
    }
  }

  private void validateSingleProto(final Message message, final FieldDescriptor fieldDescriptor) {
    final Object object = message.getField(fieldDescriptor);

    if (object instanceof Message) {
      final Message value = (Message) object;
      validateProto(value, fieldDescriptor.getMessageType());
    }
  }

  private void validateStringLength(final String string) {
    if (string.length() > MAX_STRING_LENGTH) {
      throw Status.INVALID_ARGUMENT
          .withDescription(
              String.format(
                  "String value greater than %d characters is not allowed", MAX_STRING_LENGTH))
          .asRuntimeException();
    }
  }
}
