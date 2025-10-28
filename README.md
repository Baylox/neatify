# Neatify

Outil de rangement automatique de fichiers basé sur des règles.

---

## Description

Neatify est un utilitaire en ligne de commande écrit en Java qui organise automatiquement vos fichiers dans des dossiers catégorisés selon des règles d'extension personnalisables. Conçu avec simplicité et sécurité à l'esprit, il propose un mode simulation par défaut pour prévisualiser les changements avant de les appliquer.

**Caractéristiques principales :**
- Architecture modulaire : packages organisés (cli, core, ui, util), aucune dépendance externe
- Sécurité renforcée : protections contre path traversal, quota anti-DOS, validation stricte des chemins
- Opérations sécurisées : mode dry-run par défaut, gestion atomique des collisions (anti-TOCTOU)
- Interface utilisateur : mode interactif avec aperçu visuel et confirmation
- Extensible : configuration par règles via fichiers `.properties` avec règles par défaut incluses
- Robuste : validation complète des entrées, gestion des erreurs, 60+ tests unitaires

---

## Installation

### Prérequis

- Java 21 ou supérieur
- Maven 3.8+ (ou utiliser le Maven Wrapper fourni)

### Compilation depuis les sources

```bash
# Cloner le dépôt
git clone <url-du-depot>
cd neatify

# Construire le JAR exécutable
mvn clean package

# Ou utiliser le Maven Wrapper (Windows)
.\mvnw.cmd clean package

# Le JAR sera créé dans target/neatify.jar
```

---

## Utilisation

### Mode interactif (recommandé)

Lancez simplement le programme sans arguments pour accéder au menu interactif :

```bash
java -jar target/neatify.jar
```

**Menu disponible :**
1. Organiser des fichiers (avec aperçu et confirmation)
2. Créer un fichier de règles
3. Afficher l'aide
4. Afficher la version
5. Quitter

Le mode interactif guide l'utilisateur étape par étape, affiche un aperçu des changements et demande confirmation avant toute modification.

### Mode ligne de commande

```bash
# Prévisualisation (dry-run)
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties

# Appliquer les changements
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties --apply

# Afficher l'aide
java -jar target/neatify.jar --help

# Afficher la version
java -jar target/neatify.jar --version

# Lancer le mode interactif explicitement
java -jar target/neatify.jar --interactive
```

### Mode développement

```bash
# Exécuter directement avec Maven
mvn exec:java
```

---

## Configuration

Créez un fichier `rules.properties` avec le format suivant :

```properties
# Images
jpg=Images
png=Images
gif=Images

# Documents
pdf=Documents
docx=Documents
txt=Documents

# Code
java=Code
py=Code
js=Code

# Les sous-dossiers sont supportés
csv=Documents/Tableurs
pptx=Documents/Presentations
```

**Format :** `extension=DossierCible`

**Règles :**
- Les extensions sont automatiquement normalisées (minuscules, sans point)
- Les dossiers cibles sont créés automatiquement s'ils n'existent pas
- Les caractères invalides dans les noms de dossiers sont remplacés par `_`
- Les fichiers sans règle correspondante sont ignorés

---

## Architecture

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

**Principe de conception :**
1. **Séparation CLI/Core :** L'interface utilisateur (cli) est séparée de la logique métier (core)
2. **Phase de planification :** Scanne le répertoire et calcule les actions nécessaires
3. **Phase d'exécution :** Applique les actions (ou simule en mode dry-run)
4. **Validation de sécurité :** Chaque chemin est validé avant toute opération

---

## Tests

### Exécution des tests

```bash
# Lancer tous les tests
mvn test

# Lancer un test spécifique
mvn test -Dtest=FileMoverTest

# Lancer les tests de sécurité uniquement
mvn test -Dtest=io.neatify.core.security.*Test
```

### Architecture des tests

```
src/test/java/io/neatify/
├── TestHelper.java                      # Classe de base avec helpers communs
├── cli/
│   ├── args/ArgumentParserTest.java     # Tests du parser d'arguments
│   └── PreviewRendererTest.java         # Tests du rendu d'aperçu
└── core/
    ├── FileMetadataTest.java            # Tests des métadonnées
    ├── FileMoverTest.java               # Tests du déplacement de fichiers
    ├── RulesTest.java                   # Tests du chargement de règles
    └── security/                        # Package dédié aux tests de sécurité
        ├── FileMoverSecurityTestBase.java        # Base pour tests de sécurité
        ├── FileMoverPathTraversalTest.java       # Tests anti path traversal
        ├── FileMoverQuotaTest.java               # Tests anti-DOS (quota)
        ├── FileMoverCollisionTest.java           # Tests anti-TOCTOU
        ├── PathSecurityTest.java                 # Tests de validation de chemins
        └── RulesSecurityTest.java                # Tests de validation de règles
```

