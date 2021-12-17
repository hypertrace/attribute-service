plugins {
  `java-library`
  jacoco
  id("org.hypertrace.jacoco-report-plugin")
  id("org.hypertrace.publish-plugin")
}

dependencies {
  api(project(":attribute-service-api"))
  api("io.reactivex.rxjava3:rxjava:3.0.5")
  api("io.grpc:grpc-api:1.42.0")

  implementation("io.grpc:grpc-stub:1.42.0")
  implementation("org.hypertrace.core.grpcutils:grpc-client-utils:0.6.2")
  implementation("org.hypertrace.core.grpcutils:grpc-client-rx-utils:0.6.2")
  implementation("org.hypertrace.core.grpcutils:grpc-context-utils:0.6.2")
  implementation("com.google.guava:guava:30.1.1-jre")
  annotationProcessor("org.projectlombok:lombok:1.18.20")
  compileOnly("org.projectlombok:lombok:1.18.20")
  implementation("org.slf4j:slf4j-api:1.7.30")

  testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
  testImplementation("org.mockito:mockito-core:3.8.0")
  testImplementation("org.mockito:mockito-junit-jupiter:3.8.0")
  testImplementation("io.grpc:grpc-core:1.42.0")
  testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:2.16.0")
}

tasks.test {
  useJUnitPlatform()
}
