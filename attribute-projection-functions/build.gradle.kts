plugins {
  `java-library`
  jacoco
  alias(commonLibs.plugins.hypertrace.jacoco)
  alias(commonLibs.plugins.hypertrace.publish)
}

dependencies {
  api("com.google.code.findbugs:jsr305:3.0.2")
  implementation(commonLibs.uuidcreator)

  testImplementation(commonLibs.junit.jupiter)
}

tasks.test {
  useJUnitPlatform()
}
