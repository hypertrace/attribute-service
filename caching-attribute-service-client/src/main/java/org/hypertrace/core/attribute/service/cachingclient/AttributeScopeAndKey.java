package org.hypertrace.core.attribute.service.cachingclient;

import lombok.Value;

@Value
class AttributeScopeAndKey {
  String scope;
  String key;
}
