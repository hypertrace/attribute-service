package org.hypertrace.core.attribute.service.projection;

import static org.hypertrace.core.attribute.service.projection.DefaultValue.DEFAULT_VALUE;
import static org.hypertrace.core.attribute.service.projection.StringConcatenation.STRING_CONCATENATION;
import static org.hypertrace.core.attribute.service.projection.StringHash.STRING_HASH;
import static org.hypertrace.core.attribute.service.v1.ProjectionOperator.PROJECTION_OPERATOR_CONCAT;
import static org.hypertrace.core.attribute.service.v1.ProjectionOperator.PROJECTION_OPERATOR_DEFAULT;
import static org.hypertrace.core.attribute.service.v1.ProjectionOperator.PROJECTION_OPERATOR_HASH;

import java.util.Map;
import java.util.Optional;
import org.hypertrace.core.attribute.service.v1.ProjectionOperator;

public class AttributeProjectionRegistry {

  private static final Map<ProjectionOperator, AttributeProjection> PROJECTION_MAP =
      Map.of(
          PROJECTION_OPERATOR_CONCAT, STRING_CONCATENATION,
          PROJECTION_OPERATOR_HASH, STRING_HASH,
          PROJECTION_OPERATOR_DEFAULT, DEFAULT_VALUE);

  public Optional<AttributeProjection> getProjection(ProjectionOperator projectionOperator) {
    return Optional.ofNullable(PROJECTION_MAP.get(projectionOperator));
  }


}
