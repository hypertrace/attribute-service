plugins {
  `java-library`
  jacoco
  alias(commonLibs.plugins.hypertrace.jacoco)
}

dependencies {
  api(platform(commonLibs.hypertrace.bom))
  testImplementation(commonLibs.junit.jupiter)
}

tasks.test {
  useJUnitPlatform()
}
