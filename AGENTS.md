# AGENTS.md — Conventions Techniques Mydata

## Stack & Structure
- **Projet** : Android Kotlin + Jetpack Compose (M3), Room (SQLite), Firebase Auth (Google Sign-In via Credential Manager), Firebase Firestore (base nommée), Coroutines/Flow, OkHttp (UpdateChecker).
- **Module unique `:app`**, namespace `com.example`, `applicationId = "com.aistudio.myapp.trjnl"`.
- **Séparation stricte par couches** :
  - `com.example.domain.model` / `com.example.domain.logic` : modèles immuables et règles métier pures (zéro import `android.*`, `androidx.*`, `org.json.*`).
  - `com.example.data.*` : Room (`db/`), fichiers/images (`images/`), archives ZIP (`backup/`), Firebase & synchro (`firebase/`, `sync/`, `auth/`, `session/`, `update/`), dépôt (`repository/`).
  - `com.example.viewmodel` : `StateFlow` UI et orchestration asynchrone.
  - `com.example.ui.*` : Composables déclaratifs M3 uniquement (`theme/`, `shell/`, `journal/`, `settings/`, `trash/`, `auth/`, `update/`).
  - `com.example.di` : injection manuelle via `AppContainer`.

## Règles Firebase & Sécurité
- **Base Firestore nommée** : toujours initialiser via `FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))` (jamais la base `(default)` ni `Firebase.firestore`).
- **Google Sign-In exclusif** : utiliser `GetSignInWithGoogleOption` / `GetGoogleIdOption` avec `R.string.default_web_client_id`. Aucun appel `signInAnonymously()` ou email/mot de passe.
- **Règles & Parité** : maintenir `firebase-blueprint.json`, `firestore.rules` et `firestore.test.js` synchronisés 1:1 avec `FirestoreSyncModels.kt` / `FirestoreSyncClient.kt`. Tester localement sur l'émulateur Firestore (port `8085`) avant tout `DeployRules`.

## Règles de développement
- Ne jamais créer de dossier fourre-tout (`utils/`, `helpers/`, `misc/`).
- Toute couleur UI passe par `AppColors` (`ui/theme/Color.kt`) ; aucune couleur hexadécimale codée en dur dans les Composables.
- Boutons sur `AppColors.Accent` : toujours utiliser `contentColor = AppColors.OnAccent`.
- Conserver les `Modifier.testTag(...)` existants sur les éléments interactifs.
- Ne jamais modifier `signingConfigs`, `debug.keystore*`, `applicationId` ou les versions SDK dans `app/build.gradle.kts`.

## Vérification
- Compilation : `compile_applet` (ou `gradle :app:compileDebugKotlin`).
- Tests JVM & Robolectric : `gradle :app:testDebugUnitTest`.
- Tests règles Firestore : `FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js`.
