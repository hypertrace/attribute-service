package org.hypertrace.core.attribute.service.projection;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import org.hypertrace.core.attribute.service.v1.LiteralValue;

abstract class AbstractAttributeProjection<R> implements AttributeProjection {

  private final AttributeKindWithNullability resultKind;
  private final List<AttributeKindWithNullability> argumentKinds;

  protected AbstractAttributeProjection(
      AttributeKindWithNullability resultKind, List<AttributeKindWithNullability> argumentKinds) {
    this.resultKind = resultKind;
    this.argumentKinds = argumentKinds;
  }

  @Override
  public LiteralValue project(List<LiteralValue> arguments) {
    Preconditions.checkArgument(arguments.size() == argumentKinds.size());
    List<Object> unwrappedArguments = new ArrayList<>(argumentKinds.size());
    for (int index = 0; index < arguments.size(); index++) {
      int argumentIndex = index;
      LiteralValue argumentLiteral = arguments.get(argumentIndex);
      AttributeKindWithNullability maybeNullableKind = this.argumentKinds.get(argumentIndex);
      Object unwrappedArgument =
          ValueCoercer.fromLiteral(argumentLiteral, maybeNullableKind.getKind())
              .orElseGet(
                  () -> {
                    if (maybeNullableKind.isNullable()) {
                      return null;
                    }
                    throw new IllegalArgumentException(
                        String.format(
                            "Projection argument %s at index %d could not be converted to expected type %s",
                            argumentLiteral, argumentIndex, maybeNullableKind));
                  });

      unwrappedArguments.add(argumentIndex, unwrappedArgument);
    }
    Object unwrappedResult = this.doUnwrappedProjection(unwrappedArguments);
    return ValueCoercer.toLiteral(
            unwrappedResult, this.resultKind.getKind(), this.resultKind.isNullable())
        .orElseThrow(
            () ->
                new UnsupportedOperationException(
                    String.format(
                        "Projection result %s could not be converted to expected type %s",
                        unwrappedResult, this.resultKind)));
  }

  private <T> T nullOrThrow(
      AttributeKindWithNullability maybeNullableKind, RuntimeException exception) {
    if (maybeNullableKind.isNullable()) {
      return null;
    }
    throw exception;
  }

  protected abstract R doUnwrappedProjection(List<Object> arguments);
}
