# Guide du développeur - Neatify

## Configuration de l'environnement

### Prérequis

- **Java 21+** (OpenJDK ou Oracle JDK)
- **Maven 3.8+** (ou utiliser le Maven Wrapper inclus)
- **Git**
- **IDE recommandé** : IntelliJ IDEA, Eclipse, ou VS Code avec Extension Pack for Java

### Cloner et construire

```bash
# Cloner le dépôt
git clone <url-du-depot>
cd neatify

# Construire le projet
mvn clean package

# Ou avec le Maven Wrapper (Windows)
.\mvnw.cmd clean package

# Ou avec le Maven Wrapper (Unix/Linux/Mac)
./mvnw clean package

# Le JAR sera créé dans target/neatify.jar
```

### Structure du projet

```
neatify/
├── pom.xml                       # Configuration Maven
├── mvnw, mvnw.cmd               # Maven Wrapper
├── rules.properties              # Règles d'exemple
├── README.md                     # Documentation principale
├── LICENSE                       # Licence MIT
├── docs/                         # Documentation détaillée
│   ├── ARCHITECTURE.md          # Architecture du projet
│   ├── TESTING.md               # Guide des tests
│   ├── SECURITY.md              # Documentation de sécurité
│   └── DEVELOPMENT.md           # Ce fichier
└── src/
    ├── main/
    │   ├── java/io/neatify/
    │   │   ├── cli/             # Interface ligne de commande
    │   │   ├── core/            # Logique métier
    │   │   └── Neatify.java     # Point d'entrée
    │   └── resources/
    │       └── META-INF/MANIFEST.MF
    └── test/
        └── java/io/neatify/     # Tests unitaires
            ├── TestHelper.java
            ├── cli/
            └── core/
                └── security/
```

---

## Commandes Maven

### Build et packaging

```bash
# Compilation uniquement
mvn compile

# Compilation + tests
mvn test

# Packaging (crée le JAR)
mvn package

# Clean + package
mvn clean package

# Skip tests (pour build rapide)
mvn package -DskipTests
```

### Exécution

```bash
# Exécuter directement avec Maven
mvn exec:java

# Avec arguments
mvn exec:java -Dexec.args="--source ~/Downloads --rules rules.properties"

# Ou exécuter le JAR
java -jar target/neatify.jar
```

### Tests

```bash
# Lancer tous les tests
mvn test

# Test spécifique
mvn test -Dtest=FileMoverTest

# Tests d'une catégorie
mvn test -Dtest=io.neatify.core.security.*Test

# Avec rapport de couverture
mvn test jacoco:report
```

---

## Workflow de développement

### 1. Créer une branche

```bash
git checkout -b feature/ma-fonctionnalite
```

### 2. Développer

- Écrire le code
- Ajouter des tests
- Vérifier la couverture
- Documenter si nécessaire

### 3. Tester

```bash
# Tests unitaires
mvn test

# Vérifier le packaging
mvn package

# Tester le JAR
java -jar target/neatify.jar --help
```

### 4. Commit

```bash
git add .
git commit -m "feat: ajouter fonctionnalité X"
```

**Convention de commit** (style Conventional Commits) :
- `feat:` Nouvelle fonctionnalité
- `fix:` Correction de bug
- `refactor:` Refactoring sans changement de comportement
- `test:` Ajout/modification de tests
- `docs:` Documentation
- `chore:` Tâches de maintenance

### 5. Push et Pull Request

```bash
git push origin feature/ma-fonctionnalite
```

Puis créer une Pull Request sur GitHub/GitLab.

---

## Conventions de code

### Style Java

- **Indentation** : 4 espaces
- **Accolades** : Style K&R (accolade ouvrante sur la même ligne)
- **Nommage** :
  - Classes : `PascalCase`
  - Méthodes/variables : `camelCase`
  - Constantes : `UPPER_SNAKE_CASE`
  - Packages : `lowercase`

### Exemple

```java
package io.neatify.core;

public class MyClass {
    private static final int MAX_SIZE = 100;

    private String myField;

    public void myMethod(String parameter) {
        if (parameter == null) {
            throw new IllegalArgumentException("Parameter cannot be null");
        }

        // Implementation
    }
}
```

### Javadoc

Documenter les APIs publiques :

```java
/**
 * Déplace les fichiers selon les règles fournies.
 *
 * @param sourceDir répertoire source
 * @param rules     règles de rangement (extension → dossier)
 * @return liste des actions planifiées
 * @throws IOException si le scan échoue
 */
public static List<Action> plan(Path sourceDir, Map<String, String> rules)
        throws IOException {
    // ...
}
```

---

## Ajouter une nouvelle fonctionnalité

### Exemple : Ajouter le tri par date

#### 1. Définir l'API dans `core/`

```java
// Dans FileMover.java
public enum SortBy {
    EXTENSION, NAME, DATE, SIZE
}

public static List<Action> plan(Path sourceDir,
                                Map<String, String> rules,
                                SortBy sortBy) {
    // Implementation
}
```

