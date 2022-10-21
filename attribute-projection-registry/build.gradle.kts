plugins {
  `java-library`
  jacoco
  id("org.hypertrace.jacoco-report-plugin")
  id("org.hypertrace.publish-plugin")
}

dependencies {
  api(projects.attributeServiceApi)
  implementation(projects.attributeProjectionFunctions)
  implementation(libs.google.guava)
  testImplementation(libs.junit.jupiter)
}

tasks.test {
  useJUnitPlatform()
}
