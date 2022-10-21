plugins {
  `java-library`
  jacoco
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  testImplementation(libs.junit.jupiter)
}

tasks.test {
  useJUnitPlatform()
}
