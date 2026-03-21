# Interactive Mode

Interactive mode is activated by launching Neatify without any argument:

```bash
java -jar target/neatify.jar
```

A banner is displayed, followed by the main menu.

---

## Main menu

```
╔══════════════════════════════════════════════════╗
║                    NEATIFY 1.0.0                 ║
║          Automatic organization tool             ║
╚══════════════════════════════════════════════════╝

MAIN MENU
═════════════════════════════════════════════════════════════════

  1. Organize files
  2. Create a rules file
  3. Undo last run
  4. Show help
  5. Show version
  6. Quit

> _
```

Enter the number of the desired option and press Enter. `q` or `6` exits the application.

---

## Option 1 — Organize files

Full flow guided by prompts:

### Step 1: Source directory

```
Source directory:
> /home/user/Downloads
```

Neatify validates that the path exists and is a directory. System directories are rejected.

### Step 2: Rules

```
Rules file (leave empty to use default rules):
> _
```

- Leave empty to use the **built-in rules** (67 extensions)
- Or enter the path to a custom `.properties` file

### Step 3: Filters (optional)

```
Include patterns (glob, comma-separated, leave empty for all):
> _

Exclude patterns (glob, comma-separated, leave empty for none):
> _
```

Example patterns: `**/*.pdf`, `**/*.jpg,**/*.png`, `**/node_modules/**`

### Step 4: Preview

Neatify displays the planned changes:

```
═══════════════════════════════════════════════════════════════
                      CHANGES PREVIEW
═══════════════════════════════════════════════════════════════

→ Documents/  (3 files)
  • report.pdf
  • notes.txt
  • letter.docx

→ Images/  (5 files)
  • photo.jpg
  • screenshot.png
  • banner.svg
  • + 2 more...

→ Archives/  (1 file)
  • backup.zip

[########################################] 100% (9/9)
```

### Step 5: Confirmation

```
Apply these changes? [y/N]:
> _
```

Answer `y` to apply, or press Enter / `n` to cancel.

### Step 6: Collision strategy

```
Collision strategy [rename/skip/overwrite] (default: rename):
> _
```

- `rename` — Automatically renames if the destination file already exists
- `skip` — Ignores conflicting files
- `overwrite` — Replaces existing files

### Result

```
[OK] Moved:   9
[--] Skipped: 0
[!!] Errors:  0
```

The operation is journaled in `.neatify/runs/` and can be undone with option `3`.

---

## Option 2 — Create a rules file

Generates a pre-filled `.properties` file with all default rules.

```
Rules file name (default: custom-rules/my-rules.properties):
> _
```

The file is created in the current directory, under `custom-rules/`. Edit it to fit your needs.

See [Rules format](rules-format.md) for the full syntax.

---

## Option 3 — Undo last run

```
Undoing last run...

[OK] Restored: 9
[--] Skipped:  0
[!!] Errors:   0
```

Exactly reverses the moves performed during the last journaled run. If no journal exists, a message indicates it.

See [Undo system](undo-system.md) for details.

---

## Option 4 — Help

Displays the same help as `--help` in CLI mode.

---

## Option 5 — Version

```
Neatify version 1.0.0
```

---

## Quit

Option `6`, key `q`, or `Ctrl+C`.

---

## Behavior on limited terminals

If the terminal does not support Unicode, box-drawing characters (`╔`, `═`, `→`, `•`) are replaced by their ASCII equivalents (`+`, `-`, `>`, `*`). Forceable with `--ascii` in CLI, or auto-detected based on terminal encoding.
