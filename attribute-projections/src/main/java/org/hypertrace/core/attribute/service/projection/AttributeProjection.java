package org.hypertrace.core.attribute.service.projection;

import java.util.List;
import org.hypertrace.core.attribute.service.v1.AttributeKind;
import org.hypertrace.core.attribute.service.v1.LiteralValue;

public interface AttributeProjection {

  AttributeKind getArgumentKindAtIndex(int index);

  Object project(List<Object> arguments);
}
