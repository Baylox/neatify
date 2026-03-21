# Neatify — Documentation

![Java 21](https://img.shields.io/badge/Java-21-blue)
![Maven](https://img.shields.io/badge/build-Maven-red)
![Tests](https://img.shields.io/badge/tests-103%20passing-green)

Neatify est un outil d'organisation automatique de fichiers. Il déplace les fichiers vers des sous-dossiers en fonction de leur extension, selon des règles configurables. Il propose un **mode interactif** (menu) et un **mode CLI** (scriptable, avec JSON output).

---

## Table des matières

| Document | Description |
|---|---|
| [Démarrage rapide](getting-started.md) | Prérequis, build, premiers exemples |
| [Référence CLI](cli-reference.md) | Toutes les options, comportements par défaut, exemples |
| [Format des règles](rules-format.md) | Syntaxe `.properties`, validation, règles par défaut |
| [Mode interactif](interactive-mode.md) | Guide du menu interactif |
| [Système d'annulation](undo-system.md) | Journalisation `.neatify/runs/`, commandes undo |
| [Architecture](architecture.md) | Structure des packages, flux de données, patterns |
| [Sécurité](security.md) | Protections contre traversal, symlinks, repos Git, etc. |
| [API — core](api/core.md) | Référence du package `io.neatify.core` |
| [API — cli](api/cli.md) | Référence du package `io.neatify.cli` |

---

## Vue d'ensemble

```
Neatify
├── Mode interactif  ──→ Menu guidé, prompts, confirmation visuelle
└── Mode CLI         ──→ Arguments, dry-run par défaut, JSON output
         │
         ├── Planning   : scanne les fichiers, applique les règles d'extension
         ├── Preview    : affiche les changements prévus avant toute action
         ├── Execution  : déplace les fichiers (RENAME / SKIP / OVERWRITE)
         └── Undo       : journalise chaque run, annulation complète possible
```

## Version

**1.0.0** — Java 21, Maven, SLF4J + Logback, Gson
