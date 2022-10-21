plugins {
  `java-library`
  jacoco
  id("org.hypertrace.jacoco-report-plugin")
  id("org.hypertrace.publish-plugin")
}

dependencies {
  api(projects.attributeServiceApi)
  api(libs.rxjava)
  api(libs.grpc.api)

  implementation(libs.grpc.stub)
  implementation(libs.hypertrace.grpc.client.utils)
  implementation(libs.hypertrace.grpc.client.rx.utils)
  implementation(libs.hypertrace.grpc.context.utils)
  implementation(libs.google.guava)
  annotationProcessor(libs.lombok)
  compileOnly(libs.lombok)
  implementation(libs.slf4j.api)

  testImplementation(libs.junit.jupiter)
  testImplementation(libs.bundles.mockito)
  testImplementation(libs.grpc.core)
  testImplementation(libs.apache.log4j.slf4jImpl)
}

tasks.test {
  useJUnitPlatform()
}
