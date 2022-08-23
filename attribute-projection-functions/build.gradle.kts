plugins {
  `java-library`
  jacoco
  id("org.hypertrace.jacoco-report-plugin")
  id("org.hypertrace.publish-plugin")
}

dependencies {
  api("com.google.code.findbugs:jsr305:3.0.2")
  implementation("com.github.f4b6a3:uuid-creator:5.1.0")

  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}

tasks.test {
  useJUnitPlatform()
}
