# CLI Reference

```
java -jar target/neatify.jar [OPTIONS]
```

Without any argument, Neatify starts in **interactive mode**. With arguments, it runs in **CLI mode**.

---

## Default behavior

| Parameter | Default value |
|---|---|
| Mode | Dry-run (no file is moved) |
| Collision strategy | `rename` (appends `_1`, `_2`…) |
| Max file quota | 100,000 |
| Preview per folder | 5 files |
| Preview sort | `alpha` (alphabetical) |
| ANSI colors | Enabled (auto-detected) |
| Symbols | Unicode (auto-detected) |

---

## Mode selection

| Option | Description |
|---|---|
| *(no argument)* | Starts interactive mode |
| `--interactive`, `-i` | Forces interactive mode even with other flags |
| `--undo` | Undoes the last journaled run |
| `--undo-list` | Lists all journaled runs with metadata |
| `--undo-run <timestamp>` | Undoes the run identified by its Unix ms timestamp |
| `--help`, `-h` | Prints help and exits |
| `--version`, `-v` | Prints version and exits |

---

## Paths (required for organization)

| Option | Description |
|---|---|
| `--source <dir>`, `-s <dir>` | **Required.** Directory to organize. |
| `--rules <file>`, `-r <file>` | Rules `.properties` file to use. |
| `--use-default-rules` | Use the built-in rules (67 pre-defined extensions). Replaces `--rules`. |

`--rules` and `--use-default-rules` are mutually exclusive. One is required (except for `--undo`).

---

## Execution

| Option | Description |
|---|---|
| `--apply`, `-a` | Applies changes. Without this flag: dry-run. |
| `--on-collision <mode>` | Strategy when a destination file already exists. See below. |
| `--max-files <n>` | Limits the number of scanned files (default: 100,000). |

### Collision strategies (`--on-collision`)

| Value | Behavior |
|---|---|
| `rename` *(default)* | Renames the destination: `file.pdf` → `file_1.pdf`, `file_2.pdf`… (max 1000 attempts) |
| `skip` | Ignores files whose destination already exists. Not moved, not counted as error. |
| `overwrite` | Replaces the destination file (atomic operation when supported). |

---

## Filters

Filters use Java NIO **glob** syntax (`**` matches any depth).

| Option | Description |
|---|---|
| `--include <glob>` | Only includes files matching the pattern. Repeatable. |
| `--exclude <glob>` | Excludes files matching the pattern. Repeatable. |

```bash
# Include only PDFs and DOCX
--include "**/*.pdf" --include "**/*.docx"

# Exclude node_modules and temp files
--exclude "**/node_modules/**" --exclude "**/*.tmp"
```

When `--include` is specified, only matching files are candidates. `--exclude` then applies as a subtraction.

---

## Preview display

| Option | Description |
|---|---|
| `--per-folder-preview <n>` | Maximum files shown per folder in the preview (default: 5). |
| `--sort <mode>` | Preview sort order: `alpha` (default), `ext` (by extension), `size` (by size descending). |
| `--no-color` | Disables ANSI colors. |
| `--ascii` | Uses ASCII symbols instead of Unicode (for basic terminals). |

---

## JSON output

| Option | Description |
|---|---|
| `--json` | Emits a JSON object on `stdout`. Logs are redirected to `stderr`. |

JSON output format:

```json
{
  "source": "/home/user/Downloads",
  "apply": true,
  "onCollision": "rename",
  "planned": 5,
  "actions": [
    {
      "source": "/home/user/Downloads/report.pdf",
      "target": "/home/user/Downloads/Documents/report.pdf",
      "reason": "extension: pdf -> Documents"
    }
  ],
  "result": {
    "moved": 5,
    "skipped": 0,
    "errors": []
  }
}
```

In dry-run, `result.moved` reflects the number of planned actions; no file is actually moved.

---

## Logging

| Option | Description |
|---|---|
| `--debug` | Log level DEBUG (very verbose). |
| `--verbose` | Log level INFO. |
| `--quiet`, `-q` | Log level WARN (minimal). |

Without these flags, the default level is INFO (configured in `logback.xml`).

Logs are written to `logs/`:
- `logs/neatify.<date>.log` — application logs
- `logs/security.<date>.log` — security violations only

---

## Security

| Option | Description |
|---|---|
| `--allow-inside-git` | Allows `--apply` inside a Git repository. **Dangerous.** By default, `--apply` is blocked inside a Git repo to avoid reorganizing versioned source code. |

---

## Examples

```bash
# Quick preview with default rules
java -jar target/neatify.jar -s ~/Downloads --use-default-rules

# Apply with custom rules
java -jar target/neatify.jar -s ~/Downloads -r rules.properties --apply

# Filter and apply (PDFs only, skip on collision)
java -jar target/neatify.jar -s ~/Documents -r rules.properties \
  --include "**/*.pdf" --on-collision skip --apply

# JSON output (for scripts)
java -jar target/neatify.jar -s ~/Downloads --use-default-rules --json 2>/dev/null

# Undo last run
java -jar target/neatify.jar -s ~/Downloads --undo

# List all journaled runs
java -jar target/neatify.jar -s ~/Downloads --undo-list

# Undo a specific run
java -jar target/neatify.jar -s ~/Downloads --undo-run 1710953471234

# Debug mode with extended preview
java -jar target/neatify.jar -s ~/Downloads --use-default-rules \
  --per-folder-preview 20 --sort size --debug

# Inside a Git repo (explicit override)
java -jar target/neatify.jar -s ~/project/assets -r rules.properties \
  --apply --allow-inside-git
```
