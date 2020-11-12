package org.hypertrace.core.attribute.service.projection.functions;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNullElse;

import javax.annotation.Nullable;

public class Equals {
  private static final String DEFAULT_STRING = "";

  public static boolean stringEquals(@Nullable String first, @Nullable String second) {
    if (isNull(first) && isNull(second)) {
      return true;
    }
    return requireNonNullElse(first, DEFAULT_STRING).equals(requireNonNullElse(second, DEFAULT_STRING));
  }
}
