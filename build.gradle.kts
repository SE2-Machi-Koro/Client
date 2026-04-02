plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("org.sonarqube") version "6.0.1.5171"
}

sonarqube {
    properties {
        property("sonar.projectKey", "SE2-Machi-Koro_Client")
        property("sonar.organization", "se2-machi-koro")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.sources", "app/src/main")
        property("sonar.tests", "app/src/test")
        property("sonar.java.binaries", "app/build/intermediates/javac/debug/classes")
    }
}
