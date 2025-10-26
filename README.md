# 📦 Neatify

**Outil de rangement automatique de fichiers** - Organisez vos dossiers en bordel en quelques secondes !

## 🎯 Description

Neatify est un outil CLI en Java qui range automatiquement vos fichiers selon des règles personnalisables basées sur les extensions.

**Caractéristiques :**
- ✅ Simple : 4 classes, zéro dépendance externe
- ✅ Sûr : Mode dry-run par défaut, pas d'écrasement de fichiers
- ✅ Extensible : Règles personnalisables via `.properties`
- ✅ Robuste : Gestion des collisions, validation des entrées

## 🚀 Installation

### Prérequis
- Java 21+
- Maven 3.8+

### Compilation

```bash
# Cloner le projet
git clone <url-du-repo>
cd neatify

# Compiler et créer le JAR exécutable
mvn clean package

# Le JAR sera créé dans target/neatify.jar
```

## 📖 Utilisation

### Commandes de base

```bash
# 1️⃣ Simulation (dry-run) - recommandé pour tester
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties

# 2️⃣ Application réelle des changements
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties --apply

# 3️⃣ Aide
java -jar target/neatify.jar --help

# 4️⃣ Version
java -jar target/neatify.jar --version
```

### Via Maven (développement)

```bash
# Exécuter directement avec Maven
mvn exec:java -Dexec.args="--source ~/Downloads --rules rules.properties"
```

## ⚙️ Configuration des règles

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

# Sous-dossiers possibles
csv=Documents/Tableurs
pptx=Documents/Presentations
```

**Format :** `extension=DossierCible`

- Les extensions sont automatiquement normalisées (minuscules, sans point)
- Les sous-dossiers sont créés automatiquement
- Les caractères interdits dans les noms de dossiers sont remplacés par `_`

## 🏗️ Architecture

```
src/main/java/io/neatify/
├── Neatify.java          # Point d'entrée CLI (parsing args)
├── FileMetadata.java     # Record avec métadonnées fichier
├── Rules.java            # Chargement et validation des règles
└── FileMover.java        # Logique métier (plan + execute)
```

**Principe :**
1. **Plan** : Analyse le dossier et calcule les actions à effectuer
2. **Execute** : Applique les actions (ou simule en dry-run)

## 🧪 Tests

```bash
# Lancer les tests unitaires (à venir)
mvn test
```

## 🛡️ Sécurité

- ✅ **Dry-run par défaut** : Aucun fichier n'est déplacé sans `--apply`
- ✅ **Pas d'écrasement** : Les collisions sont résolues avec suffixe `_1`, `_2`, etc.
- ✅ **Validation stricte** : Tous les chemins et règles sont validés
- ✅ **Fichiers cachés ignorés** : Les fichiers commençant par `.` sont ignorés par défaut
- ✅ **Déplacement atomique** : Utilise `ATOMIC_MOVE` quand possible

## 📋 Exemples

### Exemple 1 : Ranger un dossier Téléchargements

```bash
# Créer des règles pour vos besoins
cat > my-rules.properties << EOF
pdf=Documents
jpg=Images
mp4=Videos
zip=Archives
EOF

# Tester (dry-run)
java -jar target/neatify.jar --source ~/Downloads --rules my-rules.properties

# Appliquer
java -jar target/neatify.jar --source ~/Downloads --rules my-rules.properties --apply
```

### Exemple 2 : Ranger un projet de code

```properties
java=SourceCode/Java
py=SourceCode/Python
js=SourceCode/JavaScript
md=Documentation
json=Config
yaml=Config
```

## 🔧 Développement

### Structure du projet

```
neatify/
├── pom.xml                       # Configuration Maven
├── rules.properties              # Règles d'exemple
├── README.md
└── src/
    ├── main/
    │   ├── java/io/neatify/     # Code source
    │   └── resources/            # Ressources
    └── test/
        └── java/io/neatify/     # Tests unitaires
```

### Évolutions futures possibles

- [ ] Interface `Rule` pour des règles complexes (par date, taille, regex)
- [ ] Option `--by-date` pour organiser par année/mois
- [ ] Option `--report` pour générer un JSON avec les actions effectuées
- [ ] Support des fichiers cachés via `--include-hidden`
- [ ] Multi-modules Maven si besoin de plugins externes

## 📄 Licence

Ce projet est à usage personnel. Vous êtes libre de l'utiliser et de le modifier.

## 🤝 Contribution

Suggestions et améliorations bienvenues !

---

**⚠️ Avertissement :** Testez toujours avec `dry-run` avant d'appliquer sur des données importantes. L'outil ne crée pas de sauvegardes automatiques.
