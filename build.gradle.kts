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
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.qualitygate.wait", "true")
    }
}

project(":app") {
    sonar {
        properties {
            property(
                "sonar.coverage.jacoco.xmlReportPaths",
                layout.buildDirectory.file("reports/jacoco/jacocoTestReport/jacocoTestReport.xml").get().asFile
            )
            property(
                "sonar.androidLint.reportPaths",
                layout.buildDirectory.file("reports/lint-results-debug.xml").get().asFile
            )
            property(
                "sonar.exclusions",
                "**/build/**,**/generated/**,**/ui/**,**/res/**,**/AndroidManifest.xml,**/*.xml,**/*.pdf"
            )
            property("sonar.test.exclusions", "**/build/**,**/androidTest/**")
            property(
                "sonar.coverage.exclusions",
                "**/ui/**,**/MainActivity.kt,**/config/**,**/*Application.kt,**/BuildConfig.*,**/R.class,**/R$*.class"
            )
        }
    }
}

sonar {
    properties {
        property(
            "sonar.exclusions",
            "**/build/**,**/generated/**,**/gradlew**"
        )
    }
}

tasks.named("sonar") {
    dependsOn(":app:lint")
    dependsOn(":app:jacocoTestReport")
}
