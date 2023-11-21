plugins {
  `java-library`
  alias(commonLibs.plugins.hypertrace.publish)
}

dependencies {
  api(projects.attributeServiceApi)

  implementation(commonLibs.hypertrace.grpcutils.client)
  implementation(commonLibs.hypertrace.grpcutils.context)
  implementation(commonLibs.hypertrace.framework.metrics)
  implementation(commonLibs.slf4j2.api)
  implementation(commonLibs.typesafe.config)
  implementation(commonLibs.guava)

  annotationProcessor(commonLibs.lombok)
  compileOnly(commonLibs.lombok)

  testImplementation(commonLibs.junit.jupiter)
  testImplementation(commonLibs.mockito.core)
  testImplementation(commonLibs.mockito.junit)
  testImplementation(commonLibs.grpc.core)
  testImplementation(commonLibs.log4j.slf4j2.impl)
}

tasks.test {
  useJUnitPlatform()
}
