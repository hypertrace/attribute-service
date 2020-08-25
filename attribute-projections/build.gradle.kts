plugins {
  `java-library`
  jacoco
  id("org.hypertrace.jacoco-report-plugin")
  id("org.hypertrace.publish-plugin")
}

dependencies {
  api(project(":attribute-service-api"))
  implementation("com.github.f4b6a3:uuid-creator:2.7.7")
}

tasks.test {
  useJUnitPlatform()
}