plugins {
  `java-library`
  jacoco
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  api(projects.attributeServiceApi)
  implementation(projects.attributeServiceTenantApi)

  implementation(libs.hypertrace.document.store)
  implementation(libs.hypertrace.grpc.context.utils)

  implementation(libs.jackson.databind)
  implementation(libs.typesafe.config)
  implementation(libs.slf4j.api)
  implementation(libs.protobuf.java.util)
  implementation(libs.google.guava)

  testImplementation(libs.junit.jupiter)
  testImplementation(libs.bundles.mockito)
  testImplementation(libs.apache.log4j.slf4jImpl)
}

tasks.test {
  useJUnitPlatform()
}
