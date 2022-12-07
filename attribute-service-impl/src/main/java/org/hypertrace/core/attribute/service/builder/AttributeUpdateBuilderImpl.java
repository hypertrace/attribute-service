package org.hypertrace.core.attribute.service.builder;

import static java.util.Map.entry;
import static org.hypertrace.core.attribute.service.v1.Update.TypeCase.DISPLAY_NAME;

import java.util.Map;
import java.util.function.Function;
import org.hypertrace.core.attribute.service.v1.Update;
import org.hypertrace.core.attribute.service.v1.Update.TypeCase;
import org.hypertrace.core.documentstore.model.subdoc.SubDocumentUpdate;

public class AttributeUpdateBuilderImpl implements AttributeUpdateBuilder {
  private static final int MAX_STRING_LENGTH = 1000;

  private static final String DISPLAY_NAME_SUB_DOC_PATH = "display_name";

  private static final Map<TypeCase, Function<Update, SubDocumentUpdate>> UPDATE_PROVIDER_MAP =
      Map.ofEntries(entry(DISPLAY_NAME, AttributeUpdateBuilderImpl::getDisplayNameUpdate));

  private static SubDocumentUpdate getDisplayNameUpdate(final Update update) {
    validateMaxLength(update.getDisplayName());
    return SubDocumentUpdate.of(DISPLAY_NAME_SUB_DOC_PATH, update.getDisplayName());
  }

  private static void validateMaxLength(final String string) {
    if (string.length() > MAX_STRING_LENGTH) {
      throw new IllegalArgumentException(
          String.format(
              "Length cannot be more than allowed (%d). '%s' is exceeding this limit",
              MAX_STRING_LENGTH, string));
    }
  }

  @Override
  public SubDocumentUpdate buildUpdate(final Update update) {
    final TypeCase typeCase = update.getTypeCase();
    final Function<Update, SubDocumentUpdate> updater = UPDATE_PROVIDER_MAP.get(typeCase);

    if (updater == null) {
      throw new UnsupportedOperationException(
          String.format("Updating %s is not supported yet", typeCase));
    }

    return updater.apply(update);
  }
}
