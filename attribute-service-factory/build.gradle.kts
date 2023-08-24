plugins {
  `java-library`
}

dependencies {
  api(commonLibs.hypertrace.framework.grpc)
  api(platform(commonLibs.hypertrace.bom))
  // Only required because AttributeService constructor test overload uses a doc store API
  compileOnly(commonLibs.hypertrace.documentstore)

  implementation(projects.attributeServiceImpl)
}
