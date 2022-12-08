package org.hypertrace.core.attribute.service.validator;

import com.google.protobuf.Message;

public interface StringLengthValidator {
  void validate(final Message message);
}
