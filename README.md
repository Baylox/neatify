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

## Installation rapide

### Prérequis

- Java 21 ou supérieur
- Maven 3.8+ (ou utiliser le Maven Wrapper fourni)

### Compilation

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

```bash
java -jar target/neatify.jar
```

**Menu disponible :**
1. Organiser des fichiers (avec aperçu et confirmation)
2. Créer un fichier de règles
3. Afficher l'aide
4. Afficher la version
5. Quitter

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

## Exemple rapide

### Organiser un dossier Téléchargements

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

---

## Sécurité

Neatify intègre de multiples protections :

- ✅ **Dry-run par défaut** : Prévisualisation obligatoire avant modification
- ✅ **Anti path traversal** : Blocage des tentatives d'accès en dehors du dossier source
- ✅ **Anti-DOS** : Quota configurable (défaut: 100 000 fichiers)
- ✅ **Anti-TOCTOU** : Gestion atomique des collisions de fichiers
- ✅ **Validation stricte** : Tous les chemins et règles sont vérifiés
- ✅ **33 tests de sécurité** dédiés aux scénarios d'attaque

**⚠️ Avertissement :** Testez toujours en mode dry-run avant d'appliquer des changements sur des données importantes. Cet outil ne crée pas de sauvegardes automatiques.

📖 **Voir [docs/SECURITY.md](docs/SECURITY.md) pour les détails complets**

---

## Documentation

### 📚 Documentation détaillée

- **[ARCHITECTURE.md](docs/ARCHITECTURE.md)** - Architecture du projet, patterns utilisés, flux de données
- **[TESTING.md](docs/TESTING.md)** - Guide complet des tests, conventions, architecture des tests
- **[SECURITY.md](docs/SECURITY.md)** - Modèle de menaces, protections implémentées, bonnes pratiques
- **[DEVELOPMENT.md](docs/DEVELOPMENT.md)** - Guide du développeur, contribution, évolutions futures

### 🚀 Quick Links

- **Architecture** : Voir [structure des packages](docs/ARCHITECTURE.md#structure-des-packages)
- **Tests** : Lancer avec `mvn test` - Voir [guide des tests](docs/TESTING.md)
- **Contribution** : Voir [guide de contribution](docs/DEVELOPMENT.md#contribution)

---

## Tests

```bash
# Lancer tous les tests (60+)
mvn test

# Tests de sécurité uniquement
mvn test -Dtest=io.neatify.core.security.*Test

# Test spécifique
mvn test -Dtest=FileMoverTest
```

📖 **Voir [docs/TESTING.md](docs/TESTING.md) pour le guide complet**

---

## Développement

```bash
# Exécuter en mode développement
mvn exec:java

# Construire et tester
mvn clean package

# Avec rapport de couverture
mvn test jacoco:report
```

📖 **Voir [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) pour le guide du développeur**

---

## Licence

Ce projet est sous licence MIT - voir le fichier [LICENSE](LICENSE) pour plus de détails.

---

## Contribution

Les contributions, problèmes et demandes de fonctionnalités sont les bienvenus.

**Comment contribuer :**
1. Fork le projet
2. Créer une branche (`git checkout -b feature/ma-feature`)
3. Commit les changements (`git commit -m 'feat: ajout feature X'`)
4. Push vers la branche (`git push origin feature/ma-feature`)
5. Ouvrir une Pull Request

📖 **Voir [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md#contribution) pour les détails**

---

## Documentation en anglais

- [English documentation](README.en.md)
