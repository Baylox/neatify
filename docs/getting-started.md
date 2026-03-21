# Démarrage rapide

## Prérequis

- **Java 21** ou supérieur
- **Maven 3.8+** (ou utiliser le wrapper `mvnw` inclus — aucune installation requise)

```bash
java -version   # doit afficher Java 21+
```

---

## Build

```bash
# Compiler et générer le JAR autonome
./mvnw clean package

# L'artefact est dans :
target/neatify.jar
```

Le JAR est autonome (toutes les dépendances sont incluses via Maven Shade Plugin). Aucune installation supplémentaire n'est nécessaire.

---

## Lancer Neatify

### Mode interactif (recommandé pour débuter)

```bash
java -jar target/neatify.jar
```

Un menu s'affiche. Sélectionnez `1` pour organiser un dossier, `2` pour créer un fichier de règles, `3` pour annuler la dernière opération.

Voir [Mode interactif](interactive-mode.md) pour le guide complet.

### Mode CLI — aperçu rapide (dry-run)

```bash
# Avec les règles par défaut intégrées
java -jar target/neatify.jar --source ~/Downloads --use-default-rules

# Avec un fichier de règles custom
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties
```

Par défaut, Neatify fonctionne en **dry-run** : il affiche ce qu'il ferait sans déplacer aucun fichier.

### Mode CLI — appliquer les changements

```bash
# Appliquer avec les règles par défaut
java -jar target/neatify.jar --source ~/Downloads --use-default-rules --apply

# Appliquer avec un fichier de règles, stratégie de collision skip
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties --apply --on-collision skip
```

---

## Exemple complet

**Avant :**
```
Downloads/
  rapport.pdf
  photo.jpg
  archive.zip
  notes.txt
  video.mp4
```

**Commande :**
```bash
java -jar target/neatify.jar --source ~/Downloads --use-default-rules --apply
```

**Après :**
```
Downloads/
  Documents/
    rapport.pdf
    notes.txt
  Images/
    photo.jpg
  Archives/
    archive.zip
  Videos/
    video.mp4
```

---

## Annuler la dernière opération

```bash
java -jar target/neatify.jar --source ~/Downloads --undo
```

Neatify conserve un journal de chaque opération dans `.neatify/runs/`. Voir [Système d'annulation](undo-system.md).

---

## Étapes suivantes

- Configurer ses propres règles : [Format des règles](rules-format.md)
- Toutes les options disponibles : [Référence CLI](cli-reference.md)
- Comprendre l'architecture : [Architecture](architecture.md)
