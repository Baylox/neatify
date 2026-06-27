# Neatify

Automatic file organization based on simple rules.

[![CI](https://github.com/Baylox/neatify/actions/workflows/ci.yml/badge.svg)](https://github.com/Baylox/neatify/actions/workflows/ci.yml)
[![CodeQL](https://github.com/Baylox/neatify/actions/workflows/codeql.yml/badge.svg)](https://github.com/Baylox/neatify/actions/workflows/codeql.yml)
[![Javadoc](https://img.shields.io/badge/javadoc-online-blue)](https://baylox.github.io/neatify/)

Neatify is a small Java CLI that tidies a folder by moving files into category folders
(Documents, Images, Videos…) based on their extension. It defaults to a safe **dry-run**
preview, so you always see changes before they happen.

## Quick start

Requirements: **Java 21+** (a JDK). That's the only thing you need.

### Install (download the release)

Grab the latest `neatify-<version>.jar` from the
[releases page](https://github.com/Baylox/neatify/releases/latest) and run it:

```bash
# Download (replace the version as needed)
curl -LO https://github.com/Baylox/neatify/releases/latest/download/neatify-1.0.0.jar

# Optional: verify the checksum
curl -LO https://github.com/Baylox/neatify/releases/latest/download/neatify-1.0.0.jar.sha256
sha256sum -c neatify-1.0.0.jar.sha256

# Run it
java -jar neatify-1.0.0.jar                                          # interactive menu
java -jar neatify-1.0.0.jar --source ~/Downloads --use-default-rules # preview (dry-run)
```

The jar is self-contained — no install step, just a JDK 21+.

### Build from source

```bash
git clone https://github.com/Baylox/neatify.git
cd neatify
./mvnw package        # build target/neatify.jar  (.\mvnw.cmd on Windows)
```

This also gives you the launchers (`./neatify` on Linux/macOS/WSL, `.\neatify.cmd` on
Windows), which run the built jar so you don't have to type `java -jar` every time:

```bash
./neatify                                                   # interactive menu
./neatify --source ~/Downloads --use-default-rules          # preview (dry-run)
./neatify --source ~/Downloads --use-default-rules --apply  # actually move files
./neatify --source ~/Downloads --undo                       # revert the last run
```

### Shortcuts (Linux / macOS / WSL)

A `Makefile` wraps the common flows (Windows users: use `.\neatify.cmd`):

```bash
make build                    # ./mvnw package
make run                      # interactive mode
make preview DIR=~/Downloads  # dry-run on DIR
make apply   DIR=~/Downloads  # organize DIR for real
make help                     # list all targets and variables
```

## Documentation

Full docs live in [`docs/`](docs/index.md):

| | |
|---|---|
| [Getting started](docs/getting-started.md) | Install, build, first run |
| [Interactive mode](docs/guides/interactive-mode.md) | The menu-driven flow |
| [Rules](docs/guides/rules.md) | Map extensions to folders (+ built-in defaults) |
| [Undo](docs/guides/undo.md) | Reverse a run |
| [CLI reference](docs/reference/cli.md) | Every option (generated from the code) |
| [JSON output](docs/reference/json-output.md) | Machine-readable results |
| [Architecture](docs/explanation/architecture.md) | How it works |
| [Security](docs/explanation/security.md) | Protections and rationale |

## Quality gate

`./mvnw verify` runs the full gate: Enforcer (JDK 21+), JUnit 5 tests, JaCoCo (≥ 55% line
coverage), Spotless, SpotBugs and PMD. Handy commands:

```bash
./mvnw spotless:apply                       # auto-format
./mvnw verify -Psecurity-scan               # OWASP CVE scan (set NVD_API_KEY)
```

> On a network share without file locking (e.g. `\\wsl.localhost` from Windows), redirect
> the build directory: `./mvnw verify -Dneatify.buildDirectory=C:\tmp\neatify-target`.

## License

MIT — see [LICENSE](./LICENSE).
