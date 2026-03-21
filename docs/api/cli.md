# API — `io.neatify.cli` package

This package handles the user interface, argument parsing, and operation orchestration.

---

## `AppInfo`

Immutable record carrying application metadata.

```java
public record AppInfo(String name, String version, String description)
```

**Constant:**
```java
public static final String NEATIFY_VERSION = "1.0.0";
```

**Factory methods:**
```java
public static AppInfo neatify()                   // Creates AppInfo("NEATIFY", "1.0.0", "Automatic organization tool")
public static AppInfo neatify(String version)     // Creates AppInfo with custom version
```

---

## `FileOrganizationExecutor`

Orchestrator of the full CLI flow. Single entry point for all operations in non-interactive mode.

```java
public void execute(CLIConfig config) throws IOException
```

Internal flow:
1. Validates paths (`PathSecurity.validateSourceDir`)
2. Enforces Git policy (blocks `--apply` inside a repo unless `--allow-inside-git`)
3. Loads rules (`Rules.load()` or `Rules.getDefaults()`)
4. Plans actions (`FileMover.plan()`)
5. Displays preview or emits JSON
6. Executes (dry-run or real) and journals the run for undo

**Exceptions:**
- `IllegalArgumentException` — invalid path, missing rules file, inside git without flag
- `IOException` — I/O error during scan or execution

---

## `args.ArgumentParser`

Parses command-line arguments into an immutable `CLIConfig`.

```java
public CLIConfig parse(String[] arguments)
```

**Exceptions:**
- `IllegalArgumentException` — unknown argument, missing value for an option, mutually exclusive arguments combined

---

## `args.CLIConfig`

Immutable configuration produced by `ArgumentParser`. Contains all parameters for the current execution.

### Key getters

```java
// Paths
Path getSourceDir()
Path getRulesFile()
boolean isUseDefaultRules()

// Execution mode
boolean isApply()
boolean isInteractive()
boolean isShowHelp()
boolean isShowVersion()
boolean isJson()

// Undo
boolean isUndo()
boolean isUndoList()
long getUndoRun()          // timestamp, 0 if not specified

// Execution
FileMover.CollisionStrategy getOnCollision()
int getMaxFiles()
List<String> getIncludes()
List<String> getExcludes()

// Display
int getPerFolderPreview()
Preview.SortMode getSortMode()
boolean isNoColor()
boolean isAscii()

// Logging
boolean isDebug()
boolean isVerbose()
boolean isQuiet()

// Security
boolean isAllowInsideGit()
```

---

## `core.UndoExecutor`

Handles run journaling and undo operations.

### Nested records

```java
record Move(Path from, Path to)
```
A file move: `from` = original path, `to` = path after move.

```java
record UndoResult(int restored, int skipped, List<String> errors)
```
Result of an undo operation.

```java
record RunMeta(long time, String onCollision, int movesCount, Path file)
```
Metadata of a journaled run (used by `--undo-list`).

### Static methods

```java
// Journals a run to .neatify/runs/<timestamp>.json
// Returns the created file path, or null if moves is empty
public static Path appendRun(Path sourceRoot, String onCollision, List<Move> moves)
    throws IOException

// Undoes the most recent run (checks v2 first, then falls back to legacy manifest.json)
public static UndoResult undoLast(Path sourceRoot) throws IOException

// Undoes the most recent run (v2 format only)
// Returns null if no v2 run exists
public static UndoResult undoLastV2(Path sourceRoot) throws IOException

// Lists all journaled runs, most recent first
public static List<RunMeta> listRuns(Path sourceRoot) throws IOException

// Undoes the run identified by its timestamp
// Returns null if the journal file does not exist
public static UndoResult undoRun(Path sourceRoot, long timestamp) throws IOException
```

---

## `ui.Preview`

Generates and displays a formatted preview of planned changes.

### Enum `SortMode`

```java
enum SortMode {
    ALPHA,  // Alphabetical by file name
    EXT,    // By extension then by name
    SIZE    // By size descending then by name
}
```

### Class `Config`

```java
public static class Config {
    // Defaults
    private int maxFilesPerFolder = 5;
    private SortMode sortMode = SortMode.ALPHA;
    private boolean showDuplicates = true;

    // Fluent builder
    public Config maxFilesPerFolder(int value)
    public Config sortMode(SortMode mode)
    public Config showDuplicates(boolean value)
}
```

### Static methods

```java
// Prints the preview to stdout
public static void print(List<FileMover.Action> actions, Config config)

// Returns the preview lines without printing (for tests)
public static List<String> render(List<FileMover.Action> actions, Config config)
```

**`render()` output format:**

```
(empty line)
══════════════════════════════════════════════════════
                   CHANGES PREVIEW
══════════════════════════════════════════════════════

→ Documents/  (3 files)
  • report.pdf
  • notes.txt
  • letter.docx

→ Images/  (5 files)
  • photo.jpg  ×2
  • screenshot.png
  • + 3 more...

[########################################] 100% (8/8)
(empty line)
```

`×N` indicates N distinct source files with the same name (detected duplicates).

---

## `ui.Display`

Console output utility. All methods are static.

```java
// Basic output
public static void print(String message)
public static void println(String message)
public static void printErr(String message)          // stderr

// Formatted messages
public static void printSuccess(String message)      // [OK] in green
public static void printInfo(String message)         // [i] in cyan
public static void printWarning(String message)      // [!] in yellow
public static void printError(String message)        // [!!] in red

// Separators
public static void printLine()                       // Prints a 63-character line
public static String line()                          // Returns the line string
public static String center(String text)             // Centers text on 63 characters

// Banner
public static void printBannerSafe()                 // Prints app banner (Unicode or ASCII)

// Interaction
public static String readInput(String prompt)
public static String readInput(String prompt, String defaultValue)
public static void waitForEnter()

// Result
public static void printResultTable(int moved, int skipped, int errors)
```

---

## `ui.InteractiveCLI`

Main menu for interactive mode.

```java
public InteractiveCLI(String version)
public void run()
```

`run()` displays the banner, then loops the menu until the user chooses to quit.

---

## `ui.HelpPrinter`

Prints full help text (equivalent to `--help`).

```java
public static void print()
```

---

## `util.Ansi`

ANSI color codes for the terminal. Auto-detected, can be disabled.

```java
public static void setEnabled(boolean enabled)
public static boolean isEnabled()

// Returns colored text if ANSI is enabled, plain text otherwise
public static String cyan(String text)
public static String yellow(String text)
public static String green(String text)
public static String red(String text)
public static String dim(String text)
```

**Auto-disable:** `NO_COLOR` environment variable set, or `TERM=dumb`.

---

## `util.AsciiSymbols`

Display symbols. Automatically switches between Unicode and ASCII.

```java
public static void setUseUnicode(boolean useUnicode)
public static boolean useUnicode()   // Override via NEATIFY_FORCE_UNICODE env var

// Symbols (return → or >, • or *, ×N or xN, + or +)
public static String arrow()
public static String bullet()
public static String times()
public static String plus()
```

**Auto-detection:** based on JVM `file.encoding` (UTF-8 → Unicode enabled).

---

## `util.ResultPrinter`

Displays an execution summary.

```java
public static void print(FileMover.Result result)
```

Output:
```
[OK] Moved:   9
[--] Skipped: 0
[!!] Errors:  0
```

Also lists detailed error messages if any are present.
