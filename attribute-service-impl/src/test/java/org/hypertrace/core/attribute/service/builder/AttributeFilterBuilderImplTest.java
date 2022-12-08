package org.hypertrace.core.attribute.service.builder;

import static org.hypertrace.core.documentstore.expression.operators.RelationalOperator.EQ;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hypertrace.core.documentstore.expression.impl.ConstantExpression;
import org.hypertrace.core.documentstore.expression.impl.IdentifierExpression;
import org.hypertrace.core.documentstore.expression.impl.RelationalExpression;
import org.hypertrace.core.documentstore.expression.type.FilterTypeExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttributeFilterBuilderImplTest {

  private AttributeFilterBuilder attributeFilterBuilderImpl;

  @BeforeEach
  void setUp() {
    attributeFilterBuilderImpl = new AttributeFilterBuilderImpl();
  }

  @Test
  void testBuildIdFilter() {
    final FilterTypeExpression expectedResult =
        RelationalExpression.of(
            IdentifierExpression.of("id"), EQ, ConstantExpression.of("attributeId"));
    final FilterTypeExpression result = attributeFilterBuilderImpl.buildIdFilter("attributeId");
    assertEquals(expectedResult, result);
  }

  @Test
  void testBuildTenantIdFilter() {
    final FilterTypeExpression expectedResult =
        RelationalExpression.of(
            IdentifierExpression.of("tenant_id"), EQ, ConstantExpression.of("tenantId"));
    final FilterTypeExpression result = attributeFilterBuilderImpl.buildTenantIdFilter("tenantId");
    assertEquals(expectedResult, result);
  }
}
