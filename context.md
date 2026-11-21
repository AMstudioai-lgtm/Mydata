# Mydata — Contexte Projet

## Vision & Périmètre actuel
- Application Android **Mydata** (Kotlin + Jetpack Compose + Room + synchronisation optionnelle Supabase) dédiée aux bases de données et tableaux personnalisés offline-first, avec pièces jointes photo et sauvegarde/restauration ZIP via SAF.
- Cible : module unique `:app` (`com.example`), `minSdk = 24`, `targetSdk = 36`.

## Ce qui est fait
- **Audit & restructuration en couches indépendantes** :
  - `domain/` (`model/`, `logic/`) : modèles métier purs et calculateurs sans dépendance Android/Room/Compose (`TableQueryCalculator`, `PropertyFormatRules`, `BackupCodec`, `TrashRules`, `ConflictResolver`, `RowValuesJsonReader`).
  - `data/` (`db/`, `repository/`, `backup/`, `images/`, `sync/`, `auth/`, `session/`, `update/`) : persistance Room, stockage image fichier/cloud (`RowImageFileStore`, `ImageStore`), export/import ZIP (`BackupManager`), synchronisation et authentification.
  - `viewmodel/` : orchestration d'état (`JournalViewModel`, `AuthViewModel`, `SyncViewModel`).
  - `ui/` (`theme/`, `shell/`, `journal/`, `settings/`, `trash/`, `auth/`, `update/`) : présentation Compose M3 découplée des calculs et de l'infrastructure fichiers.
  - `di/` : conteneur d'injection manuel (`AppContainer`).
- **Design System Monochrome + Accent** :
  - Fond noir pur (`#000000`), surfaces gris anthracite plates (`#121214`, `#1A1A1D`), bordures fines (`#26262A`), texte blanc (`#FAFAFA`) et gris lisibles (`#A1A1AA`, `#6B6B73`), accent Bleu Cobalt (`#2563EB` / `OnAccent = #FAFAFA`) pour les actions clés.
  - Typographie locale installée (`PlusJakartaSans` pour le texte/titres, `JetBrainsMono` pour les nombres, dates et métadonnées).
- **Correctif Export / Réimport ZIP (`BackupManager`, `SettingsPage`, `DAO`, `JournalViewModel`)** :
  - Ouverture du sélecteur de fichiers élargie (`application/zip`, `application/x-zip-compressed`, `application/octet-stream`, `*/*`).
  - Fusion sans perte (`@Upsert` sur les DAOs Room au lieu de `REPLACE` qui déclenchait `ON DELETE CASCADE`).
  - Réattribution des tables, propriétés et lignes restaurées à la session active (`sessionProvider?.currentUserId()`) et sélection automatique de la première table active restaurée.
  - Lecture fiable des images (`file://`, chemin direct, `content://`, et secours cache/cloud `ImageStore`) + réécriture des URIs locales dans `filesDir/row_images/restored_<rowId>/`.

## Décisions prises
- Conservation du module unique `:app` imposé par l'environnement AI Studio Build avec séparation stricte par packages (`domain/`, `data/`, `viewmodel/`, `ui/`, `di/`).
- Comportement d'import : **fusionner avec les données locales existantes et rattacher à la session active**.
- Parseur JSON pur Kotlin (`RowValuesJsonReader`) dans `domain/logic` pour garantir la testabilité JVM sans stub `org.json`.

## Ce qu'il reste à faire / Suivi manuel
- Exécuter `supabase/schema.sql` côté serveur Supabase pour les environnements utilisant la synchronisation cloud de la colonne `properties.format`.
- Renseigner les variables `SUPABASE_KEY` et `SUPABASE_URL` dans les Secrets AI Studio / `.env` si la synchronisation cloud est activée.

## Pistes prévues (vX)
- Export CSV/Markdown par table.
- Filtres multi-colonnes avancés et vues enregistrées dans `domain/logic`.
