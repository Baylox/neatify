# Neatify

Automatic file organization based on simple rules.

[![CI](https://github.com/Baylox/neatify/actions/workflows/ci.yml/badge.svg)](https://github.com/Baylox/neatify/actions/workflows/ci.yml)
[![CodeQL](https://github.com/Baylox/neatify/actions/workflows/codeql.yml/badge.svg)](https://github.com/Baylox/neatify/actions/workflows/codeql.yml)

Neatify is a small Java CLI that tidies a folder by moving files into category folders
(Documents, Images, Videos…) based on their extension. It defaults to a safe **dry-run**
preview, so you always see changes before they happen.

## Quick start

Requirements: **Java 21+** (a JDK). Maven comes from the bundled wrapper.

```bash
git clone https://github.com/Baylox/neatify.git
cd neatify
./mvnw package        # build target/neatify.jar  (.\mvnw.cmd on Windows)
```

Run it with the launcher (`./neatify` on Linux/macOS/WSL, `.\neatify.cmd` on Windows):

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
