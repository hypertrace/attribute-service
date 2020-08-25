package org.hypertrace.core.attribute.service.projection;

import static org.hypertrace.core.attribute.service.projection.StringConcatenation.concatenate;

import com.github.f4b6a3.uuid.UuidCreator;
import com.github.f4b6a3.uuid.creator.rfc4122.NameBasedSha1UuidCreator;
import java.util.List;
import java.util.UUID;
import org.hypertrace.core.attribute.service.v1.AttributeKind;

public class StringHash implements AttributeProjection {

  private static final NameBasedSha1UuidCreator HASHER =
      UuidCreator.getNameBasedSha1Creator()
          .withNamespace(UUID.fromString("5088c92d-5e9c-43f4-a35b-2589474d5642"));

  public static StringHash STRING_HASH = new StringHash();

  public static String hash(List<String> strings) {
    return HASHER.create(concatenate(strings)).toString();
  }

  private StringHash() {}

  @Override
  public AttributeKind getArgumentKindAtIndex(int index) {
    return AttributeKind.TYPE_STRING;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object project(List<Object> arguments) {
    return hash((List<String>) (Object) arguments);
  }
}
