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

Build:
```bash
git clone <repo-url>
cd neatify
mvn clean package
# Windows (wrapper):
.\mvnw.cmd clean package
# Jar: target/neatify.jar
```

---

## Use

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

# Preview first
java -jar target/neatify.jar --source ~/Downloads --rules my-rules.properties

# Then apply
java -jar target/neatify.jar --source ~/Downloads --rules my-rules.properties --apply
```

---

## Safety

- Dry‑run preview by default
- Path traversal protection
- File‑count quota (anti‑DoS)
- Atomic collision handling (rename/skip/overwrite)

Tip: always preview before applying on important data.

---

## License

MIT – see `LICENSE`.
