# Architecture

## Overview

Neatify is structured in three well-separated layers:

```
┌──────────────────────────────────────────────────┐
│                     cli/ui/                       │  Presentation
│     Display  Preview  HelpPrinter  InteractiveCLI │
├──────────────────────────────────────────────────┤
│                cli/  +  cli/core/                 │  Orchestration
│   FileOrganizationExecutor  FileOrganizer         │
│   ArgumentParser  CLIConfig  UndoExecutor         │
├──────────────────────────────────────────────────┤
│                      core/                        │  Business logic
│   contract/FileMover  contract/RulesProvider      │
│   LocalFileMover  PropertiesRulesProvider         │
│   FilePlanner  FileExecutor  Rules  PathSecurity  │
└──────────────────────────────────────────────────┘
```

The `core/` layer has no dependency on the upper layers and is independently testable.
Behaviour is exposed through **interfaces** in `core.contract`, with concrete
implementations alongside them — callers depend on the contract, not the implementation.

## Package structure

```
io.neatify/
│
├── Neatify.java                     Entry point (main)
│
├── core/                            Pure business logic
│   ├── contract/
│   │   ├── FileMover                Interface: plan() + execute() (+ nested
│   │   │                            Action/Result records, CollisionStrategy,
│   │   │                            MoveListener)
│   │   └── RulesProvider            Interface: load() + getDefaults()
│   ├── LocalFileMover               FileMover implementation (local filesystem)
│   ├── PropertiesRulesProvider      RulesProvider implementation (.properties)
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
    │   ├── CliOption                Single source of truth for the CLI options
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
    │   ├── HelpPrinter              Help text (derived from CliOption)
    │   ├── Preview                  Formatted preview of planned changes
    │   └── InteractiveCLI           Interactive mode main menu
    │
    └── util/
        ├── Ansi                     ANSI color codes (auto-detected)
        ├── AsciiSymbols             Unicode/ASCII symbols (auto-detected)
        └── ResultPrinter            Execution summary display
```

## Execution flow — CLI mode

```
main(args)
  │
  ├── no args ──→ InteractiveCLI.run()
  │
  └── args present
        ├── ArgumentParser.parse(args) → CLIConfig (immutable)
        ├── configureLogLevel(config)
        └── FileOrganizationExecutor.execute(config)
              ├── 1. validatePaths()         → PathSecurity.validateSourceDir()
              ├── 2. enforceGitRepositoryPolicy()
              ├── 3. loadRules()             → RulesProvider (load / defaults)
              ├── 4. planActions()           → FileMover.plan()
              │        └── FilePlanner.plan() → walkFileTree → planFor() per file
              │             ├── filter includes/excludes
              │             ├── FileMetadata.from()
              │             ├── Rules.getTargetFolder()
              │             └── PathSecurity.safeResolveWithin()
              ├── 5. showPreview() or printJson()
              └── 6. executeActions()        → FileMover.execute(...)
                       └── FileExecutor.execute() → strategy.move() per action
                            └── listener.onMoved() → UndoExecutor.Move
                       └── UndoExecutor.appendRun() [if --apply]
```

## Execution flow — Interactive mode

```
InteractiveCLI.run()
  ├── [Banner]
  └── Menu loop
        ├── 1 → FileOrganizer.organize()   (prompt source/rules/filters → plan →
        │                                    preview → confirm → execute → journal)
        ├── 2 → RulesFileCreator.create()  (CREATE_NEW write under custom-rules/)
        ├── 3 → UndoExecutor.undoLast()
        ├── 4 → HelpPrinter.print()
        ├── 5 → AppInfo version
        └── 6/q → return
```

## Undo flow

```
UndoExecutor.undoLast(sourceRoot)
  ├── undoLastV2()
  │     ├── List .neatify/runs/*.json, pick most recent (numeric timestamp sort)
  │     └── undoRunFile(runFile)
  │           ├── Gson.fromJson() → run document
  │           ├── For each move (from, to):
  │           │     ├── Scope check (stays within sourceRoot)
  │           │     ├── Existence check (to exists, from does not)
  │           │     ├── PathSecurity.assertResolvedWithin(sourceRoot, from)
  │           │     ├── PathSecurity.assertResolvedWithin(sourceRoot, to)
  │           │     └── Files.move(to, from)
  │           └── Files.deleteIfExists(runFile)
  │
  └── [fallback] undoLastFromLegacyManifest()  (reads legacy manifest.json)
```

## Design patterns

| Pattern | Where | Description |
|---------|-------|-------------|
| **Ports & adapters** | `contract/FileMover` + `LocalFileMover`, `contract/RulesProvider` + `PropertiesRulesProvider` | Behaviour behind interfaces; callers depend on the contract |
| **Record** | `FileMover.Action`, `FileMover.Result`, `FileMetadata`, `UndoExecutor.Move` | Immutable DTOs (Java 21 value types) |
| **Strategy** | `FileMover.CollisionStrategy` (enum) | Each of RENAME/SKIP/OVERWRITE encapsulates its `move()` logic |
| **Listener** | `FileMover.MoveListener` | `onMoved(from, to)` decouples execution from journaling |
| **Single source of truth** | `CliOption` | Flags + help + generated docs all derive from one enum |
| **Template Method** | `FilePlanner` (SimpleFileVisitor) | `preVisitDirectory` + `visitFile` overridden |

## External dependencies

| Library | Usage |
|---------|-------|
| SLF4J API | Logging facade |
| Logback Classic | Logging implementation (console + files) |
| Gson | JSON serialization (undo journal, JSON output mode) |
| SpotBugs annotations | `@SuppressFBWarnings` (provided scope, not shipped) |
| JUnit 5 | Unit tests (test scope only) |

Exact versions are pinned in `pom.xml`. All runtime dependencies are bundled in
`target/neatify.jar` via the Maven Shade Plugin.

## Quality gate (`./mvnw verify`)

| Tool | Role |
|------|------|
| Maven Enforcer | JDK 21+, Maven 3.8+, dependency convergence |
| JaCoCo | Coverage report + 55% line floor |
| Spotless | Import order and whitespace hygiene |
| SpotBugs | Static bug detection (effort max, medium threshold) |
| PMD | Code smells (custom ruleset in `pmd-ruleset.xml`) |
| OWASP Dependency-Check | CVE scan, opt-in via `-Psecurity-scan` |
