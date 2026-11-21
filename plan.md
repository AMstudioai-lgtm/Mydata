# Plan Opérationnel — Mydata

## 1. Architecture & Modularité
- [x] Créer la couche `domain/model` et `domain/logic` sans dépendance Android/Room/Compose.
- [x] Extraire les calculs de tableau (`TableQueryCalculator`), le formatage numérique (`PropertyFormatRules`), la gestion de corbeille (`TrashRules`), le codec ZIP JSON (`BackupCodec`) et la résolution de conflits (`ConflictResolver`).
- [x] Extraire la gestion physique des images (`RowImageFileStore`, `RowImageJsonCodec`) hors des composables UI.
- [x] Découper les composants UI lourds (`ShellDrawer`, `TableComponents`).
- **Critère de validation** : aucun calcul métier ou accès `java.io.File`/`BitmapFactory` dans `ui/`, compilation propre.

## 2. Design System (Noir / Gris / Blanc + Accent)
- [x] Redéfinir `Color.kt`, `Type.kt`, `Theme.kt` et la barre système (`MainActivity.kt`) en thème sombre noir pur + gris plats + blanc + accent Bleu Cobalt (`#2563EB`).
- [x] Installer les polices locales `plus_jakarta_sans.ttf` et `jetbrains_mono.ttf`.
- [x] Supprimer les pilules superflues (`DB`, badges neutres) et appliquer le contraste `OnAccent` sur tous les boutons accentués.
- **Critère de validation** : contraste net, hiérarchie typographique claire, transitions douces dans `Shell.kt`.

## 3. Correctif Export / Réimport ZIP (Tables & Photos)
- [x] Remplacer `@Insert(onConflict = REPLACE)` par `@Upsert` dans `JournalDao`, `PropertyDao`, `EntryRowDao`.
- [x] Corriger `BackupManager` (lecture multi-sources des photos à l'export, normalisation des chemins ZIP, réattribution à `activeUserId`, rafraîchissement de `updatedAt`).
- [x] Élargir les types MIME dans `SettingsPage` et auto-sélectionner la table restaurée dans `JournalViewModel`.
- [x] Ajouter `BackupManagerTest` (Robolectric) et `TableQueryCalculatorTest` (JUnit).
- **Critère de validation** : `compile_applet` et `gradle :app:testDebugUnitTest` passent intégralement.
