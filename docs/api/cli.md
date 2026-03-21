# API — Package `io.neatify.cli`

Ce package gère l'interface utilisateur, le parsing des arguments et l'orchestration des opérations.

---

## `AppInfo`

Record immuable portant les métadonnées de l'application.

```java
public record AppInfo(String name, String version, String description)
```

**Constante :**
```java
public static final String NEATIFY_VERSION = "1.0.0";
```

**Méthodes factory :**
```java
public static AppInfo neatify()                   // Crée AppInfo("NEATIFY", "1.0.0", "Automatic organization tool")
public static AppInfo neatify(String version)     // Crée AppInfo avec version custom
```

---

## `FileOrganizationExecutor`

Orchestrateur du flux CLI complet. Seul point d'entrée pour toutes les opérations en mode non-interactif.

```java
public void execute(CLIConfig config) throws IOException
```

Flux interne :
1. Valide les chemins (`PathSecurity.validateSourceDir`)
2. Applique la politique Git (bloque `--apply` dans un repo sauf `--allow-inside-git`)
3. Charge les règles (`Rules.load()` ou `Rules.getDefaults()`)
4. Planifie les actions (`FileMover.plan()`)
5. Affiche l'aperçu ou émet le JSON
6. Exécute (dry-run ou réel) et journalise le run pour undo

**Exceptions :**
- `IllegalArgumentException` — chemin invalide, règles introuvables, inside git sans flag
- `IOException` — erreur I/O lors du scan ou de l'exécution

---

## `args.ArgumentParser`

Parse les arguments de la ligne de commande en une `CLIConfig` immuable.

```java
public CLIConfig parse(String[] arguments)
```

**Exceptions :**
- `IllegalArgumentException` — argument inconnu, valeur manquante pour une option, arguments mutuellement exclusifs combinés

---

## `args.CLIConfig`

Configuration immuable produite par `ArgumentParser`. Contient tous les paramètres de l'exécution courante.

### Getters principaux

```java
// Chemins
Path getSourceDir()
Path getRulesFile()
boolean isUseDefaultRules()

// Mode d'exécution
boolean isApply()
boolean isInteractive()
boolean isShowHelp()
boolean isShowVersion()
boolean isJson()

// Undo
boolean isUndo()
boolean isUndoList()
long getUndoRun()          // timestamp, 0 si non spécifié

// Exécution
FileMover.CollisionStrategy getOnCollision()
int getMaxFiles()
List<String> getIncludes()
List<String> getExcludes()

// Affichage
int getPerFolderPreview()
Preview.SortMode getSortMode()
boolean isNoColor()
boolean isAscii()

// Logs
boolean isDebug()
boolean isVerbose()
boolean isQuiet()

// Sécurité
boolean isAllowInsideGit()
```

---

## `core.UndoExecutor`

Gère la journalisation des opérations et leur annulation.

### Records imbriqués

```java
record Move(Path from, Path to)
```
Un mouvement de fichier : `from` = chemin original, `to` = chemin après déplacement.

```java
record UndoResult(int restored, int skipped, List<String> errors)
```
Résultat d'une annulation.

```java
record RunMeta(long time, String onCollision, int movesCount, Path file)
```
Métadonnées d'un run journalisé (pour `--undo-list`).

### Méthodes statiques

```java
// Journalise un run dans .neatify/runs/<timestamp>.json
// Retourne le chemin du fichier créé, ou null si moves est vide
public static Path appendRun(Path sourceRoot, String onCollision, List<Move> moves)
    throws IOException

// Annule le run le plus récent (cherche d'abord v2, puis legacy manifest.json)
public static UndoResult undoLast(Path sourceRoot) throws IOException

// Annule le run le plus récent (format v2 uniquement)
// Retourne null si aucun run v2 n'existe
public static UndoResult undoLastV2(Path sourceRoot) throws IOException

// Liste tous les runs journalisés, du plus récent au plus ancien
public static List<RunMeta> listRuns(Path sourceRoot) throws IOException

// Annule le run identifié par son timestamp
// Retourne null si le fichier journal n'existe pas
public static UndoResult undoRun(Path sourceRoot, long timestamp) throws IOException
```

