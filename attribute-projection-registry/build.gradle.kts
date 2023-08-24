plugins {
  `java-library`
  jacoco
  alias(commonLibs.plugins.hypertrace.jacoco)
  alias(commonLibs.plugins.hypertrace.publish)
}

configurations {
  compileClasspath {
    resolutionStrategy.activateDependencyLocking()
  }
  runtimeClasspath {
    resolutionStrategy.activateDependencyLocking()
  }
}

dependencyLocking {
  lockMode.set(LockMode.STRICT)
}

tasks.register("resolveAndLockAll") {
  notCompatibleWithConfigurationCache("Filters configurations at execution time")
  doFirst {
    require(gradle.startParameter.isWriteDependencyLocks) { "${path} must be run from the command line with the `--write-locks` flag" }
  }
  doLast {
    configurations.filter {
      // Add any custom filtering on the configurations to be resolved
      it.isCanBeResolved
    }.forEach { it.resolve() }
  }
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
