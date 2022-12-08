package org.hypertrace.core.attribute.service.converter;

import java.io.IOException;
import java.util.Optional;
import org.hypertrace.core.attribute.service.decorator.SupportedAggregationsDecorator;
import org.hypertrace.core.attribute.service.model.AttributeMetadataModel;
import org.hypertrace.core.attribute.service.v1.AttributeMetadata;
import org.hypertrace.core.documentstore.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributeMetadataConverterImpl implements AttributeMetadataConverter {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(AttributeMetadataConverterImpl.class);

  @Override
  public Optional<AttributeMetadata> convert(Document document) {
    final String documentJson = document.toJson();
    try {
      return Optional.of(
          new SupportedAggregationsDecorator(
                  AttributeMetadataModel.fromJson(documentJson).toDTOBuilder())
              .decorate()
              .build());
    } catch (final IOException exception) {
      LOGGER.error(
          "Unable to convert this Json String to AttributeMetadata : {}", documentJson, exception);
      return Optional.empty();
    }
  }
}
