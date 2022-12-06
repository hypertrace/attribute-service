package org.hypertrace.core.attribute.service.delegate;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.hypertrace.core.documentstore.expression.impl.LogicalExpression.and;
import static org.hypertrace.core.documentstore.model.options.UpdateOptions.DEFAULT_UPDATE_OPTIONS;

import io.grpc.Status;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.hypertrace.core.attribute.service.AttributeMetadataValidator;
import org.hypertrace.core.attribute.service.builder.AttributeFilterBuilder;
import org.hypertrace.core.attribute.service.builder.AttributeFilterBuilderImpl;
import org.hypertrace.core.attribute.service.builder.AttributeUpdateBuilder;
import org.hypertrace.core.attribute.service.builder.AttributeUpdateBuilderImpl;
import org.hypertrace.core.attribute.service.converter.AttributeMetadataConverter;
import org.hypertrace.core.attribute.service.converter.AttributeMetadataConverterImpl;
import org.hypertrace.core.attribute.service.v1.UpdateMetadataRequest;
import org.hypertrace.core.attribute.service.v1.UpdateMetadataResponse;
import org.hypertrace.core.documentstore.Collection;
import org.hypertrace.core.documentstore.Document;
import org.hypertrace.core.documentstore.expression.impl.LogicalExpression;
import org.hypertrace.core.documentstore.expression.type.FilterTypeExpression;
import org.hypertrace.core.documentstore.model.subdoc.SubDocumentUpdate;
import org.hypertrace.core.documentstore.query.Query;
import org.hypertrace.core.grpcutils.context.RequestContext;

public class AttributeUpdaterImpl implements AttributeUpdater {
  private final Collection collection;
  private final AttributeMetadataConverter converter;
  private final AttributeFilterBuilder filterBuilder;
  private final AttributeUpdateBuilder updateBuilder;

  public AttributeUpdaterImpl(final Collection collection) {
    this.collection = collection;
    this.converter = new AttributeMetadataConverterImpl();
    this.filterBuilder = new AttributeFilterBuilderImpl();
    this.updateBuilder = new AttributeUpdateBuilderImpl();
  }

  @Override
  public UpdateMetadataResponse update(
      final UpdateMetadataRequest request, final RequestContext context) throws IOException {
    final String tenantId = AttributeMetadataValidator.validateContextAndGetTenantId(context);
    validate(request);

    final FilterTypeExpression filter = buildFilter(request, tenantId);
    final Query query = Query.builder().setFilter(filter).build();
    final List<SubDocumentUpdate> updates = buildUpdates(request);
    final Optional<Document> docOptional =
        collection.update(query, updates, DEFAULT_UPDATE_OPTIONS);

    if (docOptional.isEmpty()) {
      throw Status.NOT_FOUND
          .withDescription(
              String.format("No attribute found with id: %s", request.getAttributeId()))
          .asRuntimeException();
    }

    return docOptional
        .map(converter::convert)
        .map(Optional::orElseThrow)
        .map(metadata -> UpdateMetadataResponse.newBuilder().setAttribute(metadata).build())
        .orElseThrow();
  }

  private void validate(final UpdateMetadataRequest request) {
    if (request.getAttributeId().isBlank()) {
      throw Status.INVALID_ARGUMENT
          .withDescription("Attribute id is must for updating")
          .asRuntimeException();
    }

    if (request.getUpdatesCount() == 0) {
      throw Status.INVALID_ARGUMENT
          .withDescription("At least one update is required")
          .asRuntimeException();
    }
  }

  private LogicalExpression buildFilter(UpdateMetadataRequest request, String tenantId) {
    final FilterTypeExpression idFilter = filterBuilder.buildIdFilter(request.getAttributeId());
    final FilterTypeExpression tenantIdFilter = filterBuilder.buildTenantIdFilter(tenantId);
    return and(idFilter, tenantIdFilter);
  }

  private List<SubDocumentUpdate> buildUpdates(UpdateMetadataRequest request) {
    return request.getUpdatesList().stream()
        .map(updateBuilder::buildUpdate)
        .flatMap(Optional::stream)
        .collect(toUnmodifiableList());
  }
}
