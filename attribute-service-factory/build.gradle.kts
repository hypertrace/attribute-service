plugins {
  `java-library`
}

dependencies {
  api("org.hypertrace.core.serviceframework:platform-grpc-service-framework:0.1.37")

  // Only required because AttributeService constructor test overload uses a doc store API
  compileOnly("org.hypertrace.core.documentstore:document-store:0.7.10")

  implementation(project(":attribute-service-impl"))
}
