# Guide des Tests - Neatify

## Vue d'ensemble

Neatify dispose de **60+ tests unitaires** couvrant :
- ✓ Fonctionnalités principales (plan, execute, dry-run)
- ✓ Sécurité (path traversal, quota, collisions)
- ✓ Interface CLI (parsing, interactivité)
- ✓ Rendu et formatage (aperçu, couleurs)

**Couverture des tests :** Focus sur les scénarios critiques et les cas limites.

---

## Exécution des tests

### Commandes de base

```bash
# Lancer tous les tests
mvn test

# Lancer avec rapport de couverture
mvn test jacoco:report

# Lancer un test spécifique
mvn test -Dtest=FileMoverTest

# Lancer tous les tests d'une classe
mvn test -Dtest=FileMoverSecurityTest

# Lancer un test spécifique d'une classe
mvn test -Dtest=FileMoverTest#testPlan_BasicFunctionality
```

### Tests par catégorie

```bash
# Tests de sécurité uniquement
mvn test -Dtest=io.neatify.core.security.*Test

# Tests du core
mvn test -Dtest=io.neatify.core.*Test

# Tests CLI
mvn test -Dtest=io.neatify.cli.*Test
```

---

## Architecture des tests

```
src/test/java/io/neatify/
├── TestHelper.java                       # Classe de base avec helpers communs
├── cli/
│   ├── args/ArgumentParserTest.java      # Tests du parser d'arguments (6 tests)
│   └── PreviewRendererTest.java          # Tests du rendu d'aperçu (4 tests)
└── core/
    ├── FileMetadataTest.java             # Tests des métadonnées (4 tests)
    ├── FileMoverTest.java                # Tests du déplacement (7 tests)
    ├── RulesTest.java                    # Tests du chargement de règles (6 tests)
    └── security/                         # Package dédié aux tests de sécurité
        ├── FileMoverSecurityTestBase.java         # Base pour tests de sécurité
        ├── FileMoverPathTraversalTest.java        # Tests anti path traversal (6 tests)
        ├── FileMoverQuotaTest.java                # Tests anti-DOS (4 tests)
        ├── FileMoverCollisionTest.java            # Tests anti-TOCTOU (3 tests)
        ├── PathSecurityTest.java                  # Tests de validation (9 tests)
        └── RulesSecurityTest.java                 # Tests de sécurité des règles (11 tests)
```

---

## Classes de test

### TestHelper.java

Classe de base abstraite fournissant des helpers communs à tous les tests.

**Helpers de création de fichiers :**
```java
protected void createTestFile(Path tempDir, String filename)
protected void createTestFile(Path tempDir, String filename, String content)
protected void createMultipleFiles(Path tempDir, String prefix, String extension, int count)
```

**Helpers d'actions :**
```java
protected FileMover.Action createAction(Path source, Path target)
protected FileMover.Action createAction(Path source, Path target, String label)
```

**Usage :**
```java
class MyTest extends TestHelper {
    @Test
    void myTest(@TempDir Path tempDir) {
        createTestFile(tempDir, "test.txt", "content");
        // ...
    }
}
```

### FileMoverSecurityTestBase.java

Classe de base pour les tests de sécurité, étend `TestHelper`.

**Helpers supplémentaires :**
```java
// Assertions sur les actions
protected void assertActionExists(List<Action> actions, String filename)
protected void assertActionNotExists(List<Action> actions, String filename, String message)

// Tests de règles malveillantes
protected void assertMaliciousRuleBlockedForFile(Path tempDir, String filename,
                                                  String extension, String maliciousTarget)

// Setup de scénarios de collision
protected void setupCollisionScenario(Path tempDir, String baseFilename, String... existingContents)
```

---

## Tests par composant

### FileMoverTest.java (7 tests)

**Tests de planification :**
- `testPlan_BasicFunctionality` : Scan et identification des fichiers
- `testPlan_IgnoresHiddenFiles` : Fichiers cachés ignorés
- `testPlan_WithNestedFolders` : Scan récursif

**Tests d'exécution :**
- `testExecute_DryRun` : Mode simulation
- `testExecute_RealMove` : Déplacement réel
- `testExecute_CreatesTargetDirectory` : Création de dossiers
- `testExecute_MultipleFiles` : Déplacement de plusieurs fichiers

### RulesTest.java (6 tests)

