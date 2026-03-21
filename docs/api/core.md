# API — Package `io.neatify.core`

Ce package contient toute la logique métier. Il ne dépend d'aucune autre couche de l'application.

---

## `FileMover`

Façade publique du système de déplacement de fichiers. Cache les classes `FilePlanner` et `FileExecutor` (package-private).

### Records imbriqués

```java
record Action(Path source, Path target, String reason)
```
Représente un déplacement planifié. Immuable.

```java
record Result(int moved, int skipped, List<String> errors)
```
Résultat d'une exécution. `moved` = fichiers déplacés (ou planifiés en dry-run). `skipped` = fichiers ignorés volontairement (collision skip, déjà en place). `errors` = messages d'erreur I/O.

### Enum `CollisionStrategy`

| Valeur | Comportement si la destination existe |
|---|---|
| `RENAME` | Ajoute `_1`, `_2`… jusqu'à 1000 tentatives |
| `SKIP` | Retourne `null` (fichier ignoré, pas d'erreur) |
| `OVERWRITE` | Remplace (atomique si possible) |

### Interface `MoveListener`

```java
@FunctionalInterface
interface MoveListener {
    void onMoved(Path source, Path finalTarget);
}
```

Callback appelé après chaque déplacement réussi (uniquement en mode non-dry-run).

### Méthodes statiques

```java
// Planning — version simple avec règles par défaut
public static List<Action> plan(Path sourceRoot, Map<String, String> rules)
    throws IOException

// Planning — avec quota et filtres glob
public static List<Action> plan(Path sourceRoot, Map<String, String> rules,
    int maxFiles, List<String> includes, List<String> excludes)
    throws IOException

// Planning — avec option skip VCS repos
public static List<Action> plan(Path sourceRoot, Map<String, String> rules,
    int maxFiles, List<String> includes, List<String> excludes,
    boolean skipGitRepos)
    throws IOException

// Exécution — stratégie RENAME par défaut
public static Result execute(List<Action> actions, boolean dryRun)

// Exécution — stratégie configurable
public static Result execute(List<Action> actions, boolean dryRun,
    CollisionStrategy strategy)

// Exécution — avec listener (pour journalisation undo)
public static Result execute(List<Action> actions, boolean dryRun,
    CollisionStrategy strategy, MoveListener listener)
```

**Exceptions :**
- `IllegalArgumentException` — `sourceRoot` n'est pas un répertoire, `maxFiles <= 0`
- `IllegalStateException` — quota `maxFiles` dépassé
- `IOException` — erreur I/O lors du parcours de l'arborescence

---

## `Rules`

Chargement et validation des fichiers de règles `.properties`.

### Méthodes statiques

```java
// Retourne les règles par défaut intégrées (immutable map, ~67 entrées)
public static Map<String, String> getDefaults()

// Charge un fichier .properties et retourne une map immutable
// extension (lowercase, sans dot) → dossier destination
public static Map<String, String> load(Path propertiesFile)
    throws IOException

// Retourne le dossier cible pour une extension, ou null si non trouvée
public static String getTargetFolder(Map<String, String> rules, String extension)
```

**Validation dans `load()` :**
- Le fichier doit exister et être un fichier régulier
- Les extensions vides ou les dossiers vides sont ignorés
- Les path traversal (`..`) et chemins absolus lèvent `IllegalArgumentException`
- Les caractères illégaux dans les noms de dossiers sont remplacés par `_`

**Exceptions :**
- `IllegalArgumentException` — fichier invalide, path traversal détecté
- `IOException` — fichier illisible

---

## `FileMetadata`

Record immuable contenant les métadonnées d'un fichier.

```java
public record FileMetadata(
    Path path,
    String extension,       // sans dot, lowercase — ex: "pdf"
    long sizeInBytes,
    LocalDateTime lastModified
)
```

### Méthodes statiques

```java
// Crée un FileMetadata depuis un chemin — lit les attributs du système de fichiers
public static FileMetadata from(Path filePath) throws IOException
```

**Exceptions :**
- `IllegalArgumentException` — le chemin ne pointe pas vers un fichier régulier
- `IOException` — erreur de lecture des attributs

### Méthodes d'instance

```java
public String fileName()          // Nom du fichier (sans le chemin)
public boolean hasNoExtension()   // true si extension est vide
public String formattedSize()     // "1.23 KB", "4.56 MB", "512 B" (Locale.ROOT)
```

### Méthode statique utilitaire

```java
// Extrait l'extension d'un nom de fichier (lowercase, sans dot)
// Retourne "" si pas d'extension ou dot en fin de nom
public static String extensionOf(String fileName)
```

---

## `PathSecurity`

Validation et sécurisation des chemins. Toutes les méthodes sont statiques.

### Méthodes

```java
// Valide que le dossier source n'est pas un répertoire système
// Vérifie absence de symlinks dans l'ancestry
// Lève SecurityException si violation
public static void validateSourceDir(Path sourcePath)

// Valide qu'un sous-chemin relatif ne contient pas de traversal ni d'absolu
// Lève IllegalArgumentException si violation
public static void validateRelativeSubpath(String subpath)

// Résout subpath relativement à root en garantissant que le résultat reste dans root
// Lève SecurityException si le chemin résolu s'échappe
public static Path safeResolveWithin(Path root, String subpath)

// Vérifie que le chemin et aucun de ses parents n'est un lien symbolique
// Lève SecurityException si symlink trouvé
public static void assertNoSymlinkInAncestry(Path path)
```

**Répertoires système bloqués par `validateSourceDir()` :**

Unix : `/etc`, `/bin`, `/sbin`, `/usr/bin`, `/usr/sbin`, `/var`, `/sys`, `/proc`, `/dev`, `/boot`, `/root`

Windows : `C:\Windows`, `C:\Program Files`, `C:\Program Files (x86)`, `C:\ProgramData`, `C:\Users\All Users`

---

## `DefaultRules` *(package-private)*

Définit les 67 règles par défaut intégrées. Non instanciable.

```java
static Map<String, String> create()
// Retourne la map d'associations extension → dossier
```

Appelé uniquement par `Rules.getDefaults()`.

---

## `FilePlanner` *(package-private)*

Parcourt l'arborescence de fichiers et produit la liste des `FileMover.Action`. Non instanciable.

```java
static List<FileMover.Action> plan(
    Path sourceRoot,
    Map<String, String> rules,
    int maxFiles,
    List<String> includes,
    List<String> excludes,
    boolean skipGitRepos
) throws IOException
```

**Comportement :**
- Ignore `.neatify/` (journaux d'undo)
- Ignore les fichiers cachés (nom commençant par `.`)
- Ignore les fichiers sans extension
- Si `skipGitRepos=true`, ignore les sous-dossiers contenant un marker VCS (`.git`, `.hg`, `.svn`, `.bzr`, `_darcs`, `.pijul`, `.fslckout`, `.repo`)
- Lève `IllegalStateException` si `maxFiles` est dépassé

---

## `FileExecutor` *(package-private)*

Exécute les actions planifiées. Non instanciable.

```java
static FileMover.Result execute(
    List<FileMover.Action> actions,
    boolean dryRun,
    FileMover.CollisionStrategy strategy,
    FileMover.MoveListener listener   // peut être null
)
```

**Comportement en dry-run :** log `[DRY-RUN]`, incrémente `moved` sans déplacer.

**Comportement réel :**
1. `Files.createDirectories(target.getParent())` si parent non null
2. `strategy.move(source, target)` pour le déplacement effectif
3. Si succès et listener non null : `listener.onMoved(source, finalTarget)`
4. Si `IOException` : message ajouté à `errors`, `skipped` non incrémenté
