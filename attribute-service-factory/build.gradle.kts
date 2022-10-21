plugins {
  `java-library`
}

dependencies {
  api(libs.hypertrace.serviceFramework.grpcFramework)

  // Only required because AttributeService constructor test overload uses a doc store API
  compileOnly(libs.hypertrace.document.store)

  implementation(projects.attributeServiceImpl)
}
