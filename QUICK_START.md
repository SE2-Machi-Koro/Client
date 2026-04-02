# ⚡ Quick Reference - Copia/Incolla Pronto

## 🎯 TL;DR - Cosa Fare Subito

### 1️⃣ Aggiungi i Secrets su GitHub (2 minuti)

Vai a: https://github.com/SE2-Machi-Koro/Client/settings/secrets/actions

Aggiungi 4 secrets (clicca "New repository secret" per ognuno):

#### Secret 1️⃣
```
Name: KEYSTORE_FILE
Value: [Copia da GITHUB_SECRETS_READY.md - è il testo base64 lungo]
```

#### Secret 2️⃣
```
Name: KEYSTORE_PASSWORD
Value: machikoro2024
```

#### Secret 3️⃣
```
Name: KEY_ALIAS
Value: machi_koro_key
```

#### Secret 4️⃣
```
Name: KEY_PASSWORD
Value: machikoro2024
```

---

### 2️⃣ Push i Cambiamenti (1 minuto)

```bash
cd /Users/valentinaschiavon/Documents/se2_machi_koro/Client

git add .
git commit -m "ci: optimize CI/CD pipeline, add keystore signing, and complete documentation"
git push
```

---

### 3️⃣ Monitora il Workflow (5 minuti)

Vai a: https://github.com/SE2-Machi-Koro/Client/actions

Aspetta che completino:
- ✅ `build-and-test` 
- ✅ `deploy` (se su branch main)

---

## 📁 File Importanti

| File | Scopo | Quando Leggerlo |
|---|---|---|
| `GITHUB_SECRETS_READY.md` | Valori da copiaincollare | **ADESSO** ⭐ |
| `SETUP_COMPLETE_CHECKLIST.md` | Checklist completa | Come riferimento |
| `README.md` | Overview del progetto | Per il team |
| `KEYSTORE_SETUP.md` | Come generare keystore | Se devi rigenerarne uno |
| `.env.example` | Template env vars | Per sviluppo locale |

---

## ✅ Verifica Finale

Dopo il push, su GitHub Actions dovresti vedere:

```
✅ build-and-test    → 2-3 minuti
   - Lint
   - Unit tests
   - SonarCloud analysis
   - Test reports uploaded

✅ deploy            → 1-2 minuti (solo su main)
   - Release APK firmato
   - APK artifact uploaded
```

---

## 🎁 Bonus: Comandi Utili

### Build locale
```bash
./gradlew build              # Build debug + release
./gradlew assembleDebug      # Solo debug APK
./gradlew assembleRelease    # Solo release APK firmato
```

### Check Keystore Locale
```bash
keytool -list -v -keystore machi_koro.jks -alias machi_koro_key
# Password: machikoro2024
```

### Scarica APK da GitHub
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

### Verifica APK è Firmato
```bash
jarsigner -verify -verbose app-release.apk
```

---

## ⚠️ Non Dimenticare!

- [ ] Aggiungi i 4 secrets su GitHub
- [ ] Verifica i secrets nel branch settings
- [ ] Fai il push
- [ ] Guarda il workflow su GitHub Actions
- [ ] Scarica l'APK dai artifacts

---

## 🚨 Se Qualcosa Va Male

**Errore: "SigningConfig missing"**
→ Controlla che i 4 secrets siano aggiunti correttamente

**Errore: "Build failed"**
→ Leggi i log completi su GitHub Actions

**Keystore scaduto**
→ Non è possibile nei prossimi 27 anni 😄

---

**Fatto! Il tuo CI/CD è pronto per la produzione! 🚀**

