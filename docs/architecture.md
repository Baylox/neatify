# Architecture

## Overview

Neatify is structured in three well-separated layers:

```
┌─────────────────────────────────────────────┐
│                  cli/ui/                     │  Presentation
│    Display  Preview  HelpPrinter  InteractiveCLI │
├─────────────────────────────────────────────┤
│              cli/  +  cli/core/              │  Orchestration
│  FileOrganizationExecutor  FileOrganizer     │
│  ArgumentParser  CLIConfig  UndoExecutor     │
├─────────────────────────────────────────────┤
│                   core/                      │  Business logic
│  FileMover  FilePlanner  FileExecutor        │
│  Rules  FileMetadata  PathSecurity           │
└─────────────────────────────────────────────┘
```

The `core/` layer has no dependency on upper layers and is independently testable.

---

## Package structure

```
io.neatify/
│
├── Neatify.java                     Entry point (main)
│
├── core/                            Pure business logic
│   ├── FileMover                    Public API: plan() + execute()
│   ├── FilePlanner                  (package-private) Tree traversal
│   ├── FileExecutor                 (package-private) Actual moves
│   ├── Rules                        Rule loading and validation
│   ├── DefaultRules                 (package-private) Built-in rules
│   ├── FileMetadata                 Immutable record: extension, size, date
│   └── PathSecurity                 Path security validation
│
└── cli/
    ├── AppInfo                      Version and app metadata
    ├── FileOrganizationExecutor     CLI flow orchestration
    │
    ├── args/
    │   ├── ArgumentParser           Parses CLI arguments
    │   └── CLIConfig                Immutable configuration after parsing
    │
    ├── core/
    │   ├── FileOrganizer            Organization flow in interactive mode
    │   ├── RulesFileCreator         Rules file creation in interactive mode
    │   └── UndoExecutor             Run journaling and undo
    │
    ├── ui/
    │   ├── Display                  Console output (print, prompts, tables)
    │   ├── HelpPrinter              Help text
    │   ├── Preview                  Formatted preview of planned changes
    │   └── InteractiveCLI           Interactive mode main menu
    │
    └── util/
        ├── Ansi                     ANSI color codes (auto-detected)
        ├── AsciiSymbols             Unicode/ASCII symbols (auto-detected)
        └── ResultPrinter            Execution summary display
```

---

## Execution flow — CLI mode

```
main(args)
  │
  ├── no args ──→ InteractiveCLI.run()
  │
  └── args present
        │
        ├── ArgumentParser.parse(args)
        │     └── CLIConfig (immutable)
        │
        ├── configureLogLevel(config)
        │
        └── FileOrganizationExecutor.execute(config)
              │
              ├── 1. validatePaths()
              │     └── PathSecurity.validateSourceDir()
              │
              ├── 2. enforceGitRepositoryPolicy()
              │     └── isInsideGitRepository() → blocks --apply inside Git repo
              │
              ├── 3. loadRules()
              │     └── Rules.load() or Rules.getDefaults()
              │
              ├── 4. planActions()
              │     └── FileMover.plan()
              │           └── FilePlanner.plan()
              │                 └── Files.walkFileTree()
              │                       └── planFor() per file
              │                             ├── filter includes/excludes
              │                             ├── FileMetadata.from()
              │                             ├── Rules.getTargetFolder()
              │                             └── PathSecurity.safeResolveWithin()
              │
              ├── 5. showPreview() or printJson()
              │     └── Preview.render()
              │
              └── 6. executeActions()
                    └── FileMover.execute(actions, dryRun, strategy, listener)
                          └── FileExecutor.execute()
                                └── strategy.move() per action
                                      └── listener.onMoved() → UndoExecutor.Move
                          └── UndoExecutor.appendRun() [if apply]
```

---

## Execution flow — Interactive mode

