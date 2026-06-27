# Getting started

## Prerequisites

- **Java 21+** (a JDK). That's the only prerequisite — Maven comes from the bundled
  wrapper.

```bash
java -version   # must show 21+
```

## Build

The Maven Wrapper downloads Maven automatically, so the same command works everywhere:

```bash
./mvnw package          # Linux / macOS / WSL
.\mvnw.cmd package      # Windows
```

The artifact is `target/neatify.jar`, self-contained (all dependencies bundled via the
Maven Shade Plugin).

## Run

Two launchers ship at the repo root and run the built jar with your arguments:

- `./neatify` — Linux, macOS, WSL, Git-Bash
- `.\neatify.cmd` — Windows (cmd / PowerShell)

The examples below use `./neatify`; substitute `.\neatify.cmd` on Windows.

### Interactive mode (recommended for first use)

```bash
./neatify
```

A menu appears: `1` organize a folder, `2` create a rules file, `3` undo the last run.
See the [interactive mode guide](guides/interactive-mode.md).

### Preview (dry-run)

```bash
# Built-in default rules
./neatify --source ~/Downloads --use-default-rules

# A custom rules file
./neatify --source ~/Downloads --rules rules.properties
```

By default Neatify runs a **dry-run**: it shows what it would do without moving anything.

### Apply changes

```bash
./neatify --source ~/Downloads --use-default-rules --apply
./neatify --source ~/Downloads --rules rules.properties --apply --on-collision skip
```

## Full example

**Before:**

```text
Downloads/
  report.pdf  photo.jpg  archive.zip  notes.txt  video.mp4
```

**Command:**

```bash
./neatify --source ~/Downloads --use-default-rules --apply
```

**After:**

```text
Downloads/
  Documents/  report.pdf  notes.txt
  Images/     photo.jpg
  Archives/   archive.zip
  Videos/     video.mp4
```

## Undo

```bash
./neatify --source ~/Downloads --undo
```

Every run is journaled under `.neatify/runs/`. See the [undo guide](guides/undo.md).

## Next steps

- [Rules](guides/rules.md) — configure your own mappings
- [CLI reference](reference/cli.md) — all options
- [Architecture](explanation/architecture.md) — how it works
