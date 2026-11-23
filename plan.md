# Plan Opérationnel — Mydata

## 1. Architecture & Modularité
- [x] Séparation stricte en couches (`domain/model`, `domain/logic`, `data/`, `viewmodel/`, `ui/`, `di/`).
- [x] Extraction des calculateurs métier purs et gestion physique des fichiers photo hors de l'UI.

## 2. Design System (Noir / Gris / Blanc + Accent Bleu Cobalt)
- [x] Thème sombre noir pur + gris plats + blanc + accent Bleu Cobalt (`#2563EB`) et polices locales `PlusJakartaSans` / `JetBrainsMono`.

## 3. Correctif Export / Réimport ZIP (Tables & Photos)
- [x] Fusion `@Upsert`, réattribution à la session active, restauration complète des images locales et tests Robolectric.

## 4. Migration Supabase -> Firebase Firestore & Google Sign-In
- [x] Provisionner Firebase (`ai-project-manager-503720`, base Firestore nommée, SHA-1/SHA-256 vérifiés dans `app/google-services.json`).
- [x] Définir `firebase-blueprint.json`, durcir `firestore.rules`, configurer `firebase.json` (base unique) et valider par `firestore.test.js` sur émulateur local (port `8085`).
- [x] Implémenter Google Sign-In via Jetpack Credential Manager (`GetSignInWithGoogleOption` + `GetGoogleIdOption`), `FirebaseAuthService`, `AuthScreen`, `AuthDialog` et gating `key(uid)` dans `MainActivity.kt`.
- [x] Migrer la synchronisation LWW vers `FirestoreSyncClient` / `FirestoreSyncModels` / `SyncRepository` et supprimer les fichiers Supabase obsolètes.
- [x] Vérifier compilation, tests unitaires/Robolectric (`FirestoreSyncMapperTest`, etc.) et déployer les règles Firestore (`DeployRules`).
