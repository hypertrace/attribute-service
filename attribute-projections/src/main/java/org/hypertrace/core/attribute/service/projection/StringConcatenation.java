package org.hypertrace.core.attribute.service.projection;

import java.util.List;
import org.hypertrace.core.attribute.service.v1.AttributeKind;

public class StringConcatenation implements AttributeProjection {
  public static StringConcatenation STRING_CONCATENATION = new StringConcatenation();

  public static String concatenate(List<String> strings) {
    if (strings.size() == 1) {
      return strings.get(0);
    }
    if (strings.size() == 2) {
      return strings.get(0) + strings.get(1);
    }
    StringBuilder builder = new StringBuilder();
    for (String string : strings) {
      builder.append(string);
    }
    return builder.toString();
  }

  private StringConcatenation() {}

  @Override
  public AttributeKind getArgumentKindAtIndex(int index) {
    return AttributeKind.TYPE_STRING;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object project(List<Object> arguments) {
    return concatenate((List<String>) (Object) arguments);
  }
}
