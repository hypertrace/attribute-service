package org.hypertrace.core.attribute.service.converter;

import java.util.Optional;
import org.hypertrace.core.attribute.service.v1.AttributeMetadata;
import org.hypertrace.core.documentstore.Document;

public interface AttributeMetadataConverter {
  Optional<AttributeMetadata> convert(final Document document);
}
