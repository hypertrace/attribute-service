import org.hypertrace.gradle.publishing.HypertracePublishExtension
import org.hypertrace.gradle.publishing.License

plugins {
  alias(commonLibs.plugins.hypertrace.repository)
  alias(commonLibs.plugins.hypertrace.ciutils)
  alias(commonLibs.plugins.hypertrace.publish) apply false
  alias(commonLibs.plugins.hypertrace.jacoco) apply false
  alias(commonLibs.plugins.hypertrace.integrationtest) apply false
  alias(commonLibs.plugins.hypertrace.codestyle) apply false
  alias(commonLibs.plugins.owasp.dependencycheck)
}

subprojects {
  group = "org.hypertrace.core.attribute.service"
  apply(plugin = rootProject.commonLibs.plugins.hypertrace.codestyle.get().pluginId)

  pluginManager.withPlugin(rootProject.commonLibs.plugins.hypertrace.publish.get().pluginId) {
    configure<HypertracePublishExtension> {
      license.set(License.APACHE_2_0)
    }
  }

  pluginManager.withPlugin("java") {
    configure<JavaPluginExtension> {
      sourceCompatibility = JavaVersion.VERSION_11
      targetCompatibility = JavaVersion.VERSION_11
    }
  }
}

dependencyCheck {
  format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.ALL.toString()
  suppressionFile = "owasp-suppressions.xml"
  scanConfigurations.add("runtimeClasspath")
  failBuildOnCVSS = 7.0F
}