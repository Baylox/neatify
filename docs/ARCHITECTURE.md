# Architecture de Neatify

## Vue d'ensemble

Neatify est structuré en deux packages principaux :
- **`cli/`** : Interface utilisateur (ligne de commande, UI, formatage)
- **`core/`** : Logique métier (règles, déplacement, sécurité)

Cette séparation permet de tester la logique métier indépendamment de l'interface utilisateur.

---

## Structure des packages

```
src/main/java/io/neatify/
├── Neatify.java                          # Point d'entrée principal
├── cli/                                   # Interface ligne de commande
│   ├── args/
│   │   ├── ArgumentParser.java           # Parsing des arguments CLI
│   │   └── CLIConfig.java                # Configuration CLI
│   ├── core/
│   │   ├── FileOrganizer.java            # Orchestration de l'organisation
│   │   └── RulesFileCreator.java         # Création assistée de règles
│   ├── ui/
│   │   ├── BannerRenderer.java           # Affichage de la bannière
│   │   ├── ConsoleOutput.java            # Sortie console formatée
│   │   ├── ConsoleUI.java                # Interface console
│   │   ├── HelpPrinter.java              # Affichage de l'aide
│   │   └── InteractiveCLI.java           # Mode interactif
│   ├── util/
│   │   ├── Ansi.java                     # Couleurs ANSI
│   │   ├── AsciiSymbols.java             # Symboles ASCII/Unicode
│   │   └── PreviewRenderer.java          # Rendu de l'aperçu
│   ├── FileOrganizationExecutor.java     # Exécution de l'organisation
│   └── AppInfo.java                      # Informations de version
└── core/                                  # Logique métier
    ├── DefaultRules.java                 # Règles par défaut incluses
    ├── FileMetadata.java                 # Métadonnées de fichier
    ├── FileMover.java                    # Déplacement de fichiers (plan + execute)
    ├── PathSecurity.java                 # Validation sécurité des chemins
    └── Rules.java                        # Chargement et validation des règles
```

---

## Package `core/` : Logique métier

### FileMover.java
**Responsabilité :** Orchestration du déplacement de fichiers

**API principale :**
```java
// Phase 1 : Planification
List<Action> plan(Path sourceDir, Map<String, String> rules)
List<Action> plan(Path sourceDir, Map<String, String> rules, int maxFiles)

// Phase 2 : Exécution
Result execute(List<Action> actions, boolean dryRun)
```

**Modèle de données :**
- `Action(Path source, Path target, String label)` : Une action de déplacement
- `Result(int moved, int failed)` : Résultat de l'exécution

**Fonctionnalités :**
- Scan récursif du dossier source
- Filtrage des fichiers cachés (`.`)
- Gestion atomique des collisions de fichiers
- Protection anti-DOS avec quota de fichiers

### Rules.java
**Responsabilité :** Gestion des règles de rangement

**API principale :**
```java
Map<String, String> load(Path rulesFile)
String getTargetFolder(String extension, Map<String, String> rules)
String sanitizeFolderName(String folderName)
```

**Format des règles :**
```properties
extension=DossierCible
jpg=Images
pdf=Documents/Important
```

