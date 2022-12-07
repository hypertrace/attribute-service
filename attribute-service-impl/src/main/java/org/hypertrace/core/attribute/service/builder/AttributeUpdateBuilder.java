package org.hypertrace.core.attribute.service.builder;

import org.hypertrace.core.attribute.service.v1.Update;
import org.hypertrace.core.documentstore.model.subdoc.SubDocumentUpdate;

public interface AttributeUpdateBuilder {
  SubDocumentUpdate buildUpdate(final Update update);
}
