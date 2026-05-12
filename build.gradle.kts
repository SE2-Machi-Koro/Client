plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.sonarqube)
}

sonar {
    properties {
        property("sonar.projectKey", "SE2-Machi-Koro_Client")
        property("sonar.organization", "se2-machi-koro")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.sources", "app/src/main/java")
        property("sonar.tests", "app/src/test/java")
        property("sonar.sourceEncoding", "UTF-8")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
        )
        property("sonar.qualitygate.wait", "true")
        property(
            "sonar.exclusions",
            "**/build/**,**/generated/**,**/gradlew**,**/ui/**,**/res/**," +
                    "**/AndroidManifest.xml,**/*.xml,**/*.java,**/MainActivity.kt"
        )
        property("sonar.test.exclusions", "**/build/**,**/androidTest/**")
        property("sonar.coverage.exclusions", "**/MainActivity.kt")
    }
}