**60+ tests couvrant :**
- ✓ Fonctionnalités principales (plan, execute, dry-run)
- ✓ Sécurité (path traversal, quota, collisions)
- ✓ Interface CLI (parsing, interactivité)
- ✓ Rendu et formatage (aperçu, couleurs)

---

## Fonctionnalités de sécurité

### Protection des opérations
- **Dry-run par défaut :** Aucun fichier n'est déplacé sans le flag explicite `--apply`
- **Gestion atomique des collisions (Anti-TOCTOU) :** Les collisions détectées au moment de l'exécution sont résolues avec des suffixes `_1`, `_2`, etc.
- **Déplacements atomiques :** Utilise `ATOMIC_MOVE` quand disponible pour des opérations plus sûres
- **Fichiers cachés ignorés :** Les fichiers commençant par `.` sont ignorés par défaut

### Protection contre les attaques par chemin (Path Traversal)
- **Validation stricte des chemins :** `PathSecurity` bloque les tentatives de path traversal (`../`, `..\\`)
- **Blocage des chemins absolus :** Chemins Unix (`/etc`) et Windows (`C:\`) interdits dans les règles
- **Validation des dossiers système :** Interdiction d'utiliser des dossiers système sensibles comme source
- **Double niveau de protection :** Validation au niveau de `Rules` ET de `FileMover`
- **Vérification des symlinks :** Détection et blocage des liens symboliques dans l'arborescence

### Protection anti-DOS
- **Quota de fichiers :** Limite configurable du nombre de fichiers traités (défaut: 100 000)
- **Validation stricte des règles :** Format et contenu des fichiers de règles vérifiés
- **Gestion des erreurs robuste :** Échec contrôlé en cas d'entrée malveillante

### Architecture de test sécurisée
- **60+ tests unitaires** organisés en packages fonctionnels
- **Tests de sécurité dédiés** : PathSecurityTest, RulesSecurityTest, FileMoverSecurityTest
- **Tests de scénarios d'attaque** : Path traversal, quota, collisions, règles malveillantes

---

## Exemples

### Exemple 1 : Organiser un dossier Téléchargements

```bash
# Créer des règles personnalisées
cat > mes-regles.properties << EOF
pdf=Documents
jpg=Images
mp4=Videos
zip=Archives
EOF

# Prévisualiser les changements
java -jar target/neatify.jar --source ~/Downloads --rules mes-regles.properties

# Appliquer les changements
java -jar target/neatify.jar --source ~/Downloads --rules mes-regles.properties --apply
```

### Exemple 2 : Organiser un projet de code

```properties
java=SourceCode/Java
py=SourceCode/Python
js=SourceCode/JavaScript
md=Documentation
json=Config
yaml=Config
```

---

## Développement

### Structure du projet

```
neatify/
├── pom.xml                       # Configuration Maven
├── rules.properties              # Règles d'exemple
├── README.md                     # Documentation (français)
├── README.en.md                  # Documentation (anglais)
├── LICENSE                       # Licence MIT
└── src/
    ├── main/
    │   ├── java/io/neatify/
    │   │   ├── cli/             # Interface ligne de commande
    │   │   │   ├── args/        # Parsing des arguments
    │   │   │   ├── core/        # Orchestration CLI
    │   │   │   ├── ui/          # Interface utilisateur
    │   │   │   └── util/        # Utilitaires d'affichage
    │   │   ├── core/            # Logique métier
    │   │   └── Neatify.java     # Point d'entrée
    │   └── resources/            # Ressources
    └── test/
        └── java/io/neatify/     # Tests unitaires (60+)
            ├── TestHelper.java   # Helpers communs
            ├── cli/             # Tests CLI
            └── core/            # Tests métier
                └── security/    # Tests de sécurité
```

### Évolutions futures

- [ ] Interface `Rule` pour des règles complexes (par date, taille, motifs regex)
- [ ] Option `--by-date` pour organiser les fichiers par année/mois
- [ ] Option `--report` pour générer un rapport JSON des actions effectuées
- [ ] Flag `--include-hidden` pour traiter les fichiers cachés
- [ ] Structure Maven multi-modules pour support de plugins externes

---

## Licence

Ce projet est sous licence MIT - voir le fichier [LICENSE](LICENSE) pour plus de détails.

---

## Contribution

Les contributions, problèmes et demandes de fonctionnalités sont les bienvenus.

---

## Avertissement

Testez toujours en mode dry-run avant d'appliquer des changements sur des données importantes. Cet outil ne crée pas de sauvegardes automatiques.
