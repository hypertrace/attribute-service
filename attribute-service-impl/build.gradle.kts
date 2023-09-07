plugins {
  `java-library`
  jacoco
  alias(commonLibs.plugins.hypertrace.jacoco)
}

dependencies {
  api(projects.attributeServiceApi)
  implementation(projects.attributeServiceTenantApi)
  implementation(commonLibs.hypertrace.documentstore)
  implementation(commonLibs.hypertrace.grpcutils.context)
  implementation(commonLibs.jackson.databind)
  implementation(commonLibs.typesafe.config)
  implementation(commonLibs.slf4j2.api)
  implementation(commonLibs.protobuf.javautil)
  implementation(commonLibs.guava)

  testImplementation(commonLibs.mockito.core)
  testImplementation(commonLibs.junit.jupiter)
  testImplementation(commonLibs.log4j.slf4j2.impl)
}

tasks.test {
  useJUnitPlatform()
}
