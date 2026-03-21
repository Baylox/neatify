# Architecture

## Vue d'ensemble

Neatify est structuré en trois couches bien séparées :

```
┌─────────────────────────────────────────────┐
│                  cli/ui/                     │  Présentation
│    Display  Preview  HelpPrinter  InteractiveCLI │
├─────────────────────────────────────────────┤
│              cli/  +  cli/core/              │  Orchestration
│  FileOrganizationExecutor  FileOrganizer     │
│  ArgumentParser  CLIConfig  UndoExecutor     │
├─────────────────────────────────────────────┤
│                   core/                      │  Métier
│  FileMover  FilePlanner  FileExecutor        │
│  Rules  FileMetadata  PathSecurity           │
└─────────────────────────────────────────────┘
```

La couche `core/` ne dépend d'aucune couche supérieure. Elle est testable indépendamment.

---

## Structure des packages

```
io.neatify/
│
├── Neatify.java                     Point d'entrée (main)
│
├── core/                            Logique métier pure
│   ├── FileMover                    API publique : plan() + execute()
│   ├── FilePlanner                  (package-private) Parcours arborescence
│   ├── FileExecutor                 (package-private) Mouvements réels
│   ├── Rules                        Chargement et validation des règles
│   ├── DefaultRules                 (package-private) Règles intégrées
│   ├── FileMetadata                 Record immuable : extension, taille, date
│   └── PathSecurity                 Validation sécurité des chemins
│
└── cli/
    ├── AppInfo                      Version et métadonnées de l'app
    ├── FileOrganizationExecutor     Orchestration du flux CLI complet
    │
    ├── args/
    │   ├── ArgumentParser           Parse les arguments CLI
    │   └── CLIConfig                Configuration immuable après parsing
    │
    ├── core/
    │   ├── FileOrganizer            Flux d'organisation en mode interactif
    │   ├── RulesFileCreator         Création de fichier de règles en mode interactif
    │   └── UndoExecutor             Journalisation et annulation des runs
    │
    ├── ui/
    │   ├── Display                  Output console (print, prompts, tables)
    │   ├── HelpPrinter              Texte d'aide
    │   ├── Preview                  Aperçu formaté des changements prévus
    │   └── InteractiveCLI           Menu principal du mode interactif
    │
    └── util/
        ├── Ansi                     Codes couleur ANSI (auto-détection)
        ├── AsciiSymbols             Symboles Unicode/ASCII (auto-détection)
        └── ResultPrinter            Affichage du résumé d'exécution
```

---

## Flux d'exécution — Mode CLI

```
main(args)
  │
  ├── args vide ──→ InteractiveCLI.run()
  │
  └── args présents
        │
        ├── ArgumentParser.parse(args)
        │     └── CLIConfig (immuable)
        │
        ├── configureLogLevel(config)
        │
        └── FileOrganizationExecutor.execute(config)
              │
              ├── 1. validatePaths()
              │     └── PathSecurity.validateSourceDir()
              │
              ├── 2. enforceGitRepositoryPolicy()
              │     └── isInsideGitRepository() → bloque --apply si repo Git
              │
              ├── 3. loadRules()
              │     └── Rules.load() ou Rules.getDefaults()
              │
              ├── 4. planActions()
              │     └── FileMover.plan()
              │           └── FilePlanner.plan()
              │                 └── Files.walkFileTree()
              │                       └── planFor() par fichier
              │                             ├── filter includes/excludes
              │                             ├── FileMetadata.from()
              │                             ├── Rules.getTargetFolder()
              │                             └── PathSecurity.safeResolveWithin()
              │
              ├── 5. showPreview() ou printJson()
              │     └── Preview.render()
              │
              └── 6. executeActions()
                    └── FileMover.execute(actions, dryRun, strategy, listener)
                          └── FileExecutor.execute()
                                └── strategy.move() par action
                                      └── listener.onMoved() → UndoExecutor.Move
                          └── UndoExecutor.appendRun() [si apply]
```

---

## Flux d'exécution — Mode interactif

```
InteractiveCLI.run()
  │
  ├── [Banneau]
  │
  └── Boucle menu
        │
        ├── 1 → FileOrganizer.organize()
        │       ├── Prompt source dir
        │       ├── Prompt règles
        │       ├── Prompt filtres
        │       ├── FileMover.plan()
        │       ├── Preview.print()
        │       ├── Prompt confirmation
        │       ├── Prompt stratégie collision
        │       ├── FileMover.execute() avec MoveListener
        │       └── UndoExecutor.appendRun()
        │
        ├── 2 → RulesFileCreator.create()
        │       ├── Prompt nom fichier
        │       ├── PathSecurity.validateRelativeSubpath()
        │       └── Files.writeString(..., CREATE_NEW)
        │
        ├── 3 → UndoExecutor.undoLast()
        │       └── undoLastV2() ou fallback legacy
        │
        ├── 4 → HelpPrinter.print()
        ├── 5 → AppInfo.neatify().version()
        └── 6/q → return
```

---

## Flux d'annulation

```
UndoExecutor.undoLast(sourceRoot)
  │
  ├── undoLastV2()
  │     ├── Liste .neatify/runs/*.json
  │     ├── Sélectionne le plus récent (tri numérique sur timestamp)
  │     └── undoRunFile(runFile)
  │           ├── Gson.fromJson() → RunDoc
  │           ├── Pour chaque move (from, to) :
  │           │     ├── Scope check (restent dans sourceRoot)
  │           │     ├── Existence check (to existe, from n'existe pas)
  │           │     ├── PathSecurity.assertNoSymlinkInAncestry(from)
  │           │     ├── PathSecurity.assertNoSymlinkInAncestry(to)
  │           │     ├── Files.createDirectories(from.getParent())
  │           │     └── Files.move(to, from)
  │           └── Files.deleteIfExists(runFile)
  │
  └── [fallback] undoLastFromLegacyManifest()
        └── Lit manifest.json (format {"runs":[{"moves":[...]}]})
```

---

## Patterns de conception

| Pattern | Où | Description |
|---|---|---|
| **Record** | `FileMover.Action`, `FileMover.Result`, `FileMetadata`, `UndoExecutor.Move` | DTOs immuables, value types Java 21 |
| **Strategy** | `FileMover.CollisionStrategy` (enum) | Chaque stratégie (RENAME, SKIP, OVERWRITE) encapsule sa logique de `move()` |
| **Listener** | `FileMover.MoveListener` | Callback `onMoved(from, to)` pour découpler l'exécution de la journalisation |
| **Builder** | `Preview.Config` | Enchaînement fluide `new Config().maxFilesPerFolder(10).sortMode(EXT)` |
| **Template Method** | `FilePlanner` (SimpleFileVisitor) | `preVisitDirectory` + `visitFile` surchargés pour customiser le parcours |
| **Façade** | `FileMover` | Cache `FilePlanner` et `FileExecutor` (package-private) derrière une API simple |
| **Null Object** | `CollisionStrategy.SKIP` | Retourne `null` au lieu d'une exception quand le move doit être ignoré |

---

## Dépendances externes

| Bibliothèque | Version | Usage |
|---|---|---|
| SLF4J API | 2.0.16 | Façade de logging |
| Logback Classic | 1.5.19 | Implémentation de logging (console + fichiers) |
| Gson | 2.11.0 | Sérialisation JSON (undo journal, JSON output mode) |
| JUnit 5 | 5.11.3 | Tests unitaires (scope test) |

Toutes les dépendances sont embarquées dans `target/neatify.jar` via Maven Shade Plugin.
