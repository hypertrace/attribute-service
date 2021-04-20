package org.hypertrace.core.attribute.service.projection.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ConcatenateOrNullTest {

  @Test
  void concatenatesNormalStrings() {
    assertEquals("foobar", ConcatenateOrNull.concatenate("foo", "bar"));
    assertEquals("foo", ConcatenateOrNull.concatenate("foo", ""));
    assertEquals("bar", ConcatenateOrNull.concatenate("", "bar"));
  }

  @Test
  void returnsNullIfAnyNullArgs() {
    assertNull(ConcatenateOrNull.concatenate("foo", null));
    assertNull(ConcatenateOrNull.concatenate(null, "bar"));
    assertNull(ConcatenateOrNull.concatenate(null, null));
  }
}
