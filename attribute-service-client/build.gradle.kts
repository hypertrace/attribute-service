plugins {
  `java-library`
  alias(commonLibs.plugins.hypertrace.publish)
}

dependencies {
  api(platform(commonLibs.hypertrace.bom))
  api(projects.attributeServiceApi)
  api(commonLibs.typesafe.config)

  implementation(commonLibs.hypertrace.grpcutils.client)
}