- `testLoad_ValidRules` : Chargement de règles valides
- `testLoad_NormalizesExtensions` : Normalisation (minuscules, sans point)
- `testLoad_SupportsNestedFolders` : Support des sous-dossiers
- `testLoad_RejectsEmptyFile` : Rejet des fichiers vides
- `testGetTargetFolder_CaseInsensitive` : Recherche insensible à la casse
- `testSanitizeFolderName_ReplacesInvalidChars` : Remplacement des caractères invalides

### PreviewRendererTest.java (4 tests)

- `testRender_EmptyActions` : Liste vide
- `testRender_SingleFile` : Un seul fichier
- `testRender_MultipleFolders` : Plusieurs dossiers de destination
- `testRender_DuplicateCounting` : Comptage des duplicatas

### ArgumentParserTest.java (6 tests)

- `testParse_ValidArguments` : Arguments valides
- `testParse_MissingSource` : Source manquante
- `testParse_MissingRules` : Règles manquantes
- `testParse_WithApplyFlag` : Flag --apply
- `testParse_HelpFlag` : Flag --help
- `testParse_VersionFlag` : Flag --version

---

## Tests de sécurité

### FileMoverPathTraversalTest.java (6 tests)

**Protection contre path traversal :**
- `testPathTraversal_SecondLevelProtection` : Blocage de `../../../etc`
- `testResolvedPath_StaysInSourceRoot` : Chemins restent dans la racine
- `testValidNestedPath_Works` : Chemins valides autorisés
- `testAbsolutePath_Blocked` : Chemins absolus bloqués
- `testMixedRules_OnlyValidProcessed` : Seules les règles valides traitées
- `testPathNormalization` : Normalisation des chemins

### FileMoverQuotaTest.java (4 tests)

**Protection anti-DOS :**
- `testQuota_UnderLimit` : Sous la limite
- `testQuota_ExceedsLimit` : Au-dessus de la limite
- `testQuota_DefaultQuota` : Quota par défaut (100k)
- `testQuota_InvalidQuota` : Quota négatif rejeté

### FileMoverCollisionTest.java (3 tests)

**Protection anti-TOCTOU :**
- `testAtomicMove_NoCollision` : Pas de collision
- `testAtomicMove_WithCollision` : Gestion d'une collision (`_1`)
- `testAtomicMove_MultipleCollisions` : Collisions multiples (`_1`, `_2`, `_3`)

### PathSecurityTest.java (9 tests)

**Validation des chemins :**
- `testValidateRelativeSubpath_Valid` : Chemins relatifs valides
- `testValidateRelativeSubpath_RejectsPathTraversal` : Blocage `../`
- `testValidateRelativeSubpath_RejectsAbsoluteUnix` : Blocage `/etc`
- `testValidateRelativeSubpath_RejectsAbsoluteWindows` : Blocage `C:\`
- `testSafeResolveWithin_Valid` : Résolution sécurisée
- `testSafeResolveWithin_RejectsEscape` : Rejet des échappements
- `testAssertNoSymlinkInAncestry_ValidPath` : Pas de symlink
- `testValidateSourceDir_ValidDir` : Dossier valide
- `testValidateSourceDir_RejectsSystemDirs` : Dossiers système rejetés

### RulesSecurityTest.java (11 tests)

**Validation des règles :**
- Tests de path traversal (4 tests)
- Tests de chemins absolus (2 tests)
- Tests de caractères spéciaux (2 tests)
- Tests de règles mixtes (3 tests)

---

## Conventions de test

### Nomenclature

```java
testMethodName_Scenario()
testMethodName_ExpectedBehavior()
```

**Exemples :**
- `testPlan_BasicFunctionality`
- `testExecute_DryRun`
- `testLoad_RejectsEmptyFile`

### Structure AAA (Arrange-Act-Assert)

```java
@Test
void testExample() {
    // Arrange : Setup
    createTestFile(tempDir, "test.txt");
    Map<String, String> rules = Map.of("txt", "Documents");

    // Act : Exécution
    List<Action> actions = FileMover.plan(tempDir, rules);

    // Assert : Vérification
    assertEquals(1, actions.size());
    assertEquals("test.txt", actions.get(0).source().getFileName().toString());
}
```

### Utilisation de @TempDir

Tous les tests utilisant le système de fichiers utilisent `@TempDir` :

```java
@Test
void myTest(@TempDir Path tempDir) throws IOException {
    // tempDir est automatiquement créé et nettoyé
    createTestFile(tempDir, "test.txt");
    // ...
}
```

**Avantages :**
- Isolation des tests
- Nettoyage automatique
- Pas d'effets de bord

---

## Bonnes pratiques

### 1. Tests indépendants

Chaque test doit être exécutable seul et dans n'importe quel ordre.

### 2. Pas de dépendances entre tests

❌ **Mauvais :**
```java
private static Path sharedFile; // État partagé