---

## `ui.Preview`

Génère et affiche un aperçu formaté des changements planifiés.

### Enum `SortMode`

```java
enum SortMode {
    ALPHA,  // Alphabétique par nom de fichier
    EXT,    // Par extension puis par nom
    SIZE    // Par taille décroissante puis par nom
}
```

### Classe `Config`

```java
public static class Config {
    // Valeurs par défaut
    private int maxFilesPerFolder = 5;
    private SortMode sortMode = SortMode.ALPHA;
    private boolean showDuplicates = true;

    // Builder fluent
    public Config maxFilesPerFolder(int value)
    public Config sortMode(SortMode mode)
    public Config showDuplicates(boolean value)
}
```

### Méthodes statiques

```java
// Affiche l'aperçu sur stdout
public static void print(List<FileMover.Action> actions, Config config)

// Retourne les lignes de l'aperçu sans les afficher (pour tests)
public static List<String> render(List<FileMover.Action> actions, Config config)
```

**Format de sortie de `render()` :**

```
(ligne vide)
══════════════════════════════════════════════════════
                   CHANGES PREVIEW
══════════════════════════════════════════════════════

→ Documents/  (3 files)
  • rapport.pdf
  • notes.txt
  • lettre.docx

→ Images/  (5 files)
  • photo.jpg  ×2
  • screenshot.png
  • + 3 more...

[########################################] 100% (8/8)
(ligne vide)
```

`×N` indique N fichiers sources distincts avec le même nom (doublons détectés).

---

## `ui.Display`

Utilitaire de sortie console. Toutes les méthodes sont statiques.

```java
// Sortie basique
public static void print(String message)
public static void println(String message)
public static void printErr(String message)          // stderr

// Messages formatés
public static void printSuccess(String message)      // [OK] en vert
public static void printInfo(String message)         // [i] en cyan
public static void printWarning(String message)      // [!] en jaune
public static void printError(String message)        // [!!] en rouge

// Séparateurs
public static void printLine()                       // Affiche une ligne de 63 caractères
public static String line()                          // Retourne la ligne (pour composition)
public static String center(String text)             // Centre le texte sur 63 caractères

// Banneau
public static void printBannerSafe()                 // Affiche le banneau (Unicode ou ASCII)

// Interaction
public static String readInput(String prompt)
public static String readInput(String prompt, String defaultValue)
public static void waitForEnter()

// Résultat
public static void printResultTable(int moved, int skipped, int errors)
```

---

## `ui.InteractiveCLI`

Menu principal du mode interactif.

```java
public InteractiveCLI(String version)
public void run()
```

`run()` affiche le banneau, puis boucle sur le menu jusqu'à ce que l'utilisateur choisisse de quitter.

---

## `ui.HelpPrinter`

Affiche l'aide complète (équivalent de `--help`).

```java
public static void print()
```

---

## `util.Ansi`

Codes couleur ANSI pour le terminal. Auto-détecté, désactivable.

```java
public static void setEnabled(boolean enabled)
public static boolean isEnabled()

// Retourne le texte coloré si ANSI activé, sinon texte brut
public static String cyan(String text)
public static String yellow(String text)
public static String green(String text)
public static String red(String text)
public static String dim(String text)
```

**Auto-désactivation :** variable d'environnement `NO_COLOR` définie, ou `TERM=dumb`.

---

## `util.AsciiSymbols`

Symboles pour l'affichage. Bascule automatiquement entre Unicode et ASCII.

```java
public static void setUseUnicode(boolean useUnicode)
public static boolean useUnicode()   // Override via NEATIFY_FORCE_UNICODE env var

// Symboles (retournent → ou >, • ou *, ×N ou xN, + ou +)
public static String arrow()
public static String bullet()
public static String times()
public static String plus()
```

**Auto-détection :** basée sur `file.encoding` de la JVM (UTF-8 → Unicode activé).

---

## `util.ResultPrinter`

Affiche le résumé d'une exécution.

```java
public static void print(FileMover.Result result)
```

Affiche :
```
[OK] Moved:   9
[--] Skipped: 0
[!!] Errors:  0
```

Et liste les messages d'erreur détaillés si présents.