#### 2. Ajouter la logique métier

```java
private static List<FileMetadata> sortFiles(List<FileMetadata> files,
                                            SortBy sortBy) {
    return switch (sortBy) {
        case NAME -> files.stream()
            .sorted(Comparator.comparing(FileMetadata::name))
            .toList();
        case DATE -> files.stream()
            .sorted(Comparator.comparing(f -> getLastModified(f.path())))
            .toList();
        // ...
    };
}
```

#### 3. Ajouter des tests

```java
// Dans FileMoverTest.java
@Test
void testPlan_SortByDate(@TempDir Path tempDir) throws IOException {
    // Arrange
    createTestFile(tempDir, "old.txt");
    Thread.sleep(100);
    createTestFile(tempDir, "new.txt");

    Map<String, String> rules = Map.of("txt", "Documents");

    // Act
    List<Action> actions = FileMover.plan(tempDir, rules, SortBy.DATE);

    // Assert
    assertEquals("old.txt", actions.get(0).source().getFileName().toString());
    assertEquals("new.txt", actions.get(1).source().getFileName().toString());
}
```

#### 4. Mettre à jour la CLI

```java
// Dans ArgumentParser.java
public CLIConfig parse(String[] args) {
    // ...
    String sortBy = extractArg(args, "--sort");
    // ...
}
```

#### 5. Documenter

Mettre à jour `README.md` et créer une section dans `ARCHITECTURE.md` si nécessaire.

---

## Debugging

### Logs de débogage

Ajouter des prints temporaires :

```java
System.err.println("[DEBUG] Variable value: " + myVar);
```

**Note :** Utiliser `System.err` pour les logs de debug (séparé de la sortie normale).

### Debugger dans IntelliJ IDEA

1. Placer un breakpoint (clic sur la marge gauche)
2. Clic droit sur la classe → "Debug 'Main'"
3. Le programme s'arrêtera au breakpoint

### Debugger avec Maven

```bash
# Démarrer en mode debug
mvnDebug exec:java

# Puis attacher le debugger de l'IDE sur le port 8000
```

---

## Tests

Voir [TESTING.md](TESTING.md) pour le guide complet des tests.

### Quick reference

```bash
# Tests unitaires
mvn test

# Test spécifique
mvn test -Dtest=FileMoverTest#testPlan_BasicFunctionality

# Tests de sécurité
mvn test -Dtest=io.neatify.core.security.*Test

# Avec couverture
mvn test jacoco:report
# Rapport dans target/site/jacoco/index.html
```

---

## Performance

### Profiling

Pour identifier les bottlenecks :

```bash
# Avec JProfiler, YourKit, ou VisualVM
java -agentpath:/path/to/profiler -jar target/neatify.jar --source /large/folder
```

### Benchmarking

```java
long start = System.nanoTime();
// Code à mesurer
long duration = System.nanoTime() - start;
System.out.println("Duration: " + duration / 1_000_000 + " ms");
```

### Optimisations actuelles

- `Files.walk()` avec streaming (pas de chargement en mémoire complet)
- Quota pour limiter le nombre de fichiers traités
- Déplacements atomiques quand disponibles
- Pas de duplication de données

---

## Évolutions futures

### Priorité haute

- [ ] **Interface `Rule`** pour des règles complexes
  - Règle par date : "photos de 2024 → Photos/2024"
  - Règle par taille : "fichiers > 100 MB → LargeFiles"
  - Règle par regex : "IMG_\\d{4} → Images"

- [ ] **Option `--by-date`** pour organiser par année/mois
  ```bash
  java -jar neatify.jar --source ~/Photos --by-date year/month
  # Crée Photos/2024/01/, Photos/2024/02/, etc.
  ```

- [ ] **Option `--report`** pour générer un rapport JSON
  ```json
  {
    "timestamp": "2024-01-15T10:30:00Z",
    "moved": 150,
    "failed": 2,
    "actions": [...]
  }
  ```

### Priorité moyenne

- [ ] **Support des tags** : `#important`, `#urgent`
  ```properties
  pdf|important=Documents/Important
  txt|urgent=Documents/Urgent
  ```

- [ ] **Undo** : Garder une trace des déplacements
  ```bash
  java -jar neatify.jar --undo last
  ```

- [ ] **Watch mode** : Surveillance continue
  ```bash
  java -jar neatify.jar --watch ~/Downloads --rules rules.properties
  # Détecte et range automatiquement les nouveaux fichiers
  ```

- [ ] **Plugins** : Architecture modulaire
  ```
  ~/.neatify/plugins/
  ├── my-custom-rule.jar
  └── cloud-sync.jar
  ```

### Priorité basse

- [ ] **Interface graphique** : JavaFX ou Swing
- [ ] **Configuration globale** : `~/.neatify/config.yaml`
- [ ] **Historique** : Base de données des opérations
- [ ] **Intégration shell** : Complétion bash/zsh

