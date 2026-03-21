# Neatify — Documentation

![Java 21](https://img.shields.io/badge/Java-21-blue)
![Maven](https://img.shields.io/badge/build-Maven-red)
![Tests](https://img.shields.io/badge/tests-103%20passing-green)

Neatify is an automatic file organization tool. It moves files into subfolders based on their extension, according to configurable rules. It supports an **interactive mode** (menu-driven) and a **CLI mode** (scriptable, with JSON output).

---

## Table of contents

| Document | Description |
|---|---|
| [Getting started](getting-started.md) | Prerequisites, build, first examples |
| [CLI reference](cli-reference.md) | All options, default behavior, examples |
| [Rules format](rules-format.md) | `.properties` syntax, validation, built-in ruleset |
| [Interactive mode](interactive-mode.md) | Interactive menu guide |
| [Undo system](undo-system.md) | `.neatify/runs/` journal format, undo commands |
| [Architecture](architecture.md) | Package structure, data flow, patterns |
| [Security](security.md) | Protections against traversal, symlinks, Git repos, etc. |
| [API — core](api/core.md) | `io.neatify.core` package reference |
| [API — cli](api/cli.md) | `io.neatify.cli` package reference |

---

## Overview

```
Neatify
├── Interactive mode  ──→ Guided menu, prompts, visual confirmation
└── CLI mode          ──→ Arguments, dry-run by default, JSON output
         │
         ├── Planning   : scans files, applies extension rules
         ├── Preview    : displays planned changes before any action
         ├── Execution  : moves files (RENAME / SKIP / OVERWRITE)
         └── Undo       : journals every run, full rollback available
```

## Version

**1.0.0** — Java 21, Maven, SLF4J + Logback, Gson