@Test
void test1() {
    sharedFile = createFile();
}

@Test
void test2() {
    // Dépend de test1
    useFile(sharedFile);
}
```

✅ **Bon :**
```java
@Test
void test1(@TempDir Path tempDir) {
    Path file = createFile(tempDir);
    // Test complet
}

@Test
void test2(@TempDir Path tempDir) {
    Path file = createFile(tempDir);
    // Test complet indépendant
}
```

### 3. Utiliser des helpers

❌ **Mauvais :**
```java
@Test
void test() {
    Path file = tempDir.resolve("test.txt");
    Files.writeString(file, "content");
    // Duplication dans chaque test
}
```

✅ **Bon :**
```java
@Test
void test() {
    createTestFile(tempDir, "test.txt", "content");
    // Utilise le helper
}
```

### 4. Messages d'assertion clairs

❌ **Mauvais :**
```java
assertTrue(actions.size() > 0);
```

✅ **Bon :**
```java
assertTrue(actions.size() > 0, "Actions list should not be empty");
assertEquals(1, actions.size(), "Should have exactly 1 action for the single file");
```

### 5. Tester les cas limites

- Fichiers vides
- Caractères spéciaux dans les noms
- Très grands nombres de fichiers
- Permissions insuffisantes
- Entrées malveillantes (sécurité)

---

## Tests de sécurité : Approche

### Principe : Test d'attaque

Les tests de sécurité simulent des attaques réelles :

```java
@Test
void testPathTraversal_SecondLevelProtection() {
    // Simuler une règle malveillante
    Map<String, String> maliciousRules = Map.of(
        "jpg", "ValidFolder/../../../etc"  // Tentative d'échappement
    );

    // Vérifier que c'est bloqué
    List<Action> actions = FileMover.plan(tempDir, maliciousRules);
    assertEquals(0, actions.size(), "Malicious rules should generate no actions");
}
```

### Scénarios testés

1. **Path Traversal :** `../`, `..\\`, chemins absolus
2. **DOS :** Trop de fichiers
3. **TOCTOU :** Collisions détectées après planification
4. **Injection :** Caractères spéciaux dans les noms
5. **Validation :** Entrées invalides, fichiers vides

---

## Ajouter de nouveaux tests

### 1. Déterminer la catégorie

- **Fonctionnel** → `core/` ou `cli/`
- **Sécurité** → `core/security/`

### 2. Choisir la classe de base

- Hériter de `TestHelper` pour les helpers de base
- Hériter de `FileMoverSecurityTestBase` pour les tests de sécurité

### 3. Exemple de nouveau test

```java
package io.neatify.core;

import io.neatify.TestHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MyNewTest extends TestHelper {

    @Test
    void testMyNewFeature(@TempDir Path tempDir) throws Exception {
        // Arrange
        createTestFile(tempDir, "test.txt");

        // Act
        // ... votre logique

        // Assert
        // ... vos assertions
    }
}
```

---

## Déboguer les tests

### Afficher les chemins temporaires

```java
@Test
void test(@TempDir Path tempDir) {
    System.out.println("Temp dir: " + tempDir);
    // Les fichiers restent accessibles pendant le débogage
}
```

### Exécuter avec sortie verbose

```bash
mvn test -X -Dtest=MyTest
```

### Déboguer un test spécifique

Dans IntelliJ IDEA :
1. Clic droit sur le test
2. "Debug 'testMethodName()'"

---

## Métriques de test

| Métrique | Valeur |
|----------|--------|
| Nombre total de tests | 60+ |
| Tests de sécurité | 33 |
| Tests fonctionnels | 27 |
| Couverture estimée | ~85% (lignes critiques) |

---

## Références

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [AssertJ (si ajouté)](https://assertj.github.io/doc/)
- [ARCHITECTURE.md](ARCHITECTURE.md) - Comprendre le code testé
- [SECURITY.md](SECURITY.md) - Comprendre les menaces testées
