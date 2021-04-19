package org.hypertrace.core.attribute.service.projection.functions;

import static java.util.Objects.isNull;

import javax.annotation.Nullable;

public class ConcatenateOrNull {

  @Nullable
  public static String concatenate(@Nullable String first, @Nullable String second) {
    if (isNull(first) || isNull(second)) {
      return null;
    }
    return first + second;
  }
}
