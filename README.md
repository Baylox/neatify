# Neatify

Outil de rangement automatique de fichiers basé sur des règles.

---

## Description

Neatify est un utilitaire en ligne de commande écrit en Java qui organise automatiquement vos fichiers dans des dossiers catégorisés selon des règles d'extension personnalisables. Conçu avec simplicité et sécurité à l'esprit, il propose un mode simulation par défaut pour prévisualiser les changements avant de les appliquer.

**Caractéristiques principales :**
- Architecture simple : 4 classes principales, aucune dépendance externe
- Opérations sécurisées : mode dry-run par défaut, gestion des collisions avec renommage automatique
- Extensible : configuration par règles via fichiers `.properties`
- Robuste : validation complète des entrées et gestion des erreurs

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

### Commandes de base

```bash
# Mode dry-run (prévisualisation sans appliquer)
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties

# Appliquer les changements
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties --apply

# Afficher l'aide
java -jar target/neatify.jar --help

# Afficher la version
java -jar target/neatify.jar --version
```

### Mode développement

```bash
# Exécuter directement avec Maven
mvn exec:java -Dexec.args="--source ~/Downloads --rules rules.properties"
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
├── Neatify.java          # Point d'entrée CLI (parsing des arguments)
├── FileMetadata.java     # Record des métadonnées de fichier
├── Rules.java            # Chargement et validation des règles
└── FileMover.java        # Logique métier principale (plan + execute)
```

**Principe de conception :**
1. **Phase de planification :** Scanne le répertoire et calcule les actions nécessaires
2. **Phase d'exécution :** Applique les actions (ou simule en mode dry-run)

---

## Tests

```bash
# Lancer les tests unitaires
mvn test
```

---

## Fonctionnalités de sécurité

- **Dry-run par défaut :** Aucun fichier n'est déplacé sans le flag explicite `--apply`
- **Pas d'écrasement :** Les collisions de fichiers sont résolues avec des suffixes `_1`, `_2`, etc.
- **Validation stricte :** Tous les chemins et règles sont validés avant exécution
- **Fichiers cachés ignorés :** Les fichiers commençant par `.` sont ignorés par défaut
- **Déplacements atomiques :** Utilise `ATOMIC_MOVE` quand disponible pour des opérations plus sûres

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
    │   ├── java/io/neatify/     # Code source
    │   └── resources/            # Ressources
    └── test/
        └── java/io/neatify/     # Tests unitaires
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

---

## Documentation

- [English documentation](README.en.md)
