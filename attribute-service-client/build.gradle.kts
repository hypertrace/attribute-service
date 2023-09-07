plugins {
  `java-library`
  alias(commonLibs.plugins.hypertrace.publish)
}

dependencies {
  api(projects.attributeServiceApi)
  api(commonLibs.typesafe.config)

  implementation(commonLibs.hypertrace.grpcutils.client)
}
