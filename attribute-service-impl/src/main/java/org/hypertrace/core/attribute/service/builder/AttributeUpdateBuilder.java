package org.hypertrace.core.attribute.service.builder;

import java.util.Optional;
import org.hypertrace.core.attribute.service.v1.Update;
import org.hypertrace.core.documentstore.model.subdoc.SubDocumentUpdate;

public interface AttributeUpdateBuilder {
  Optional<SubDocumentUpdate> buildUpdate(final Update update);
}
