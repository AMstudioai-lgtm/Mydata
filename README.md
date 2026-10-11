# Mydata

Application Android de bases de données locales et de tableaux personnalisés.
Vos données sont stockées sur votre appareil et restent accessibles sans connexion internet.

## Téléchargement et installation

1. Rendez-vous sur la page des versions : https://github.com/AMstudioai-lgtm/Mydata/releases/latest
2. Téléchargez le fichier `.apk`.
3. Autorisez l'installation depuis votre navigateur ou votre gestionnaire de fichiers
   (Android demande une confirmation pour les applications hors du Play Store).

## Mise à jour d'une version à l'autre

**À partir de la version 1.2.0, vous pouvez installer une nouvelle version directement,
sans désinstaller l'ancienne.** L'application se met à jour normalement et vos données
locales sont conservées.

Il suffit de télécharger le `.apk` de la nouvelle version et de lancer l'installation.

### Cas particuliers

- **Vous étiez sur une version antérieure à la 1.2.0** (jusqu'à la 1.1.1 incluse) :
  la clé de signature a changé à ce moment-là. Android refuse la mise à jour sur place.
  Désinstallez une fois l'ancienne version, installez la 1.2.0 ou une version plus
  récente, et les mises à jour suivantes se feront ensuite sans désinstallation.
- **Vous réinstallez complètement l'application** (après une désinstallation) :
  vos données locales sont effacées, car elles ne vivaient que sur l'appareil.
  Voir « Retrouver ses données » plus bas.

## Tutoriel — premiers pas

### 1. Créer une table

Ouvrez le menu latéral (icône ☰ en haut à gauche), puis appuyez sur le **+** de la
section « database ». La nouvelle table s'appelle « Database N » et s'ouvre aussitôt.

Vous pouvez aussi appuyer sur **Créer une database** depuis l'écran d'accueil si aucune
table n'existe encore.

Pour supprimer une table, utilisez l'icône de corbeille dans le menu latéral, ou le
bouton **Supprimer la database** sur son bandeau supérieur.

### 2. Créer une propriété (une colonne)

Sur une table vide, appuyez sur **Ajouter propriété** (ou **Modèle Standard** pour
partir d'un jeu de colonnes prêt à l'emploi : Nom, Catégorie, Statut, Montant, Date).

Une boîte de dialogue demande :

- **Nom de la colonne** : par exemple « P&L ($) », « Stratégie », « Lot ».
- **Type** de la colonne :
  - **Texte** — texte libre.
  - **Nombre** — valeur numérique. Un total et une moyenne sont calculés
    automatiquement en bas du tableau. Un format d'affichage est proposé :
    nombre libre, pourcentage (`%`) ou dollars (`$`).
  - **Date** — affiche un sélecteur de date, au format `AAAA-MM-JJ`.
  - **Sélection** — choix parmi une liste définie par vous : indiquez les options
    dans le champ « Options », séparées par des virgules (ex. `Long, Short`).

Appuyez sur **Ajouter** pour valider.

### 3. Créer une ligne

Appuyez sur **+ Ligne** en bas de l'écran. Une ligne vide est ajoutée.

Chaque cellule se remplit selon le type de sa colonne :

| Type | Comment remplir |
| --- | --- |
| Texte | Saisissez, puis validez avec la touche Entrée |
| Nombre | Saisissez un nombre (la virgule est acceptée) |
| Date | Appuyez sur la cellule, choisissez une date |
| Sélection | Appuyez sur la cellule, choisissez dans la liste |

Appuyez sur une ligne pour ouvrir son détail : vous y retrouvez ses propriétés,
un espace pour ajouter des images, et une zone de notes libres en dessous.
La corbeille en fin de ligne supprime cette ligne.

## Où sont stockées vos données

Tout est enregistré **localement sur votre téléphone**, dans la base de données interne
de l'application. Aucune donnée n'est envoyée sur internet sans que vous ne vous
connectiez à un compte. L'application fonctionne donc entièrement hors connexion.

## Retrouver ses données

Si vous réinstallez l'application, la base locale est repartie de zéro. Pour tout
récupérer, connectez-vous à votre compte :

1. Menu latéral → **Paramètres** → « Connexion / Créer un compte ».
2. Une fois connecté, la section **Synchronisation cloud** apparaît plus bas.
3. Appuyez sur **Synchroniser maintenant** : vos tables, colonnes et lignes sont
   récupérées depuis votre espace en ligne.

Sans compte connecté, vos données restent uniquement sur l'appareil.
