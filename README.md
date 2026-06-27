# Neatify

Automatic file organization based on simple rules.

---

## What It Does

Neatify is a small Java CLI that tidies a folder by moving files into category folders (Documents, Images, Videos, etc.) based on file extensions. It defaults to a safe "dry‑run" preview so you can see changes before applying them.

---

## Getting Started

### Requirements
- Java 21+ (a JDK). That's the only prerequisite — Maven is provided by the bundled wrapper.

### Clone
```bash
git clone https://github.com/Baylox/neatify.git
cd neatify
```

### Build
The Maven Wrapper downloads Maven automatically, so the same command works on every OS:

**Linux / macOS / WSL:**
```bash
./mvnw package
```
**Windows:**
```bash
.\mvnw.cmd package
```

**Build Artifact:** `target/neatify.jar`

### Usage

Two launchers ship at the repo root and simply run the built jar with your arguments,
so you don't have to type `java -jar target/neatify.jar` every time:

- `./neatify` — Linux, macOS, WSL, Git‑Bash
- `.\neatify.cmd` — Windows (cmd.exe / PowerShell)

The examples below use `./neatify`; on Windows substitute `.\neatify.cmd`.

**Interactive mode (recommended):**
```bash
./neatify
```

**Basic usage:**
```bash
# Preview (dry‑run)
./neatify --source ~/Downloads --rules rules.properties

# Apply changes
./neatify --source ~/Downloads --rules rules.properties --apply

# Use built‑in default rules (no file)
./neatify --source ~/Downloads --use-default-rules
```

**Advanced options:**
```bash
# Include / Exclude globs
./neatify --source ~/Downloads --rules rules.properties \
  --include "**/*.pdf" --exclude "**/node_modules/**"

# Collision strategy (rename | skip | overwrite)
./neatify --source ~/Downloads --rules rules.properties --on-collision skip
```

**Help & Version:**
```bash
./neatify --help
./neatify --version
```

**Undo operations:**
```bash
./neatify --source <dir> --undo            # undo last run
./neatify --source <dir> --undo-list       # list journals
./neatify --source <dir> --undo-run <ts>   # undo by timestamp
```

### Shortcuts (Linux / macOS / WSL)

A `Makefile` wraps the common flows (`make` is not available by default on Windows — use
`.\neatify.cmd` there):
```bash
make build                       # ./mvnw package
make run                         # interactive mode
make preview DIR=~/Downloads     # dry-run on DIR
make apply   DIR=~/Downloads     # organize DIR for real
make dev     DIR=~/Downloads     # build, then preview
make help                        # list all targets and variables
```
Variables: `DIR` (folder to tidy), `RULES` (`default` ⇒ built‑in rules, or a `.properties` path),
`ARGS` (extra flags, e.g. `ARGS="--on-collision skip"`).

> No build yet? You can also run without packaging a jar (non‑interactive modes only):
> `./mvnw -q exec:java -Dexec.args="--source ~/Downloads --use-default-rules"`.

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

**Core Protections:**
- **Dry‑run preview by default** – see changes before applying
- **Path traversal protection** – prevents malicious file paths
- **File‑count quota (anti‑DoS)** – limits processing scope
- **Atomic collision handling** – choose between rename/skip/overwrite strategies

**VCS Repository Protection:**
- **Ignore VCS by default** – Neatify skips version-controlled worktrees during scans
  - Detected markers: `.git`, `.hg`, `.svn`, `.bzr`, `_darcs`, `.pijul`, Fossil (`.fslckout`), Repo (`.repo`)
  - Prevents accidental reorganization of project sources
- **Git repository blocking** – `--apply` is blocked when source directory is inside a Git repository
- **Explicit override** – use `--allow-inside-git` to bypass (not recommended)
  - Always preview first and ensure backups before using this flag

> **Tip:** Always preview before applying on important data.

---

## Quality & Build Checks

The `verify` phase runs the full quality gate:

```bash
./mvnw verify
```

| Tool | Purpose | Failure threshold |
|---|---|---|
| **Enforcer** | Requires JDK 21+, Maven 3.8+, dependency convergence | any violation |
| **Surefire + JUnit 5** | Unit tests | any failure |
| **JaCoCo** | Test coverage (`target/site/jacoco/index.html`) | < 55% line coverage |
| **Spotless** | Source hygiene (import order, whitespace) | any unformatted file |
| **SpotBugs** | Static bug detection (effort max) | medium+ priority bug |
| **PMD** | Code smells, dead code | any violation |

Useful commands:

```bash
./mvnw spotless:apply                          # auto-format sources
./mvnw versions:display-dependency-updates    # check for dependency updates
./mvnw verify -Psecurity-scan                  # OWASP dependency CVE scan (set NVD_API_KEY)
```

> **Note:** if the working tree sits on a network share that does not support
> file locking (e.g. `\\wsl.localhost` from Windows), redirect the build
> directory: `./mvnw verify -Dneatify.buildDirectory=C:\tmp\neatify-target`.

---

## License

MIT – see [LICENSE](./LICENSE).
