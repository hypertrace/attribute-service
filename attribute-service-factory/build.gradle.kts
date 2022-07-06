plugins {
  `java-library`
}

dependencies {
  api("org.hypertrace.core.serviceframework:platform-grpc-service-framework:0.1.36")

  // Only required because AttributeService constructor test overload uses a doc store API
  compileOnly("org.hypertrace.core.documentstore:document-store:0.6.15")

  implementation(project(":attribute-service-impl"))
}