**Validations :**
- Normalisation des extensions (minuscules, sans point)
- Blocage des path traversal (`../`)
- Blocage des chemins absolus (`/`, `C:\`)
- Remplacement des caractères invalides

### PathSecurity.java
**Responsabilité :** Validation de sécurité des chemins

**API principale :**
```java
void validateRelativeSubpath(String subpath)
Path safeResolveWithin(Path baseDir, String subpath)
void validateSourceDir(Path dir)
void assertNoSymlinkInAncestry(Path path)
```

**Protections :**
- Détection de path traversal (`../`, `..\\`)
- Blocage des chemins absolus
- Validation des dossiers système
- Détection des liens symboliques

### FileMetadata.java
**Responsabilité :** Représentation des métadonnées de fichier

```java
record FileMetadata(String name, String extension, Path path)
```

### DefaultRules.java
**Responsabilité :** Règles par défaut intégrées

Contient des règles préconfigurées pour les extensions courantes (images, documents, vidéos, etc.).

---

## Package `cli/` : Interface utilisateur

### args/ - Parsing des arguments

**ArgumentParser.java**
- Parse les arguments de ligne de commande
- Valide la présence des arguments requis
- Gère les flags (`--apply`, `--help`, `--version`, `--interactive`)

**CLIConfig.java**
- Configuration immutable de la CLI
- Contient : source, rules, apply, interactive, help, version

### core/ - Orchestration

**FileOrganizer.java**
- Orchestration de haut niveau
- Coordination entre Rules, FileMover et l'affichage

**RulesFileCreator.java**
- Assistant de création de fichiers de règles
- Mode interactif avec suggestions

### ui/ - Interface utilisateur

**InteractiveCLI.java**
- Menu interactif principal
- Navigation dans les options
- Workflow guidé avec confirmations

**ConsoleUI.java**
- Composants d'interface réutilisables
- Prompts, confirmations, sélections

**ConsoleOutput.java**
- Affichage formaté dans la console
- Messages de succès, erreurs, warnings

**BannerRenderer.java**
- Bannière ASCII de l'application
- Barres de progression

**HelpPrinter.java**
- Affichage de l'aide détaillée
- Documentation des commandes

### util/ - Utilitaires d'affichage

**PreviewRenderer.java**
- Rendu de l'aperçu des changements
- Groupement par dossier
- Comptage des duplicatas
- Modes de tri (alpha, extension)

**Ansi.java**
- Gestion des couleurs ANSI
- Activation/désactivation dynamique

**AsciiSymbols.java**
- Symboles ASCII/Unicode pour l'affichage
- Fallback ASCII si Unicode non supporté

---

## Principe de conception

### 1. Séparation des responsabilités

```
User Input → CLI → Core → File System
            ↓
         Display
```

- **CLI** : Collecte les entrées, affiche les sorties
- **Core** : Contient toute la logique métier
- **Pas de logique métier dans CLI** : Testabilité maximale

### 2. Workflow en 2 phases

**Phase 1 : Planification**
```java
List<Action> actions = FileMover.plan(sourceDir, rules);
```
- Scan du dossier
- Calcul des actions nécessaires
- Aucune modification du système de fichiers

**Phase 2 : Exécution**
```java
Result result = FileMover.execute(actions, dryRun);
```
- Application des actions (ou simulation)
- Gestion des collisions
- Retour du résultat

**Avantage :** Permet de prévisualiser avant d'appliquer

### 3. Validation en couches

```
Input → Rules.sanitize → PathSecurity.validate → FileMover.execute
```

Chaque couche ajoute une validation :
1. `Rules` : Normalise et bloque les patterns dangereux
2. `PathSecurity` : Valide les chemins résolus
3. `FileMover` : Vérifie à nouveau au moment de l'exécution

**Principe :** Defense in depth (défense en profondeur)

### 4. Immutabilité

- **Records Java** pour les données (`FileMetadata`, `Action`, `Result`)
- **Configuration immutable** (`CLIConfig`)
- **Collections immutables** retournées (`.toList()`)

**Avantage :** Thread-safety, prévisibilité, pas d'effets de bord

---

## Flux de données

### Mode interactif

```
User
  ↓
InteractiveCLI (menu)
  ↓
FileOrganizer (orchestration)
  ↓
Rules.load() + FileMover.plan()
  ↓
PreviewRenderer.render() → Console
  ↓
User confirmation
  ↓
FileMover.execute()
  ↓
ConsoleOutput (résultat)
```

### Mode CLI

```
Arguments CLI
  ↓
ArgumentParser → CLIConfig
  ↓
FileOrganizationExecutor
  ↓
FileOrganizer (orchestration)
  ↓
Rules.load() + FileMover.plan() + execute()
  ↓
Console output
```

---

## Patterns utilisés

### Pattern : Strategy (Tri)
`PreviewRenderer` utilise `SortMode` pour différentes stratégies de tri (ALPHA, EXT, SIZE)

### Pattern : Builder
`PreviewRenderer.Config` utilise le pattern builder fluent

### Pattern : Template Method
`TestHelper` définit des méthodes template pour les tests

### Pattern : Record (Data Class)
Utilisation extensive des records Java pour les données immutables

---

## Dépendances

**Zéro dépendance externe** en production. Le projet n'utilise que :
- **Java 21 Standard Library**
- **JUnit 5** (tests uniquement)

**Philosophie :** Garder le projet simple, portable et facile à auditer.

---

## Points d'extension

Pour étendre Neatify, voici les points recommandés :

### 1. Nouvelles règles de tri
Implémenter dans `Rules.java` ou créer une interface `Rule`

### 2. Nouveaux formats d'affichage
Ajouter des modes de rendu dans `PreviewRenderer`

### 3. Nouveaux modes de validation
Étendre `PathSecurity` avec de nouvelles vérifications

### 4. Nouvelles sources de règles
Actuellement limité aux fichiers `.properties`, pourrait supporter JSON, YAML, etc.

---

## Considérations de performance

- **Scan de fichiers :** `Files.walk()` avec limite de profondeur
- **Quota configurable :** Limite le nombre de fichiers traités (défaut: 100k)
- **Opérations atomiques :** `StandardCopyOption.ATOMIC_MOVE` quand disponible
- **Pas de buffering excessif :** Stream processing des fichiers

---

## Considérations de sécurité

Voir [SECURITY.md](SECURITY.md) pour les détails complets.

**Résumé :**
- Validation stricte de tous les chemins
- Pas d'exécution de code arbitraire
- Dry-run par défaut
- Gestion défensive des erreurs
