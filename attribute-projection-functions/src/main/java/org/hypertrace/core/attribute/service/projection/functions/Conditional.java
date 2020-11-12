package org.hypertrace.core.attribute.service.projection.functions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Conditional {
  @Nullable
  public static String getValue(@Nonnull boolean condition,
                                @Nullable String first,
                                @Nullable String second) {
    return condition ? first : second;
  }
}
