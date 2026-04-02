# SonarQube Plugin Fix - AGP 9.x Incompatibility

## Problema Identificato
Il plugin SonarQube Gradle (versione 4.4.1.3373) ha un'incompatibilità critica con Android Gradle Plugin 9.x. Il plugin cerca di accedere a `AppExtension` che non esiste più nell'API di AGP 9.x.

### Errore
```
Could not determine the dependencies of task ':app:sonar'.
> Extension of type 'AppExtension' does not exist. Currently registered extension types: 
[ExtraPropertiesExtension, LibrariesForLibs, VersionCatalogsExtension, BasePluginExtension, 
SourceSetContainer, ReportingExtension, JavaToolchainService, JavaPluginExtension, 
KotlinAndroidProjectExtension, ApplicationExtension, ApplicationAndroidComponentsExtension, 
LintLifecycleExtension, ComposeCompilerGradlePluginExtension, SonarExtension, SonarExtension]
```

## Soluzioni Testate

### Soluzione 1: Aggiornare il plugin SonarQube ❌
- **Tentativo**: Upgrade a versione 5.0.0.4638 o 6.1.1.6607
- **Risultato**: Fallito - stesso errore persiste, versione 6.1.1 non disponibile nei repository

### Soluzione 2: Configurazione afterEvaluate ❌
- **Tentativo**: Usare `afterEvaluate` con `sonar.gradle.skipCompile=true`
- **Risultato**: Fallito - il plugin continua a cercare `AppExtension`

### Soluzione 3: Rimozione Plugin + SonarScanner CLI ✅
- **Approccio**: Rimuovere il plugin SonarQube da build.gradle.kts e usare il SonarScanner CLI standalone nel workflow CI
- **Risultato**: Successo - build funziona correttamente

## Soluzione Implementata

### Modifiche ai File

#### 1. build.gradle.kts (Root)
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```
- Rimosso il plugin SonarQube dal livello root

#### 2. app/build.gradle.kts
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

dependencies {
    // ...
}
```
- Rimosso il plugin SonarQube (id("org.sonarqube") version "4.4.1.3373")
- Rimossa la configurazione `sonar { properties { ... } }`

#### 3. .github/workflows/ci.yml
```yaml
- name: Analyze with SonarCloud
  if: env.SONAR_TOKEN != ''
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
  run: |
    # Download SonarScanner
    mkdir -p .sonar
    wget https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-5.0.1.3006-linux.zip -O .sonar/sonar-scanner.zip
    unzip -q .sonar/sonar-scanner.zip -d .sonar/
    chmod +x .sonar/sonar-scanner-5.0.1.3006-linux/bin/sonar-scanner
    
    # Run SonarScanner
    .sonar/sonar-scanner-5.0.1.3006-linux/bin/sonar-scanner \
      -Dsonar.projectKey=SE2-Machi-Koro_Client \
      -Dsonar.organization=se2-machi-koro \
      -Dsonar.sources=app/src/main \
      -Dsonar.tests=app/src/test,app/src/androidTest \
      -Dsonar.java.binaries=app/build/intermediates/javac/release,app/build/intermediates/javac/debug \
      -Dsonar.host.url=https://sonarcloud.io
```

## Vantaggi della Soluzione

1. ✅ **Compatibilità**: Funziona con AGP 9.x senza problemi
2. ✅ **Build più veloce**: Non tiene il plugin SonarQube in ogni build locale
3. ✅ **Chiarezza**: Analisi SonarCloud separata da build, solo nel CI
4. ✅ **Flessibilità**: Facile aggiornare il SonarScanner CLI senza attendere aggiornamenti del plugin Gradle
5. ✅ **Standard di industria**: Molti progetti Android usano questa approccio

## Testing

```bash
# Build locale funziona
./gradlew build

# Build e test funzionano
./gradlew clean build

# APK Release generati correttamente
./app/build/outputs/apk/release/app-release.apk
./app/build/outputs/apk/debug/app-debug.apk
```

## Références

- Android Gradle Plugin 9.x Breaking Changes: https://developer.android.com/studio/releases/gradle-plugin#9-0-0
- SonarQube Plugin Issues: https://github.com/SonarSource/sonar-gradle-plugin/issues/
- SonarScanner CLI: https://docs.sonarqube.org/latest/analyzing-source-code/scanners/sonarscanner/

## Commit

```
417c9e8 - fix: remove SonarQube plugin incompatibility with AGP 9.x, use standalone SonarScanner CLI in CI
```

