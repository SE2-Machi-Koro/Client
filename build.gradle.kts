// Top-level build file where you can add configuration options common to all sub-projects/modules.


plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("org.sonarqube") version "5.1.0.4882"
}

sonar {
    properties {
        property("sonar.projectKey", "SE2-Macht-Koro_Client")
        property("sonar.organization", "se2-macht-koro")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.sources", "app/src/main")
        property("sonar.tests", "app/src/test")
    }
}