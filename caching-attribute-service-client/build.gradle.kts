plugins {
  `java-library`
  jacoco
  id("org.hypertrace.jacoco-report-plugin")
  id("org.hypertrace.publish-plugin")
}

dependencies {
  api(project(":attribute-service-api"))
  api("io.reactivex.rxjava3:rxjava:3.0.5")
  api("io.grpc:grpc-api:1.36.1")

  implementation("io.grpc:grpc-stub:1.36.1")
  implementation("org.hypertrace.core.grpcutils:grpc-client-utils:0.4.0")
  implementation("org.hypertrace.core.grpcutils:grpc-client-rx-utils:0.4.0")
  implementation("org.hypertrace.core.grpcutils:grpc-context-utils:0.4.0")
  implementation("com.google.guava:guava:30.1.1-jre")

  testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
  testImplementation("org.mockito:mockito-core:3.8.0")
  testImplementation("org.mockito:mockito-junit-jupiter:3.8.0")
  testImplementation("io.grpc:grpc-core:1.36.1")
  testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:2.14.1")
}

tasks.test {
  useJUnitPlatform()
}