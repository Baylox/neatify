# Interactive mode

Interactive mode starts when you run Neatify with no argument:

```bash
./neatify            # Linux / macOS / WSL
.\neatify.cmd        # Windows
```

A banner and the main menu appear.

## Main menu

```
MAIN MENU
═════════════════════════════════════════════════
  1. Organize files
  2. Create a rules file
  3. Undo last run
  4. Show help
  5. Show version
  6. Quit       (or 'q')
```

Type the option number and press Enter. `q` or `6` quits.

## 1 — Organize files

A guided flow:

1. **Source directory** — must exist and be a directory; system directories are rejected.
2. **Rules** — leave empty for the [built-in rules](rules.md), or enter a custom
   `.properties` path.
3. **Filters (optional)** — include / exclude glob patterns, comma-separated, e.g.
   `**/*.pdf` or `**/node_modules/**`.
4. **Preview** — Neatify shows the planned moves grouped by destination folder.
5. **Confirmation** — `y` applies, Enter / `n` cancels.
6. **Collision strategy** — `rename` (default), `skip`, or `overwrite`.

The result reports `Moved` / `Skipped` / `Errors`. The run is journaled in
`.neatify/runs/` and can be reversed with option `3`.

## 2 — Create a rules file

Generates a pre-filled `.properties` file (all default rules) under `custom-rules/`.
Edit it to taste — see [Rules](rules.md) for the syntax.

## 3 — Undo last run

Reverses the moves of the last journaled run, or reports that none exists. See
[Undo](undo.md).

## 4 / 5 — Help and version

Option `4` prints the same help as `--help`; option `5` prints the version.

## Quit

Option `6`, `q`, or `Ctrl+C`.

## Limited terminals

When the terminal does not support Unicode, box-drawing characters (`╔`, `═`, `→`, `•`)
fall back to ASCII (`+`, `-`, `>`, `*`). Force ASCII with `--ascii`, or let Neatify
auto-detect from the terminal encoding.
