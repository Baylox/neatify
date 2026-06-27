# Security

Neatify moves files on the user's system, so several protections guard against
accidental or malicious damage. Examples below use `./neatify`; on Windows use
`.\neatify.cmd`.

## 1. Path traversal protection

**Class:** `PathSecurity.validateRelativeSubpath()` — Rule loading, target resolution

Blocks any rule or path containing `..` (directory traversal) or an absolute path
(`/`, `C:\`).

```properties
# Malicious rules — rejected
pdf=../../../etc
java=/usr/bin
```

A `SecurityException` is thrown, the file is skipped, and a `[SECURITY]` warning is logged.

## 2. Symlink resolution

**Class:** `PathSecurity.assertResolvedWithin()` — before every file move (source and
destination) and every undo.

Rather than forbidding any symlink in the path, Neatify resolves the **real**
(symlink-followed) location and checks it still falls **within the trusted root**
(the source directory being organized). This blocks the real attack — a symlink
inside the work area pointing elsewhere — while allowing legitimate system symlinks
that sit *above* the root (e.g. macOS `/var` → `/private/var`, or an usrmerge
`/bin` → `/usr/bin`). Chained symlinks and hidden `..` segments are caught too.

```
~/Downloads/Images/linked → /etc/   ← escapes the root, rejected
```

A `SecurityException` is thrown and the operation is cancelled.

## 3. System directory protection

**Class:** `PathSecurity.validateSourceDir()` — source validation before any operation.

Refuses system directories as a source. The candidate is resolved to its real path
first, so a symlink pointing at a system directory is caught by its target.

**Unix / macOS:** `/etc` `/bin` `/sbin` `/usr/bin` `/usr/sbin` `/sys` `/proc` `/dev`
`/boot` `/root`. (`/var` and `/tmp` are intentionally allowed — they host legitimate
user temp areas.)

**Windows:** `C:\Windows`, `C:\Program Files`, `C:\Program Files (x86)`,
`C:\ProgramData`, `C:\Users\All Users`.

## 4. Git repository protection

**Class:** `FileOrganizationExecutor.enforceGitRepositoryPolicy()` — before any `--apply`.

Detects if the source (or a parent) is a versioned repository:

| Marker | VCS |
|--------|-----|
| `.git` | Git |
| `.hg` | Mercurial |
| `.svn` | Subversion |
| `.bzr` | Bazaar |
| `_darcs` | Darcs |
| `.pijul` | Pijul |
| `.fslckout` | Fossil |
| `.repo` | Android repo tool |

If a marker is found and `--allow-inside-git` is not given, `--apply` is rejected. In
dry-run a warning is shown but the preview continues.

```bash
./neatify --source ~/project/assets -r rules.properties --apply --allow-inside-git
```

Subdirectories containing a VCS marker are also skipped during planning
(`skipGitRepos=true` in `FilePlanner`).

## 5. File count quota (anti-DoS)

**Class:** `FilePlanner.plan()` — during traversal.

Caps the scan at **100,000 files** by default (`--max-files <n>` to change). Prevents
accidentally scanning entire partitions or mounted archives.

## 6. Atomic operations (TOCTOU prevention)

**Class:** `RulesFileCreator`, `UndoExecutor.appendRun()`.

File creation uses `StandardOpenOption.CREATE_NEW` (`O_CREAT | O_EXCL`), which fails
atomically if the file already exists — closing TOCTOU race windows. Journals are
created the same way, with a retry loop on timestamp collisions.

## 7. Collision strategies

**Class:** `FileMover.CollisionStrategy` (enum on the `FileMover` contract) — on every
move where the destination exists.

| Strategy | Behavior |
|----------|----------|
| `RENAME` (default) | Adds a numeric suffix, up to 1000 attempts |
| `SKIP` | Ignores the file without error |
| `OVERWRITE` | `ATOMIC_MOVE` if possible, otherwise `REPLACE_EXISTING` |

The default `RENAME` never overwrites a destination file.

## 8. Undo scope validation

**Class:** `UndoExecutor` — on every undo move.

Verifies that the `from` and `to` paths recorded in a journal stay **inside the
current source directory** (via `assertResolvedWithin`). A move pointing outside the
scope (corrupted or relocated journal) is skipped with an error.

## 9. Folder name sanitization

**Class:** `Rules.sanitizeFolderName()` — on every rules load.

Illegal characters `< > : " \ | ? *` are replaced by `_`. Slashes `/` are preserved
to allow subfolders (`Documents/Spreadsheets`); spaces are preserved.

## 10. Target path containment

**Class:** `PathSecurity.safeResolveWithin()`, `FilePlanner` — for every planned file.

After resolving a target path, Neatify verifies it starts with the normalized source
path. Even if sanitization missed a pattern, this final check prevents any move
outside the source directory.

## 11. JSON mode output isolation

**Class:** `Neatify.main()`, `logback.xml` — under `--json`.

An MDC flag (`jsonMode=true`) is set before execution; a Logback `TurboFilter`
suppresses console logs so only structured JSON reaches `stdout` (logs go to `stderr`
and files). The flag is always cleared in a `finally` block.

## 12. Security violation logging

Violations are logged with the SLF4J `SECURITY` marker to a separate
`logs/security.<date>.log`, enabling independent auditing.

## 13. Hidden and extension-less files ignored

**Class:** `FilePlanner` — for every file.

Files starting with `.` (`.gitconfig`, `.env`…) and files without an extension
(`Makefile`, `LICENSE`…) are ignored, reducing the risk of moving system or
configuration files.
