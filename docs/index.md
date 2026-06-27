# Neatify documentation

Neatify is a small Java CLI that tidies a folder by moving files into category folders
(Documents, Images, Videos…) based on their extension. It defaults to a safe dry-run
preview, and offers both an interactive menu and a scriptable CLI with JSON output.

The docs follow the [Diátaxis](https://diataxis.fr/) quadrants.

## Tutorial

Start here if you're new.

- [Getting started](getting-started.md) — install, build, first run.

## How-to guides

Task-focused recipes.

- [Interactive mode](guides/interactive-mode.md) — the menu-driven flow.
- [Rules](guides/rules.md) — map extensions to folders (incl. the built-in defaults).
- [Undo](guides/undo.md) — reverse a run.

## Reference

Precise; generated from the code where possible.

- [CLI reference](reference/cli.md) — every option.
- [JSON output](reference/json-output.md) — machine-readable results.

## Explanation

Background and design.

- [Architecture](explanation/architecture.md) — layers, packages, flows, patterns.
- [Security](explanation/security.md) — the protections and their rationale.

## API

The Java API is documented as Javadoc, generated from the source. Build it locally with
`./mvnw javadoc:javadoc` (output in `target/site/apidocs/`).
