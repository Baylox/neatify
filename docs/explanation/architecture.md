# Architecture

## Overview

Neatify is structured in three well-separated layers:

```
┌───────────────────────────────────────────────────┐
│                     cli/ui/                       │  Presentation
│   Display  Preview  HelpPrinter  InteractiveCLI   │
│   Console/SystemConsole  Theme  DisplayOptions    │
├───────────────────────────────────────────────────┤
│                cli/  +  cli/core/                 │  Orchestration
│   AppContext  FileOrganizationExecutor            │
│   FileOrganizer  ArgumentParser  CLIConfig        │
├───────────────────────────────────────────────────┤
│                      core/                        │  Business logic
│   contract/FileMover  contract/RulesProvider      │
│   contract/RunJournal  OrganizationService        │
│   LocalFileMover  PropertiesRulesProvider         │
│   FileSystemRunJournal                            │
│   FilePlanner  FileExecutor  Rules  PathSecurity  │
└───────────────────────────────────────────────────┘
```

The `core/` layer has no dependency on the upper layers and is independently testable.
Behaviour is exposed through **interfaces** in `core.contract`, with concrete
implementations alongside them — callers depend on the contract, not the implementation.

`AppContext` is the **composition root**: the one place that instantiates the concrete
implementations (`LocalFileMover`, `PropertiesRulesProvider`, `FileSystemRunJournal`,
`SystemConsole`) and injects them downstream. Everything else receives its collaborators
through constructors, so tests can substitute fakes — including for the interactive flows,
which read through the `Console` interface rather than a process-wide `Scanner`.

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
│   │   ├── RulesProvider            Interface: load() + getDefaults()
│   │   └── RunJournal               Interface: append() + undoLast() + undoRun()
│   │                                + list() (+ nested Move/UndoResult/RunMeta)
│   ├── OrganizationService          Shared plan → dry-run|apply → journal flow
│   ├── LocalFileMover               FileMover implementation (local filesystem)
│   ├── PropertiesRulesProvider      RulesProvider implementation (.properties)
│   ├── FileSystemRunJournal         RunJournal implementation (.neatify/runs/)
│   ├── FilePlanner                  (package-private) Tree traversal
│   ├── FileExecutor                 (package-private) Actual moves
│   ├── Rules                        Rule loading and validation
│   ├── DefaultRules                 (package-private) Built-in rules
│   ├── FileMetadata                 Immutable record: extension, size, date
│   └── PathSecurity                 Path security validation
│
└── cli/
    ├── AppContext                   Composition root (wires the implementations)
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
    │   └── RulesFileCreator         Rules file creation in interactive mode
    │
    ├── ui/
    │   ├── Console                  Interface: interactive input (readInput, waitForEnter)
    │   ├── SystemConsole            Console implementation (stdin)
    │   ├── DisplayOptions           Immutable record: color + Unicode preferences
    │   ├── Theme                    Colors and symbols, resolved from DisplayOptions
    │   ├── Display                  Console output (print, prompts, tables)
    │   ├── HelpPrinter              Help text (derived from CliOption)
    │   ├── Preview                  Formatted preview of planned changes
    │   └── InteractiveCLI           Interactive mode main menu
    │
    └── util/
        └── ResultPrinter            Execution summary display
```

## Execution flow — CLI mode

```
main(args)
  │
  ├── AppContext.production()   (wires FileMover, RulesProvider, RunJournal, Console)
  │
  ├── no args ──→ InteractiveCLI.run()
  │
  └── args present
        ├── ArgumentParser.parse(args) → CLIConfig (immutable)
        ├── configureLogLevel(config)
        └── FileOrganizationExecutor.from(context).execute(config)
              ├── 1. validatePaths()         → PathSecurity.validateSourceDir()
              ├── 2. enforceGitRepositoryPolicy()
              ├── 3. applyDisplayOptions()   → DisplayOptions → Theme
              ├── 4. loadRules()             → RulesProvider (load / defaults)
              ├── 5. planActions()           → OrganizationService.plan()
              │        └── FileMover.plan()
              │             └── FilePlanner.plan() → walkFileTree → planFor() per file
              │                  ├── filter includes/excludes
              │                  ├── FileMetadata.from()
              │                  ├── Rules.getTargetFolder()
              │                  └── PathSecurity.safeResolveWithin()
              ├── 6. showPreview() or printJson()
              └── 7. executeActions()
                       ├── [dry-run] OrganizationService.dryRun()
                       └── [--apply] OrganizationService.apply()
                             ├── FileMover.execute(...)
                             │     └── FileExecutor.execute() → strategy.move() per action
                             │          └── listener.onMoved() → RunJournal.Move
                             └── RunJournal.append(root, onCollision, moves)
```

`OrganizationService` is the single place where moving and journaling are sequenced, so both
front-ends — the flag-driven `FileOrganizationExecutor` and the interactive `FileOrganizer` —
share it instead of duplicating the flow. Journaling failures are non-fatal: the files are
already moved, so the result is returned regardless and the error surfaces through
`Outcome.journalError()`.

## Execution flow — Interactive mode

```
InteractiveCLI.run()
  ├── [Banner]
  └── Menu loop
        ├── 1 → FileOrganizer.organize()   (prompt source/rules/filters → plan →
        │                                    preview → confirm → execute → journal)
        ├── 2 → RulesFileCreator.create()  (CREATE_NEW write under custom-rules/)
        ├── 3 → RunJournal.undoLast()
        ├── 4 → HelpPrinter.print()
        ├── 5 → AppInfo version
        └── 6/q → return
```

## Undo flow

```
FileSystemRunJournal.undoLast(sourceRoot)
  ├── undoLastV2()
  │     ├── List .neatify/runs/*.json, pick most recent (numeric timestamp sort)
  │     └── undoRunFile(runFile)
  │           ├── Gson.fromJson() → run document
  │           ├── restoreMoves() — for each move (from, to):
  │           │     ├── Scope check (stays within sourceRoot)
  │           │     ├── Existence check (to exists, from does not)
  │           │     ├── PathSecurity.assertResolvedWithin(sourceRoot, from)
  │           │     ├── PathSecurity.assertResolvedWithin(sourceRoot, to)
  │           │     └── Files.move(to, from)
  │           └── Files.deleteIfExists(runFile)
  │
  └── [fallback] undoLastFromLegacyManifest()  (reads legacy manifest.json)
```

`undoRun(root, timestamp)` targets one specific run instead of the latest, and `list(root)`
returns the persisted runs (most recent first) as `RunMeta` records — these back the
`--undo-run` and `--undo-list` flags.

## Design patterns

| Pattern | Where | Description |
|---------|-------|-------------|
| **Ports & adapters** | `contract/FileMover` + `LocalFileMover`, `contract/RulesProvider` + `PropertiesRulesProvider`, `contract/RunJournal` + `FileSystemRunJournal`, `ui/Console` + `SystemConsole` | Behaviour behind interfaces; callers depend on the contract |
| **Composition root** | `AppContext` | The only place that calls `new` on implementations; everything else is injected |
| **Record** | `FileMover.Action`, `FileMover.Result`, `FileMetadata`, `RunJournal.Move`, `RunJournal.UndoResult`, `RunJournal.RunMeta`, `OrganizationService.Request`, `DisplayOptions` | Immutable DTOs (Java 21 value types) |
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
