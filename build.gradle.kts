// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.sonarqube)
}

val localProperties = java.util.Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
val sonarToken = localProperties.getProperty("sonar.token") ?: System.getenv("SONAR_TOKEN") ?: ""

sonar {
  properties {
    property("sonar.projectName", "Friday")
    property("sonar.projectKey", "ai-weather-monitor")
    property("sonar.host.url", "http://localhost:9000")
    property("sonar.token", sonarToken)
    property("sonar.sources", "src/main/java")
    property("sonar.tests", "src/test/java, src/androidTest/java")
    property("sonar.java.coveragePlugin", "jacoco")
    property("sonar.coverage.jacoco.xmlReportPaths", "${project.rootDir}/app/build/reports/coverage/test/debug/report.xml")
    property("sonar.androidLint.reportPaths", "${project.rootDir}/app/build/reports/lint-results-debug.xml")
  }
}

tasks.named("sonar") {
    dependsOn(":app:lintDebug")
}