# Mydata - Suivi de projet

## Description
Application Android de bases de donnees dynamiques et tableaux personnalises,
offline-first (style Notion / VS Code dark). Kotlin + Jetpack Compose + Room.

Depot : https://github.com/AMstudioai-lgtm/Mydata (public)

## Technologies retenues
- Android, Kotlin 2.2.10, Jetpack Compose, Room (SQLite local)
- Gradle 9.3.1 / AGP 9.1.1 (Kotlin DSL), JDK 17, minSdk 24, targetSdk 36
- Firebase (AI + App Check), Retrofit / OkHttp / Moshi
- CI/CD : GitHub Actions

## Module en cours
[x] `.github/workflows/android-release.yml` : build APK a chaque push sur main
    et publication automatique d'une release GitHub taguee.

### Fonctionnement
1. Lit `versionName` / `versionCode` dans `app/build.gradle.kts`.
2. Si le tag `v<versionName>` existe deja, incremente le patch, met a jour
   `build.gradle.kts` et pousse le commit avec `[skip ci]`.
3. Compile `:app:assembleRelease`, renomme en `Mydata-<version>.apk`.
4. Cree le tag `v<version>` et publie la release avec l'APK en asset.
- Option `workflow_dispatch` + `dry_run=true` : build sans publier ni bumper.

## Resultats de validation
| Run | Declencheur | Resultat |
| --- | --- | --- |
| 38069941979 | push workflow | echec : paquet SDK `tools` obsolete |
| 38070023752 | push global.md | echec : meme cause |
| 38070444022 | push correctif SDK | succes - release **v1.0.1** (16,7 Mo) |
| 38070886850 | push README | succes - release **v1.0.2** (bump auto) |
| 38071465706 | dispatch dry_run | succes - build 2 min, aucun effet sur le depot |

APK verifie : Signing Block v2 present (magic `APK Sig Block 42`), zip sain,
classes.dex multiples, 217 entrees. Telechargeable publiquement:
https://github.com/AMstudioai-lgtm/Mydata/releases

## Contraintes et limites connues
- Signature : aucun keystore dans le depot. Sans secrets configures, le workflow
  genere une cle jetable : APK installable mais non majable (desinstaller avant
  chaque mise a jour). Voir section Signature du README.
- Pas de `gradlew` versionne : le workflow installe Gradle 9.3.1
  via `gradle/actions/setup-gradle`.
- `google-services.json` absent du depot (plugin en mode WARN/PASSTHROUGH).
- Warning KSP non bloquant au build (NPE AWT dans le thread interne KSP).
- `android-actions/setup-android@v3` : le paquet `platforms;android-36.1`
  n'est pas fourni par le runner, AGP le telecharge au build.

## Modules en attente (backlog)
- [ ] Secrets de signature : `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`
      pour des mises a jour d'application sans desinstallation
- [ ] Supprimer le warning KSP (mise a jour du plugin KSP)
- [ ] Versionner le wrapper Gradle (`gradle wrapper`)
- [ ] Ajouter `gradle :app:test` (tests unitaires) avant le build release
- [ ] Renommer l'applicationID `com.aistudio.myapp.trjnl` en identifiant definitif

## Historique
- 2026-10-10 : mise en place du workflow de release automatique.
- 2026-10-10 : correctif SDK runner 2026, setup-java v5, option dry_run.
- 2026-10-10 : depot passe en public, README de distribution ajoute.
