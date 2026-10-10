# Mydata - Suivi de projet

## Description
Application Android locale de bases de donnees dynamiques et tableaux personnalises,
offline-first (style Notion / VS Code dark). Langage : Kotlin + Jetpack Compose + Room.

## Technologies retenues
- Android, Kotlin 2.2.10, Jetpack Compose, Room (SQLite local)
- Gradle 9.3.1 / AGP 9.1.1 (Kotlin DSL), JDK 17, minSdk 24, targetSdk 36
- Firebase (AI + App Check), Retrofit / OkHttp / Moshi
- CI/CD : GitHub Actions

## Module en cours
[x] `.github/workflows/android-release.yml` : build APK a chaque push sur main
    et publication automatique d une release GitHub taguee.

### Fonctionnement du workflow
1. Lit `versionName` / `versionCode` dans `app/build.gradle.kts`.
2. Si le tag `v<versionName>` existe deja, incremente le patch,
   met a jour `build.gradle.kts` et pousse le commit avec `[skip ci]`.
3. Compile `:app:assembleRelease`, renomme en `Mydata-<version>.apk`.
4. Cree le tag `v<version>` et publie la release avec l APK en asset.

## Contraintes et limites connues
- Depot **prive** : les releases ne sont telechargeables que par les
  utilisateurs ayant acces au repo. A rendre public pour distribution.
- Signature : le repo ne contient aucun keystore. Sans secrets configures,
  le workflow genere une cle jetable (APK installable mais non majable).
- Pas de `gradlew` versionne : le workflow installe Gradle 9.3.1
  via `gradle/actions/setup-gradle`.
- `google-services.json` absent du depot (plugin en mode WARN/PASSTHROUGH).
- Tests unitaires non executes dans la CI pour l instant.

## Modules en attente (backlog)
- [ ] Secrets de signature : `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`
      (commit `gradle wrapper` pour versionner le wrapper)
- [ ] Passer le depot en public pour rendre l APK telechargeable
- [ ] Ajouter `gradle :app:test` (tests unitaires) avant le build release
- [ ] Publier aussi l artifact de build dans la release (changelog automatique)
- [ ] Changelog genere depuis les commits

## Historique
- 2026-10-10 : mise en place du workflow de release automatique.
