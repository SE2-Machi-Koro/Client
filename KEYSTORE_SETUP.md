# Keystore Setup Guide

Questa guida spiega come generare un keystore per la firma del Release APK di Machi Koro.

## 📋 Prerequisiti

- Java Development Kit (JDK) installato
- Accesso al terminale/command line

## 🔑 Generare un Nuovo Keystore

### 1. Genera il keystore localmente

Esegui questo comando nel terminale:

```bash
keytool -genkey -v -keystore machi_koro.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias machi_koro_key
```

**Spiegazione parametri:**
- `-genkey` - Genera una nuova coppia di chiavi
- `-v` - Output verboso
- `-keystore machi_koro.jks` - Nome del file keystore (puoi cambiarlo)
- `-keyalg RSA` - Algoritmo di crittografia
- `-keysize 2048` - Dimensione della chiave (2048 bit = sicuro)
- `-validity 10000` - Validità in giorni (~27 anni)
- `-alias machi_koro_key` - Nome dell'alias della chiave (puoi cambiarlo)

### 2. Ti verranno chiesti i seguenti dati

Rispondi alle domande interattive:

```
Enter keystore password:                    ← Scegli una password forte
Re-enter new password:
What is your first and last name?           ← Es: John Doe
What is the name of your organizational unit? ← Es: Engineering
What is the name of your organization?      ← Es: SE2 Machi Koro
What is the name of your City or Locality?  ← Es: Milano
What is the name of your State or Province? ← Es: Lombardia
What is the two-letter country code?        ← Es: IT
Is CN=John Doe, OU=Engineering, ... correct? → yes
Enter key password (RETURN if same as keystore password): ← Premi Enter o digita password diversa
```

### 3. Verifica il keystore creato

```bash
keytool -list -v -keystore machi_koro.jks -alias machi_koro_key
```

Ti chiederà la password del keystore.

## 🔐 Configurare GitHub Actions

### Opzione A: Base64-encode il keystore (Consigliato)

1. **Codifica il keystore in base64:**

```bash
base64 -i machi_koro.jks -o keystore_base64.txt
```

(Su macOS/Linux - su Windows usa `certutil` o PowerShell)

2. **Copia il contenuto del file base64:**

```bash
cat keystore_base64.txt
```

3. **Aggiungi su GitHub:**
   - Vai a: Settings → Secrets and variables → Actions
   - Clicca "New repository secret"
   - Name: `KEYSTORE_FILE`
   - Value: Incolla il contenuto base64

### Opzione B: Upload diretto (Per repository private)

Se il repository è privato e sicuro, puoi:

1. Commit il keystore in una cartella `.secrets/` (aggiungere a `.gitignore`)
2. Usare il percorso relativo: `.secrets/machi_koro.jks`

## 📝 Aggiungere gli altri Secrets

Dopo aver configurato `KEYSTORE_FILE`, aggiungi questi secrets:

| Secret Name | Valore | Esempio |
|---|---|---|
| `KEYSTORE_FILE` | Base64 del keystore | (output base64 precedente) |
| `KEYSTORE_PASSWORD` | Password del keystore | `MySecure!Pass123` |
| `KEY_ALIAS` | Alias della chiave | `machi_koro_key` |
| `KEY_PASSWORD` | Password della chiave | `MySecure!Pass123` |

## ✅ Verificare che Funzioni

Una volta configurati i secrets:

1. Fai un push al branch `main`
2. Vai a GitHub → Actions
3. Guarda il workflow "Client CI"
4. Se il job "Build & Upload Release APK" ha successo, il keystore è configurato correttamente!
5. Scarica l'APK firmato dall'artifatto

## 🛡️ Sicurezza

⚠️ **IMPORTANTE:**
- **NON** committare il keystore in git
- Tieni la password del keystore al sicuro
- Usa password forti e uniche
- Se il keystore viene compromesso, dovrai crearne uno nuovo
- Aggiorna i secrets di GitHub se generi un nuovo keystore

## 🆘 Troubleshooting

### Errore: "keytool not found"
Assicurati che JDK sia installato e aggiunto al PATH.

### Errore: "Invalid keystore"
Verifica la password del keystore. Deve essere la stessa usata al momento della creazione.

### Build fallisce con "SigningConfig missing"
Verifica che tutti e 4 i secrets siano configurati in GitHub.

### APK non è firmato
Controlla i log del workflow su GitHub per errori specifici.

## 📚 Link Utili

- [Android Signing Guide](https://developer.android.com/studio/publish/app-signing)
- [Keytool Documentation](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html)
- [GitHub Secrets Documentation](https://docs.github.com/en/actions/security-guides/encrypted-secrets)

