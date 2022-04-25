plugins {
  `java-library`
  jacoco
  id("org.hypertrace.jacoco-report-plugin")
  id("org.hypertrace.publish-plugin")
}

dependencies {
  api(project(":attribute-service-api"))
  api("io.reactivex.rxjava3:rxjava:3.1.3")
  api("io.grpc:grpc-api")

  implementation("io.grpc:grpc-stub")
  implementation("org.hypertrace.core.grpcutils:grpc-client-utils:0.7.2")
  implementation("org.hypertrace.core.grpcutils:grpc-client-rx-utils:0.7.2")
  implementation("org.hypertrace.core.grpcutils:grpc-context-utils:0.7.2")
  implementation("com.google.guava:guava:31.1-jre")
  annotationProcessor("org.projectlombok:lombok:1.18.22")
  compileOnly("org.projectlombok:lombok:1.18.22")
  implementation("org.slf4j:slf4j-api:1.7.32")

  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("org.mockito:mockito-core:4.2.0")
  testImplementation("org.mockito:mockito-junit-jupiter:4.2.0")
  testImplementation("io.grpc:grpc-core")
  testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.1")
}

tasks.test {
  useJUnitPlatform()
}
