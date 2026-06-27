# CLI reference

```text
neatify [options]
```

Running `neatify` with no arguments starts [interactive mode](../guides/interactive-mode.md).
On Windows, use `.\neatify.cmd`.

By default Neatify runs a **dry-run** (preview only). Add `--apply` to actually move files.

## Options

The table below is generated from the code, so it always matches the real flags.

<!-- AUTOGEN:flags START -->

### Modes

| Option | Argument | Description |
|--------|----------|-------------|
| `--interactive`, `-i` |  | Start interactive mode |
| `--undo` |  | Undo the last run (journal) |
| `--undo-list` |  | List journaled runs (.neatify/runs) |
| `--undo-run` | `<timestamp>` | Undo a specific run |
| `--help`, `-h` |  | Show this help |
| `--version`, `-v` |  | Show version |

### Paths

| Option | Argument | Description |
|--------|----------|-------------|
| `--source`, `-s` | `<dir>` | Directory to organize (required) |
| `--rules`, `-r` | `<file>` | Rules file (required unless --use-default-rules) |
| `--use-default-rules` |  | Use built-in default rules (no --rules) |

### Execution

| Option | Argument | Description |
|--------|----------|-------------|
| `--apply`, `-a` |  | Apply changes (otherwise dry-run) |
| `--json` |  | JSON output (preview + result) |
| `--on-collision` | `<mode>` | Collision: rename (default), skip, overwrite |
| `--max-files` | `<n>` | Max files to scan (default: 100000) |
| `--include` | `<glob>` | Include (repeatable), e.g. **/*.pdf |
| `--exclude` | `<glob>` | Exclude (repeatable), e.g. **/node_modules/** |
| `--allow-inside-git` |  | Allow operating inside Git repositories (unsafe) |

### Display

| Option | Argument | Description |
|--------|----------|-------------|
| `--no-color` |  | Disable ANSI colors |
| `--ascii` |  | Use ASCII symbols instead of Unicode |
| `--per-folder-preview` | `<n>` | Files per folder to display (default: 5) |
| `--sort` | `<mode>` | File sort: alpha, ext or size (default: alpha) |

### Logging

| Option | Argument | Description |
|--------|----------|-------------|
| `--quiet`, `-q` |  | Minimal output (WARN level) |
| `--verbose` |  | Verbose output (INFO level, default) |
| `--debug` |  | Very verbose output (DEBUG level) |

<!-- AUTOGEN:flags END -->

## Collision strategies

`--on-collision` controls what happens when a destination file already exists:

| Mode | Behavior |
|------|----------|
| `rename` (default) | Keep both; the incoming file gets a numeric suffix |
| `skip` | Leave the existing file; do not move the incoming one |
| `overwrite` | Replace the existing file |

## Examples

```bash
# Preview with built-in rules
./neatify --source ~/Downloads --use-default-rules

# Apply with a custom rules file and skip collisions
./neatify --source ~/Downloads --rules rules.properties --apply --on-collision skip

# Only PDFs, excluding a folder
./neatify --source ~/Downloads --use-default-rules --include "**/*.pdf" --exclude "**/node_modules/**"
```

See [JSON output](json-output.md) for machine-readable results, and
[undo](../guides/undo.md) to revert a run.
