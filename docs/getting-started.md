# Getting Started

## Prerequisites

- **Java 21** or higher
- **Maven 3.8+** (or use the included `mvnw` wrapper — no installation required)

```bash
java -version   # must show Java 21+
```

---

## Build

```bash
# Compile and generate the standalone JAR
./mvnw clean package

# Output artifact:
target/neatify.jar
```

The JAR is self-contained (all dependencies are bundled via Maven Shade Plugin). No additional installation is needed.

---

## Running Neatify

### Interactive mode (recommended for first use)

```bash
java -jar target/neatify.jar
```

A menu is displayed. Select `1` to organize a folder, `2` to create a rules file, `3` to undo the last operation.

See [Interactive mode](interactive-mode.md) for the full guide.

### CLI mode — quick preview (dry-run)

```bash
# Using built-in default rules
java -jar target/neatify.jar --source ~/Downloads --use-default-rules

# Using a custom rules file
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties
```

By default, Neatify runs in **dry-run** mode: it displays what it would do without moving any file.

### CLI mode — apply changes

```bash
# Apply with default rules
java -jar target/neatify.jar --source ~/Downloads --use-default-rules --apply

# Apply with custom rules, skip on collision
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties --apply --on-collision skip
```

---

## Full example

**Before:**
```
Downloads/
  report.pdf
  photo.jpg
  archive.zip
  notes.txt
  video.mp4
```

**Command:**
```bash
java -jar target/neatify.jar --source ~/Downloads --use-default-rules --apply
```

**After:**
```
Downloads/
  Documents/
    report.pdf
    notes.txt
  Images/
    photo.jpg
  Archives/
    archive.zip
  Videos/
    video.mp4
```

---

## Undoing the last operation

```bash
java -jar target/neatify.jar --source ~/Downloads --undo
```

Neatify keeps a journal of every operation in `.neatify/runs/`. See [Undo system](undo-system.md).

---

## Next steps

- Configure your own rules: [Rules format](rules-format.md)
- All available options: [CLI reference](cli-reference.md)
- Understand the architecture: [Architecture](architecture.md)
