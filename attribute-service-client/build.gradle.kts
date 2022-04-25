plugins {
  `java-library`
  id("org.hypertrace.publish-plugin")
}

dependencies {
  api(project(":attribute-service-api"))
  api("com.typesafe:config:1.4.1")

  implementation("org.hypertrace.core.grpcutils:grpc-client-utils:0.7.2")
}
