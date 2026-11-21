# AGENTS.md — Conventions Techniques Mydata

## Stack & Structure
- **Projet** : Android Kotlin + Jetpack Compose (M3), Room (SQLite), Coroutines/Flow, OkHttp/Moshi, Secrets Gradle Plugin.
- **Module unique `:app`**, namespace `com.example`, `applicationId = "com.aistudio.myapp.trjnl"`.
- **Séparation stricte par couches** :
  - `com.example.domain.model` / `com.example.domain.logic` : modèles immuables et règles métier pures (zéro import `android.*`, `androidx.*`, `org.json.*`).
  - `com.example.data.*` : Room (`db/`), fichiers/images (`images/`), archives ZIP (`backup/`), réseau/sync/auth (`sync/`, `auth/`, `update/`, `session/`), dépôt (`repository/`).
  - `com.example.viewmodel` : `StateFlow` UI et orchestration asynchrone.
  - `com.example.ui.*` : Composables déclaratifs M3 uniquement (`theme/`, `shell/`, `journal/`, `settings/`, `trash/`, `auth/`, `update/`).
  - `com.example.di` : injection manuelle via `AppContainer`.

## Règles de développement
- Ne jamais créer de dossier fourre-tout (`utils/`, `helpers/`, `misc/`).
- Toute couleur UI passe par `AppColors` (`ui/theme/Color.kt`) ; aucune couleur hexadécimale codée en dur dans les Composables.
- Boutons sur `AppColors.Accent` : toujours utiliser `contentColor = AppColors.OnAccent`.
- Conserver les `Modifier.testTag(...)` existants sur les éléments interactifs.
- Ne jamais modifier `signingConfigs`, `debug.keystore*`, `applicationId` ou les versions SDK dans `app/build.gradle.kts`.
- Ne jamais commiter de clés API : utiliser `.env.example` / AI Studio Secrets (`BuildConfig`).

## Vérification
- Compilation : `compile_applet` (ou `gradle :app:compileDebugKotlin`).
- Tests JVM & Robolectric : `gradle :app:testDebugUnitTest`.
