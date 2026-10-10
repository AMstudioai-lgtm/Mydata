# Mydata

Application Android de bases de données locales et de tableaux personnalisés.
Vos données sont stockées sur votre appareil et restent accessibles sans connexion internet.

## Téléchargement et installation

1. Rendez-vous sur la page des versions : https://github.com/AMstudioai-lgtm/Mydata/releases/latest
2. Téléchargez le fichier `.apk`.
3. Autorisez l'installation depuis votre navigateur ou votre gestionnaire de fichiers
   (Android demande une confirmation pour les applications hors du Play Store).

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
    automatiquement en bas du tableau.
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

Appuyez sur une ligne pour ouvrir son détail et la consulter en pleine page.
La corbeille en fin de ligne supprime cette ligne.

## Où sont stockées vos données

Tout est enregistré **localement sur votre téléphone**, dans la base de données interne
de l'application. Aucune donnée n'est envoyée sur internet sans que vous ne vous
connectiez à un compte. L'application fonctionne donc entièrement hors connexion.

## Désinstaller puis réinstaller

Pour passer d'une version à l'autre, il faut **désinstaller l'ancienne version** puis
installer le nouveau `.apk`. Android refuse une mise à jour sur place entre ces versions.

**À noter : désinstaller l'application supprime les données stockées sur l'appareil.**
Vos tables, colonnes et lignes seront perdues si vous n'en avez pas fait de copie.

## Retrouver ses données

Deux possibilités pour ne rien perdre :

1. **Se connecter à un compte.** Depuis le menu latérieur → **Paramètres**, utilisez
   « Connexion / Créer un compte ». Une fois connecté, la section *Synchronisation cloud*
   de la page Paramètres permet d'envoyer vos données vers votre espace en ligne et de les
   récupérer sur un autre appareil. Après une réinstallation, reconnectez-vous puis
   lancez **Synchroniser maintenant** pour retrouver vos tables.
2. **Conserver ses données avant toute manipulation.** Faute de compte connecté, notez ou
   exportez les informations importantes avant de désinstaller.

## Mise à jour vers une nouvelle version

1. Désinstallez la version actuelle.
2. Téléchargez le `.apk` de la dernière version.
3. Installez-le.
4. Si vous utilisiez un compte, reconnectez-vous et appuyez sur **Synchroniser maintenant**
   dans Paramètres.
