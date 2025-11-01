# Neatify

Automatic file organization based on simple rules.

---

## What It Does

Neatify is a small Java CLI that tidies a folder by moving files into category folders (Documents, Images, Videos, etc.) based on file extensions. It defaults to a safe “dry‑run” preview so you can see changes before applying them.

---

## Install

Requirements:
- Java 21+
- Maven 3.8+ (or the Maven Wrapper)

## Clone:
```bash
git clone https://github.com/Baylox/neatify.git
cd neatify
```

## Build:
### Linux / macOS:
```bash
mvn clean package
```
### Windows (wrapper):
```bash
.\mvnw.cmd clean package
```
# Artefact de build

Interactive (recommended):
```bash
java -jar target/neatify.jar
```

Command‑line examples:
```bash
# Preview (dry‑run)
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties

# Apply changes
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties --apply

# Use built‑in default rules (no file)
java -jar target/neatify.jar --source ~/Downloads --use-default-rules

# Include / Exclude globs
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties \
  --include "**/*.pdf" --exclude "**/node_modules/**"

# Collision strategy (rename | skip | overwrite)
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties --on-collision skip

# Help / Version
java -jar target/neatify.jar --help
java -jar target/neatify.jar --version
```

Undo (optional):
```bash
java -jar target/neatify.jar --source <dir> --undo            # undo last run
java -jar target/neatify.jar --source <dir> --undo-list       # list journals
java -jar target/neatify.jar --source <dir> --undo-run <ts>   # undo by timestamp
```

## JSON Output

- `--json` writes a single JSON document to stdout; no human‑readable messages are printed on stdout.
- Human logs and warnings (if any) go to stderr and log files.
- When no actions are planned, Neatify still emits a valid JSON envelope with `planned=0`, `actions=[]` and empty `result` counters.

Example:
```bash
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties --json
```

Output (example):
```json
{
  "source": "/home/user/Downloads",
  "apply": false,
  "onCollision": "rename",
  "planned": 12,
  "actions": [
    { "source": "/home/user/Downloads/a.jpg", "target": "/home/user/Downloads/Images/a.jpg", "reason": "extension: jpg -> Images" }
  ],
  "result": { "moved": 12, "skipped": 0, "errors": [] }
}
```

Zero‑action case:
```json
{
  "source": "/home/user/Downloads",
  "apply": false,
  "onCollision": "rename",
  "planned": 0,
  "actions": [],
  "result": { "moved": 0, "skipped": 0, "errors": [] }
}
```

JSON + jq examples:
```bash
# Count planned moves
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties --json | jq '.planned'
```
```bash
# Print first 5 planned moves (source -> target)
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties --json \
  | jq -r '.actions[] | "\(.source) -> \(.target)"' | head -n 5
```
```bash
# List unique target folders (POSIX-style splitting)
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties --json \
  | jq -r '.actions[].target | split("/")[:-1] | join("/")' | sort -u
```

---

## Rules

Create a `rules.properties` file:
```properties
# Images
jpg=Images
png=Images

# Documents
pdf=Documents
txt=Documents

# Code
java=Code
py=Code
```

Notes:
- Format: `extension=TargetFolder`
- Extensions are normalized (lowercase, no leading dot)
- Target folders are created if missing
- Invalid folder characters are replaced with `_`
- Files without a matching rule are ignored

---

## Quick Start

```bash
cat > my-rules.properties << EOF
pdf=Documents
jpg=Images
mp4=Videos
zip=Archives
EOF
```
### Preview first
```bash
java -jar target/neatify.jar --source ~/Downloads --rules my-rules.properties
```
### Then apply
```bash
java -jar target/neatify.jar --source ~/Downloads --rules my-rules.properties --apply
```

---

## Safety

- Dry‑run preview by default
- Path traversal protection
- File‑count quota (anti‑DoS)
- Atomic collision handling (rename/skip/overwrite)
- Ignore VCS repositories by default: Neatify skips folders that are version-controlled worktrees when scanning. Markers detected include: `.git`, `.hg`, `.svn`, `.bzr`, `_darcs`, `.pijul`, Fossil (`.fslckout`), and Repo tool (`.repo`). This prevents reorganizing project sources by mistake.
- Apply blocked inside Git repos: `--apply` is blocked when the source directory is inside a Git repository.
- Explicit override: use `--allow-inside-git` if you intentionally want to operate inside a Git repo (not recommended). Preview first and ensure backups.

Tip: always preview before applying on important data.

---

## License

MIT – see `LICENSE`.
