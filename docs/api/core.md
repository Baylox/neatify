# API — `io.neatify.core` package

This package contains all the business logic. It has no dependency on any other application layer.

---

## `FileMover`

Public facade of the file movement system. Hides the `FilePlanner` and `FileExecutor` classes (package-private).

### Nested records

```java
record Action(Path source, Path target, String reason)
```
Represents a planned move. Immutable.

```java
record Result(int moved, int skipped, List<String> errors)
```
Execution result. `moved` = files moved (or planned in dry-run). `skipped` = intentionally ignored files (skip collision, already in place). `errors` = I/O error messages.

### Enum `CollisionStrategy`

| Value | Behavior when destination exists |
|---|---|
| `RENAME` | Appends `_1`, `_2`… up to 1000 attempts |
| `SKIP` | Returns `null` (file ignored, no error) |
| `OVERWRITE` | Replaces (atomic when possible) |

### Interface `MoveListener`

```java
@FunctionalInterface
interface MoveListener {
    void onMoved(Path source, Path finalTarget);
}
```

Callback invoked after each successful move (non-dry-run only).

### Static methods

```java
// Planning — basic with default rules
public static List<Action> plan(Path sourceRoot, Map<String, String> rules)
    throws IOException

// Planning — with quota and glob filters
public static List<Action> plan(Path sourceRoot, Map<String, String> rules,
    int maxFiles, List<String> includes, List<String> excludes)
    throws IOException

// Planning — with VCS repo skip option
public static List<Action> plan(Path sourceRoot, Map<String, String> rules,
    int maxFiles, List<String> includes, List<String> excludes,
    boolean skipGitRepos)
    throws IOException

// Execution — default RENAME strategy
public static Result execute(List<Action> actions, boolean dryRun)

// Execution — configurable strategy
public static Result execute(List<Action> actions, boolean dryRun,
    CollisionStrategy strategy)

// Execution — with listener (for undo journaling)
public static Result execute(List<Action> actions, boolean dryRun,
    CollisionStrategy strategy, MoveListener listener)
```

**Exceptions:**
- `IllegalArgumentException` — `sourceRoot` is not a directory, `maxFiles <= 0`
- `IllegalStateException` — `maxFiles` quota exceeded
- `IOException` — I/O error during tree traversal

---

## `Rules`

Loading and validation of `.properties` rules files.

### Static methods

```java
// Returns the built-in default rules (immutable map, ~67 entries)
public static Map<String, String> getDefaults()

// Loads a .properties file and returns an immutable map
// extension (lowercase, no dot) → destination folder
public static Map<String, String> load(Path propertiesFile)
    throws IOException

// Returns the target folder for an extension, or null if not found
public static String getTargetFolder(Map<String, String> rules, String extension)
```

**Validation in `load()`:**
- File must exist and be a regular file
- Empty extensions or empty folders are ignored
- Path traversal (`..`) and absolute paths throw `IllegalArgumentException`
- Illegal characters in folder names are replaced by `_`

**Exceptions:**
- `IllegalArgumentException` — invalid file, path traversal detected
- `IOException` — unreadable file

---

## `FileMetadata`

Immutable record holding file metadata.

```java
public record FileMetadata(
    Path path,
    String extension,       // no dot, lowercase — e.g. "pdf"
    long sizeInBytes,
    LocalDateTime lastModified
)
```

### Static methods

```java
// Creates a FileMetadata from a path — reads filesystem attributes
public static FileMetadata from(Path filePath) throws IOException
```

**Exceptions:**
- `IllegalArgumentException` — path does not point to a regular file
- `IOException` — error reading attributes

### Instance methods

```java
public String fileName()          // File name (without path)
public boolean hasNoExtension()   // true if extension is empty
public String formattedSize()     // "1.23 KB", "4.56 MB", "512 B" (Locale.ROOT)
```

### Static utility method

```java
// Extracts the extension from a file name (lowercase, no dot)
// Returns "" if no extension or trailing dot
public static String extensionOf(String fileName)
```

---

## `PathSecurity`

Path validation and security. All methods are static.

### Methods

```java
// Validates that the source directory is not a system directory
// Checks for symlinks in ancestry
// Throws SecurityException on violation
public static void validateSourceDir(Path sourcePath)

// Validates that a relative subpath contains no traversal or absolute component
// Throws IllegalArgumentException on violation
public static void validateRelativeSubpath(String subpath)

// Resolves subpath relative to root, guaranteeing the result stays within root
// Throws SecurityException if the resolved path escapes
public static Path safeResolveWithin(Path root, String subpath)

// Verifies that the path and none of its parents is a symbolic link
// Throws SecurityException if a symlink is found
public static void assertNoSymlinkInAncestry(Path path)
```

**System directories blocked by `validateSourceDir()`:**

Unix: `/etc`, `/bin`, `/sbin`, `/usr/bin`, `/usr/sbin`, `/var`, `/sys`, `/proc`, `/dev`, `/boot`, `/root`

Windows: `C:\Windows`, `C:\Program Files`, `C:\Program Files (x86)`, `C:\ProgramData`, `C:\Users\All Users`

---

## `DefaultRules` *(package-private)*

Defines the 67 built-in default rules. Not instantiable.

```java
static Map<String, String> create()
// Returns the extension → folder mapping
```

Called only by `Rules.getDefaults()`.

---

## `FilePlanner` *(package-private)*

Traverses the file tree and produces the `FileMover.Action` list. Not instantiable.

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

**Behavior:**
- Skips `.neatify/` (undo journals)
- Skips hidden files (name starting with `.`)
- Skips extension-less files
- If `skipGitRepos=true`, skips subdirectories containing a VCS marker (`.git`, `.hg`, `.svn`, `.bzr`, `_darcs`, `.pijul`, `.fslckout`, `.repo`)
- Throws `IllegalStateException` if `maxFiles` is exceeded

---

## `FileExecutor` *(package-private)*

Executes planned actions. Not instantiable.

```java
static FileMover.Result execute(
    List<FileMover.Action> actions,
    boolean dryRun,
    FileMover.CollisionStrategy strategy,
    FileMover.MoveListener listener   // may be null
)
```

**Dry-run behavior:** logs `[DRY-RUN]`, increments `moved` without moving anything.

**Real behavior:**
1. `Files.createDirectories(target.getParent())` if parent is non-null
2. `strategy.move(source, target)` for the actual move
3. If successful and listener is non-null: `listener.onMoved(source, finalTarget)`
4. On `IOException`: message added to `errors`, `skipped` not incremented
