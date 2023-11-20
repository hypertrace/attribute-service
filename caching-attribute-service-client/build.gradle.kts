plugins {
  `java-library`
  jacoco
  alias(commonLibs.plugins.hypertrace.jacoco)
  alias(commonLibs.plugins.hypertrace.publish)
}

dependencies {
  api(projects.attributeServiceApi)
  api(commonLibs.rxjava3)
  api(commonLibs.grpc.api)

  implementation(commonLibs.grpc.stub)
  implementation(commonLibs.hypertrace.grpcutils.client)
  implementation(commonLibs.hypertrace.grpcutils.rx.client)
  implementation(commonLibs.hypertrace.grpcutils.context)
  implementation(commonLibs.hypertrace.framework.metrics)
  implementation(commonLibs.guava)
  implementation(commonLibs.slf4j2.api)
  implementation(commonLibs.typesafe.config)

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
