package org.hypertrace.core.attribute.service.builder;

import static java.util.Map.entry;
import static org.hypertrace.core.attribute.service.v1.Update.TypeCase.DISPLAY_NAME;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.hypertrace.core.attribute.service.v1.Update;
import org.hypertrace.core.attribute.service.v1.Update.TypeCase;
import org.hypertrace.core.documentstore.model.subdoc.SubDocumentUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttributeUpdateBuilderImpl implements AttributeUpdateBuilder {
  private static final Logger LOGGER = LoggerFactory.getLogger(AttributeUpdateBuilderImpl.class);

  private static final String DISPLAY_NAME_SUB_DOC_PATH = "display_name";

  private static final Map<TypeCase, Function<Update, SubDocumentUpdate>> UPDATE_PROVIDER_MAP =
      Map.ofEntries(entry(DISPLAY_NAME, AttributeUpdateBuilderImpl::getDisplayNameUpdate));

  private static SubDocumentUpdate getDisplayNameUpdate(final Update update) {
    return SubDocumentUpdate.of(DISPLAY_NAME_SUB_DOC_PATH, update.getDisplayName());
  }

  @Override
  public Optional<SubDocumentUpdate> buildUpdate(final Update update) {
    final TypeCase typeCase = update.getTypeCase();
    final Optional<SubDocumentUpdate> subDocumentUpdate =
        Optional.ofNullable(UPDATE_PROVIDER_MAP.get(typeCase))
            .map(provider -> provider.apply(update));

    if (subDocumentUpdate.isEmpty()) {
      LOGGER.warn(String.format("Updating %s is not supported yet", typeCase));
    }

    return subDocumentUpdate;
  }
}
