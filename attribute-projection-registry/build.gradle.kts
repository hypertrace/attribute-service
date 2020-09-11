plugins {
  `java-library`
  jacoco
  id("org.hypertrace.jacoco-report-plugin")
  id("org.hypertrace.publish-plugin")
}

dependencies {
  api(project(":attribute-service-api"))
  implementation(project(":attribute-projection-functions"))
  implementation("com.google.guava:guava:29.0-jre")
  testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
}

tasks.test {
  useJUnitPlatform()
}