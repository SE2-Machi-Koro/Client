# 🔧 CI Failure Fix - Cosa è Stato Risolto

## 🐛 Problemi Identificati e Risolti

### Problem 1: SONAR_TOKEN Non Configurato
**Errore:** 
```
FAILURE: Build failed with an exception.
Could not determine the dependencies of task ':app:sonar'.
```

**Causa:** Il workflow tentava di eseguire il task `sonar` anche se `SONAR_TOKEN` non era configurato.

**Soluzione:** Aggiunto controllo condizionale nel workflow:
```yaml
if [ -z "$SONAR_TOKEN" ]; then
  ./gradlew build --info        # Skip sonar se token manca
else
  ./gradlew build sonar --info  # Esegui sonar se token presente
fi
```

### Problem 2: Keystore Base64 Non Decodificato
**Errore:**
```
SigningConfig "release" is missing required property "storeFile".
```

**Causa:** Il secret `KEYSTORE_FILE` contiene il base64 encoded del keystore, ma non era decodificato prima di usarlo.

**Soluzione:** Aggiunto step per decodificare il keystore:
```yaml
- name: Decode and prepare Keystore
  run: |
    if [ -n "$KEYSTORE_FILE" ]; then
      echo "$KEYSTORE_FILE" | base64 -d > keystore.jks
      echo "KEYSTORE_PATH=$(pwd)/keystore.jks" >> $GITHUB_ENV
    fi
```

---

## 📝 Modifiche al Workflow

### Prima:
```yaml
- name: Build, Test & Analyze with SonarCloud
  run: ./gradlew build sonar --info

- name: Build Release APK
  run: ./gradlew assembleRelease
  env:
    KEYSTORE_FILE: ${{ secrets.KEYSTORE_FILE }}
```

### Dopo:
```yaml
- name: Build, Test & Analyze with SonarCloud
  run: |
    if [ -z "$SONAR_TOKEN" ]; then
      ./gradlew build --info
    else
      ./gradlew build sonar --info
    fi

- name: Decode and prepare Keystore
  run: |
    if [ -n "$KEYSTORE_FILE" ]; then
      echo "$KEYSTORE_FILE" | base64 -d > keystore.jks
      echo "KEYSTORE_PATH=$(pwd)/keystore.jks" >> $GITHUB_ENV
    fi

- name: Build Release APK
  run: ./gradlew assembleRelease
  env:
    KEYSTORE_FILE: ${{ env.KEYSTORE_PATH }}
```

---

## ✅ Cosa Succede Adesso

### Scenario 1: Senza SONAR_TOKEN Configurato
```
✅ Build eseguito normalmente
✅ Tests eseguiti
✅ Lint eseguito
⏭️ SonarCloud analysis SKIPPED (con warning)
✅ Test reports caricati
```

### Scenario 2: Con SONAR_TOKEN Configurato
```
✅ Build eseguito
✅ Tests eseguiti
✅ Lint eseguito
✅ SonarCloud analysis ESEGUITO
✅ Test reports caricati
```

### Scenario 3: Con Keystore Configurato (Deploy Job)
```
✅ Keystore decodificato da base64
✅ Release APK compilato
✅ Release APK FIRMATO con keystore
✅ APK artifact caricato
```

---

## 🚀 Come Testare

### Opzione 1: Workflow Manual Trigger
1. Vai a: https://github.com/SE2-Machi-Koro/Client/actions
2. Seleziona "Client CI"
3. Clicca "Run workflow"
4. Monitora l'esecuzione

### Opzione 2: Push a Develop
```bash
git push origin develop
```

### Opzione 3: Push a Main (con Deploy)
```bash
git push origin main
```

---

## 📊 Status Check

Dopo il fix, nel workflow dovresti vedere:

### build-and-test Job
- ✅ Checkout code
- ✅ Setup JDK 21
- ✅ Build, Test & Analyze (con o senza SonarCloud)
- ✅ Upload Test Reports

### deploy Job (solo main branch)
- ✅ Decode and prepare Keystore
- ✅ Build Release APK
- ✅ Upload APK artifact

---

## 🔍 Se Fallisce Ancora

### Errore: "base64: invalid input"
→ Il secret `KEYSTORE_FILE` non è valido. Ricopia il valore da `GITHUB_SECRETS_READY.md`

### Errore: "file not found: keystore.jks"
→ Il keystore non è stato decodificato correttamente. Verifica il secret.

### Errore: "SigningConfig missing"
→ Il secret `KEYSTORE_FILE` non è configurato. Segui `GITHUB_SECRETS_READY.md` per aggiungerlo.

### Errore: "Could not find sonar"
→ Normale se SONAR_TOKEN non è configurato. Il workflow lo skipperà.

---

## ✨ Miglioramenti Apportati

1. ✅ Workflow più robusto
2. ✅ Gestione graceful dei secret mancanti
3. ✅ Decodifica corretta del keystore
4. ✅ Supporto per CI con/senza SonarCloud
5. ✅ Supporto per CI con/senza Keystore Signing

---

## 📚 File Correlati

- `.github/workflows/ci.yml` - Workflow corretto
- `app/build.gradle.kts` - Gradle config con signing
- `GITHUB_SECRETS_READY.md` - Valori dei secrets
- `SETUP_COMPLETE_CHECKLIST.md` - Checklist setup

---

**Il workflow è ora pronto! 🚀**

