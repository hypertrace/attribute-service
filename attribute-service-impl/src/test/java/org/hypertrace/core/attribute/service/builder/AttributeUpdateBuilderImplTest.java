package org.hypertrace.core.attribute.service.builder;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Sets;
import com.google.protobuf.Internal.EnumLite;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.hypertrace.core.attribute.service.v1.Update;
import org.hypertrace.core.documentstore.model.subdoc.PrimitiveSubDocumentValue;
import org.hypertrace.core.documentstore.model.subdoc.SubDocumentUpdate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AttributeUpdateBuilderImplTest {

  private static AttributeUpdateBuilder attributeUpdateBuilderImpl;
  private static ArgumentCaptor<Update> captor;

  @BeforeAll
  static void setUp() {
    attributeUpdateBuilderImpl = spy(new AttributeUpdateBuilderImpl());
    captor = ArgumentCaptor.forClass(Update.class);
  }

  @AfterAll
  static void tearDown() {
    verify(attributeUpdateBuilderImpl, atLeastOnce()).buildUpdate(captor.capture());
    verifyAllOneOfCasesCovered(Update.TypeCase.values(), Update::getTypeCase, captor);
  }

  @Test
  void testBuildUpdateDisplayName() {
    final Update update = Update.newBuilder().setDisplayName("new_display_name").build();
    final SubDocumentUpdate result = attributeUpdateBuilderImpl.buildUpdate(update);

    assertEquals("display_name", result.getSubDocument().getPath());
    assertEquals(
        "new_display_name", ((PrimitiveSubDocumentValue) result.getSubDocumentValue()).getValue());
  }

  private static <SOURCE, ENUM extends Enum<ENUM> & EnumLite> void verifyAllOneOfCasesCovered(
      final ENUM[] allValues,
      final Function<SOURCE, ENUM> caseEnumMapper,
      final ArgumentCaptor<SOURCE> captor) {
    final Set<ENUM> coveredValues =
        captor.getAllValues().stream().map(caseEnumMapper).collect(toUnmodifiableSet());

    // Ignore the enum (NOT_SET) whose number is 0
    final Set<ENUM> validCases =
        Stream.of(allValues).filter(val -> val.getNumber() != 0).collect(toUnmodifiableSet());

    assertEquals(
        validCases,
        coveredValues,
        String.format("Tests missing for cases %s", Sets.difference(validCases, coveredValues)));
  }
}
