package org.hypertrace.core.attribute.service.constants;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hypertrace.core.attribute.service.v1.AttributeSource;
import org.junit.jupiter.api.Test;

class AttributeFieldPathConstantsTest {

  @Test
  void testSourceMetadataPathFor() {
    assertEquals(
        "metadata.EDS", AttributeFieldPathConstants.sourceMetadataPathFor(AttributeSource.EDS));
  }
}
