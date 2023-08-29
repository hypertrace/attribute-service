plugins {
  `java-library`
  jacoco
  alias(commonLibs.plugins.hypertrace.jacoco)
  alias(commonLibs.plugins.hypertrace.publish)
}

dependencies {
  api(projects.attributeServiceApi)
  api(platform(commonLibs.hypertrace.bom))
  implementation(projects.attributeProjectionFunctions)
  implementation(commonLibs.guava)
  testImplementation(commonLibs.junit.jupiter)
}

tasks.test {
  useJUnitPlatform()
}