---

## Architecture pour les extensions

### Pattern : Strategy pour les règles

```java
public interface Rule {
    boolean matches(FileMetadata file);
    String getTargetFolder(FileMetadata file);
}

public class ExtensionRule implements Rule {
    private final Map<String, String> mappings;

    public boolean matches(FileMetadata file) {
        return mappings.containsKey(file.extension());
    }

    public String getTargetFolder(FileMetadata file) {
        return mappings.get(file.extension());
    }
}

public class DateRule implements Rule {
    public boolean matches(FileMetadata file) {
        return true; // Matches all files
    }

    public String getTargetFolder(FileMetadata file) {
        LocalDate date = getCreationDate(file.path());
        return String.format("%d/%02d", date.getYear(), date.getMonthValue());
    }
}
```

### Pattern : Chain of Responsibility

```java
List<Rule> rules = List.of(
    new TagRule(),        // Vérifie d'abord les tags
    new DateRule(),       // Puis la date
    new ExtensionRule()   // Enfin l'extension (fallback)
);

for (Rule rule : rules) {
    if (rule.matches(file)) {
        String target = rule.getTargetFolder(file);
        break;
    }
}
```

---

## Dépendances

### Production
**Aucune dépendance externe.** Utilise uniquement la Java Standard Library (JDK 21).

**Pourquoi ?**
- ✅ Simplicité : Pas de gestion de dépendances complexes
- ✅ Sécurité : Pas de vulnérabilités tierces
- ✅ Performance : Pas de overhead de frameworks lourds
- ✅ Portabilité : Fonctionne partout où Java 21 est installé

### Tests
- **JUnit 5** (jupiter) : Framework de tests
- **JUnit Platform** : Exécution des tests

### Ajouter une dépendance (si vraiment nécessaire)

```xml
<!-- Dans pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.example</groupId>
        <artifactId>my-library</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

**Critères pour accepter une dépendance :**
- Est-elle vraiment nécessaire ?
- Peut-on l'implémenter nous-mêmes simplement ?
- Est-elle maintenue activement ?
- A-t-elle des vulnérabilités connues ?
- Ajoute-t-elle beaucoup de poids au JAR ?

---

## Release

### Versioning

Neatify utilise [Semantic Versioning](https://semver.org/) :

```
MAJOR.MINOR.PATCH
  1   .  2  .  3
```

- **MAJOR** : Breaking changes
- **MINOR** : Nouvelles fonctionnalités (backward compatible)
- **PATCH** : Bug fixes

### Créer une release

```bash
# 1. Mettre à jour la version dans pom.xml
<version>1.2.0</version>

# 2. Mettre à jour AppInfo.java
public static final String VERSION = "1.2.0";

# 3. Créer un tag Git
git tag -a v1.2.0 -m "Release version 1.2.0"
git push origin v1.2.0

# 4. Builder le JAR de release
mvn clean package

# 5. Créer une release sur GitHub/GitLab avec le JAR
```

---

## Contribution

### Comment contribuer

1. **Fork** le projet
2. Créer une **branche** (`git checkout -b feature/ma-feature`)
3. **Commit** les changements (`git commit -m 'feat: ajout feature X'`)
4. **Push** vers la branche (`git push origin feature/ma-feature`)
5. Ouvrir une **Pull Request**

### Checklist avant PR

- [ ] Le code compile sans erreurs
- [ ] Tous les tests passent
- [ ] Des tests ont été ajoutés pour la nouvelle fonctionnalité
- [ ] Le code suit les conventions du projet
- [ ] La documentation est à jour (README, docs/)
- [ ] Les commits sont clairs et suivent Conventional Commits

### Code review

Toutes les PR sont reviewées avant merge. Points vérifiés :
- ✅ Qualité du code
- ✅ Tests adéquats
- ✅ Sécurité (pas de nouvelles vulnérabilités)
- ✅ Performance (pas de régression)
- ✅ Documentation

---

## Ressources

### Documentation officielle
- [Java 21 Documentation](https://docs.oracle.com/en/java/javase/21/)
- [Maven Documentation](https://maven.apache.org/guides/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)

### Documentation du projet
- [README.md](../README.md) - Vue d'ensemble
- [ARCHITECTURE.md](ARCHITECTURE.md) - Architecture détaillée
- [TESTING.md](TESTING.md) - Guide des tests
- [SECURITY.md](SECURITY.md) - Documentation de sécurité

### Outils recommandés
- **IDE** : IntelliJ IDEA Community (gratuit)
- **Git GUI** : GitKraken, SourceTree, ou GitHub Desktop
- **Profiler** : VisualVM (inclus avec JDK)
- **Code quality** : SonarLint (plugin IDE)

---

## Contact

Pour toute question sur le développement :
- Ouvrir une issue sur GitHub/GitLab
- Contacter les mainteneurs

---

## Licence

Ce projet est sous licence MIT. Voir [LICENSE](../LICENSE) pour plus de détails.
