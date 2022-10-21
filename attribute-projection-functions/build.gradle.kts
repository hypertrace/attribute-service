plugins {
  `java-library`
  jacoco
  id("org.hypertrace.jacoco-report-plugin")
  id("org.hypertrace.publish-plugin")
}

dependencies {
  api(libs.findbugs.jsr)
  implementation(libs.uuid.creator)

  testImplementation(libs.junit.jupiter)
}

tasks.test {
  useJUnitPlatform()
}