```
InteractiveCLI.run()
  │
  ├── [Banner]
  │
  └── Menu loop
        │
        ├── 1 → FileOrganizer.organize()
        │       ├── Prompt source dir
        │       ├── Prompt rules
        │       ├── Prompt filters
        │       ├── FileMover.plan()
        │       ├── Preview.print()
        │       ├── Prompt confirmation
        │       ├── Prompt collision strategy
        │       ├── FileMover.execute() with MoveListener
        │       └── UndoExecutor.appendRun()
        │
        ├── 2 → RulesFileCreator.create()
        │       ├── Prompt file name
        │       ├── PathSecurity.validateRelativeSubpath()
        │       └── Files.writeString(..., CREATE_NEW)
        │
        ├── 3 → UndoExecutor.undoLast()
        │       └── undoLastV2() or legacy fallback
        │
        ├── 4 → HelpPrinter.print()
        ├── 5 → AppInfo.neatify().version()
        └── 6/q → return
```

---

## Undo flow

```
UndoExecutor.undoLast(sourceRoot)
  │
  ├── undoLastV2()
  │     ├── List .neatify/runs/*.json
  │     ├── Select most recent (numeric sort on timestamp)
  │     └── undoRunFile(runFile)
  │           ├── Gson.fromJson() → RunDoc
  │           ├── For each move (from, to):
  │           │     ├── Scope check (stays within sourceRoot)
  │           │     ├── Existence check (to exists, from does not)
  │           │     ├── PathSecurity.assertNoSymlinkInAncestry(from)
  │           │     ├── PathSecurity.assertNoSymlinkInAncestry(to)
  │           │     ├── Files.createDirectories(from.getParent())
  │           │     └── Files.move(to, from)
  │           └── Files.deleteIfExists(runFile)
  │
  └── [fallback] undoLastFromLegacyManifest()
        └── Reads manifest.json (format {"runs":[{"moves":[...]}]})
```

---

## Design patterns

| Pattern | Where | Description |
|---|---|---|
| **Record** | `FileMover.Action`, `FileMover.Result`, `FileMetadata`, `UndoExecutor.Move` | Immutable DTOs, Java 21 value types |
| **Strategy** | `FileMover.CollisionStrategy` (enum) | Each strategy (RENAME, SKIP, OVERWRITE) encapsulates its `move()` logic |
| **Listener** | `FileMover.MoveListener` | `onMoved(from, to)` callback decouples execution from journaling |
| **Builder** | `Preview.Config` | Fluent chaining: `new Config().maxFilesPerFolder(10).sortMode(EXT)` |
| **Template Method** | `FilePlanner` (SimpleFileVisitor) | `preVisitDirectory` + `visitFile` overridden to customize traversal |
| **Facade** | `FileMover` | Hides `FilePlanner` and `FileExecutor` (package-private) behind a simple API |
| **Null Object** | `CollisionStrategy.SKIP` | Returns `null` instead of an exception when the move should be silently ignored |

---

## External dependencies

| Library | Version | Usage |
|---|---|---|
| SLF4J API | 2.0.17 | Logging facade |
| Logback Classic | 1.5.34 | Logging implementation (console + files) |
| Gson | 2.14.0 | JSON serialization (undo journal, JSON output mode) |
| SpotBugs annotations | 4.10.2 | `@SuppressFBWarnings` (provided scope, not shipped) |
| JUnit 5 | 5.11.3 | Unit tests (test scope only) |

All runtime dependencies are bundled in `target/neatify.jar` via Maven Shade Plugin.

---

## Quality gate (mvn verify)

| Tool | Role |
|---|---|
| Maven Enforcer | JDK 21+, Maven 3.8+, dependency convergence |
| JaCoCo | Coverage report + 55% line floor |
| Spotless | Import order and whitespace hygiene |
| SpotBugs | Static bug detection (effort max, medium threshold) |
| PMD | Code smells (custom ruleset in `pmd-ruleset.xml`) |
| OWASP Dependency-Check | CVE scan, opt-in via `-Psecurity-scan` |
