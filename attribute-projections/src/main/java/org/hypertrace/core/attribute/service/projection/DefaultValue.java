package org.hypertrace.core.attribute.service.projection;

import java.util.List;
import org.hypertrace.core.attribute.service.v1.AttributeKind;

public class DefaultValue implements AttributeProjection {

  public static DefaultValue DEFAULT_VALUE = new DefaultValue();

  public static Object defaultValue(List<Object> values) {
    for (Object value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private DefaultValue() {}

  @Override
  public AttributeKind getArgumentKindAtIndex(int index) {
    return AttributeKind.KIND_UNDEFINED;
  }

  @Override
  public Object project(List<Object> arguments) {
    return defaultValue(arguments);
  }
}
