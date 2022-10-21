plugins {
  `java-library`
  id("org.hypertrace.publish-plugin")
}

dependencies {
  api(projects.attributeServiceApi)
  api(libs.typesafe.config)

  implementation(libs.hypertrace.grpc.client.utils)
}
