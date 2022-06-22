rootProject.name = "attribute-service-root"

pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
    maven("https://hypertrace.jfrog.io/artifactory/maven")
  }
}

plugins {
  id("org.hypertrace.version-settings") version "0.2.0"
}

include(":attribute-service-api")
include(":attribute-service-client")
include(":attribute-service-impl")
include(":attribute-service")
include(":attribute-service-factory")
include(":attribute-service-tenant-api")
include(":caching-attribute-service-client")
include(":attribute-projection-functions")
include(":attribute-projection-registry")

