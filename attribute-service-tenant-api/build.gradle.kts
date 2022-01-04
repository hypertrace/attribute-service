plugins {
  `java-library`
  jacoco
  id("org.hypertrace.jacoco-report-plugin")
}

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}

tasks.test {
  useJUnitPlatform()
}
