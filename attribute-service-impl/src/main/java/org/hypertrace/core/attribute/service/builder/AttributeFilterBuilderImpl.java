package org.hypertrace.core.attribute.service.builder;

import static org.hypertrace.core.attribute.service.constants.AttributeFieldPathConstants.ID_PATH;
import static org.hypertrace.core.attribute.service.constants.AttributeFieldPathConstants.TENANT_ID_PATH;
import static org.hypertrace.core.documentstore.expression.operators.RelationalOperator.EQ;

import org.hypertrace.core.documentstore.expression.impl.ConstantExpression;
import org.hypertrace.core.documentstore.expression.impl.IdentifierExpression;
import org.hypertrace.core.documentstore.expression.impl.RelationalExpression;
import org.hypertrace.core.documentstore.expression.type.FilterTypeExpression;

public class AttributeFilterBuilderImpl implements AttributeFilterBuilder {
  @Override
  public FilterTypeExpression buildIdFilter(final String attributeId) {
    return RelationalExpression.of(
        IdentifierExpression.of(ID_PATH), EQ, ConstantExpression.of(attributeId));
  }

  @Override
  public FilterTypeExpression buildTenantIdFilter(final String tenantId) {
    return RelationalExpression.of(
        IdentifierExpression.of(TENANT_ID_PATH), EQ, ConstantExpression.of(tenantId));
  }
}
