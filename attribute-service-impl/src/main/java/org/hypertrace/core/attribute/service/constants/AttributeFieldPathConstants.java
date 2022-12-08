package org.hypertrace.core.attribute.service.constants;

import org.hypertrace.core.attribute.service.v1.AttributeSource;

public class AttributeFieldPathConstants {
  public static final String ID_PATH = "id";
  public static final String FQN_PATH = "fqn";
  public static final String SCOPE_PATH = "scope";
  public static final String SCOPE_STRING_PATH = "scope_string";
  public static final String KEY_PATH = "key";
  public static final String INTERNAL_PATH = "internal";

  public static final String TENANT_ID_PATH = "tenant_id";
  public static final String SOURCE_METADATA_PATH = "metadata";

  public static String sourceMetadataPathFor(final AttributeSource source) {
    return String.join(".", SOURCE_METADATA_PATH, source.name());
  }
}
