# Mydata — Contexte Projet

## Vision & Périmètre actuel
- Application Android **Mydata** (Kotlin + Jetpack Compose + Room + Firebase Auth Google Sign-In + Firebase Firestore) dédiée aux bases de données et tableaux personnalisés offline-first, avec pièces jointes photo locales et sauvegarde/restauration ZIP via SAF.
- Cible : module unique `:app` (`com.example`, `applicationId = "com.aistudio.myapp.trjnl"`), `minSdk = 24`, `targetSdk = 36`.
- Projet Firebase provisionné : `ai-project-manager-503720` (base Firestore nommée `ai-studio-android-mydata-9eb6c64c-b872-4d8e-9f3f-1fa39ca8c325`, région `us-west1`).

## Ce qui est fait
- **Audit & restructuration en couches indépendantes** :
  - `domain/` (`model/`, `logic/`) : modèles métier purs et calculateurs sans dépendance Android/Room/Compose.
  - `data/` (`db/`, `repository/`, `backup/`, `images/`, `sync/`, `auth/`, `firebase/`, `session/`, `update/`) : persistance Room, auth Google/Firebase, synchro Firestore, stockage photo local et ZIP backup.
  - `viewmodel/` : orchestration d'état (`JournalViewModel`, `AuthViewModel`, `SyncViewModel`).
  - `ui/` (`theme/`, `shell/`, `journal/`, `settings/`, `trash/`, `auth/`, `update/`) : présentation Compose M3.
  - `di/` : conteneur d'injection manuel (`AppContainer`).
- **Design System Monochrome + Accent Bleu Cobalt** :
  - Fond noir pur (`#000000`), surfaces gris anthracite plates (`#121214`, `#1A1A1D`), bordures fines (`#26262A`), texte blanc (`#FAFAFA`), accent Bleu Cobalt (`#2563EB` / `OnAccent = #FAFAFA`), polices locales `PlusJakartaSans` et `JetBrainsMono`.
- **Migration Supabase -> Firebase Firestore & Google Sign-In** :
  - Suppression complète de Supabase (`SupabaseAuthService`, `SupabaseSyncClient`, `SupabaseStorageClient`, `supabase/schema.sql`).
  - Authentification exclusive Google Sign-In via Jetpack Credential Manager (`GetSignInWithGoogleOption` interactif + `GetGoogleIdOption` reconnexion silencieuse) et Firebase Auth (`FirebaseAuthService`).
  - Écran de connexion `AuthScreen` + isolation par utilisateur `key(uid)` et `viewModel(key = ...)` dans `MainActivity.kt`.
  - Synchronisation incrémentale LWW vers la base Firestore nommée (`R.string.firestore_database_id`) sur `/users/{userId}/journals`, `/properties`, `/entry_rows` avec modèles natifs (`List`/`Map`), `ServerTimestamp`, `FirestoreErrorHandler` et curseurs isolés par `userId`.
  - Schéma `firebase-blueprint.json`, règles zéro-confiance `firestore.rules` validées sur émulateur local (`firestore.test.js`) et déployées.
- **Export / Réimport ZIP (`BackupManager`)** :
  - Fusion sans perte (`@Upsert`), réattribution à la session active, restauration complète des tables et photos locales.

## Décisions prises
- Conservation du module unique `:app` avec séparation stricte par packages.
- Utilisation obligatoire de la base Firestore nommée via `R.string.firestore_database_id` (jamais la base `(default)`).
- Les photos restent stockées localement sur l'appareil et incluses dans l'export/import ZIP (`ProvisionFirebase` provisionne Firestore et Auth Google, pas Firebase Storage).

## Ce qu'il reste à faire / Suivi manuel
- Optionnel : si une synchronisation cloud des fichiers photo binaires est souhaitée ultérieurement, activer manuellement Firebase Storage dans la console Firebase du projet `ai-project-manager-503720`.

## Pistes prévues (vX)
- Export CSV/Markdown par table.
- Synchronisation temps réel par listeners Firestore (`snapshots()`) en complément de la synchro LWW.
- Filtres multi-colonnes avancés et vues enregistrées dans `domain/logic`.
