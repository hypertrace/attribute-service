plugins {
  `java-library`
  jacoco
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  api(project(":attribute-service-api"))
  implementation(project(":attribute-service-tenant-api"))

  implementation("org.hypertrace.core.documentstore:document-store:0.7.20")
  implementation("org.hypertrace.core.grpcutils:grpc-context-utils:0.7.2")

  implementation("com.fasterxml.jackson.core:jackson-databind:2.13.2.2")
  implementation("com.typesafe:config:1.4.1")
  implementation("org.slf4j:slf4j-api:1.7.32")
  implementation("com.google.protobuf:protobuf-java-util:3.19.2")
  implementation("com.google.guava:guava:31.1-jre")

  testImplementation("org.mockito:mockito-core:4.2.0")
  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.1")
}

tasks.test {
  useJUnitPlatform()
}
