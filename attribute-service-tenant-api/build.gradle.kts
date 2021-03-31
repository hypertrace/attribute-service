plugins {
  `java-library`
  jacoco
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
}

tasks.test {
  useJUnitPlatform()
}
