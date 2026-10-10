# Mydata

Application Android de bases de donnees locales dynamiques et de tableaux personnalises,
offline-first (style Notion / VS Code dark). Kotlin + Jetpack Compose + Room.

## Telechargement

Chaque push sur `main` declenche le workflow `.github/workflows/android-release.yml` :
build de l APK release, creation du tag de version et publication d une release GitHub
contenant l APK en fichier telechargeable.

Derniere version : https://github.com/AMstudioai-lgtm/Mydata/releases/latest

## Numero de version

La version est celle declaree dans `app/build.gradle.kts` (`versionName` / `versionCode`).
Si le tag correspondant existe deja, le workflow incremente automatiquement le patch
(1.0.1 -> 1.0.2) et pousse le bump avec un commit `[skip ci]`.

## Signature

Par defaut, le workflow genere une cle jetable : l APK est installable mais sa signature
change a chaque release (desinstaller l ancienne version avant de mettre a jour).

Pour une signature stable (mises a jour directes), ajouter les secrets du depot :

| Secret | Contenu |
| --- | --- |
| `KEYSTORE_BASE64` | keystore `.jks` encode en base64 (`base64 -w0 mon-keystore.jks`) |
| `STORE_PASSWORD` | mot de passe du keystore |
| `KEY_PASSWORD` | mot de passe de la cle `upload` |

Generer un keystore si besoin :

    keytool -genkeypair -v -keystore my-upload-key.jks -alias upload \
      -storetype PKCS12 -keyalg RSA -keysize 2048 -validity 10000
