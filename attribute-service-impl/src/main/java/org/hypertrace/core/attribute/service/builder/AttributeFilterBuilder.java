package org.hypertrace.core.attribute.service.builder;

import org.hypertrace.core.documentstore.expression.type.FilterTypeExpression;

public interface AttributeFilterBuilder {
  FilterTypeExpression buildIdFilter(final String attributeId);

  FilterTypeExpression buildTenantIdFilter(final String tenantId);
}
