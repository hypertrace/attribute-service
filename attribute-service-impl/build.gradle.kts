plugins {
  `java-library`
  jacoco
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  api(project(":attribute-service-api"))
  implementation(project(":attribute-service-tenant-api"))

  implementation("org.hypertrace.core.documentstore:document-store:0.5.4")
  implementation("org.hypertrace.core.grpcutils:grpc-context-utils:0.4.0")

  implementation("com.fasterxml.jackson.core:jackson-databind:2.12.2")
  implementation("com.typesafe:config:1.4.1")
  implementation("org.slf4j:slf4j-api:1.7.30")
  implementation("com.google.protobuf:protobuf-java-util:3.15.6")

  testImplementation("org.mockito:mockito-core:3.8.0")
  testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
  testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:2.14.1")
}

tasks.test {
  useJUnitPlatform()
}
