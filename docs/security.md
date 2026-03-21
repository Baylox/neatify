# Security

Neatify manipulates files on the user's system. Several protections are in place to prevent accidental or malicious damage.

---

## 1. Path traversal protection

**Class:** `PathSecurity.validateRelativeSubpath()`
**Triggered by:** Rule loading, target path resolution

Blocks any rule or path containing `..` (directory traversal) or an absolute path (`/`, `C:\`).

```properties
# Malicious rule — rejected
pdf=../../../etc
java=/usr/bin
```

**Behavior:** `SecurityException` thrown, the file is skipped. A `[SECURITY]` warning is logged.

---

## 2. Symlink attack prevention

**Class:** `PathSecurity.assertNoSymlinkInAncestry()`
**Triggered by:** Before every file move (source and destination), before every undo

Verifies that the path itself **and all its parents** are not symbolic links. An attacker cannot silently redirect an operation to an arbitrary location by creating an intermediate symlink.

```
/data/linked → /etc/   ← malicious symlink
```

If `/data/linked` is a symlink, any operation on its children is rejected.

**Behavior:** `SecurityException` thrown, the operation is cancelled.

> Note: This check is disabled on Windows (symlinks require admin rights there and are uncommon in this context). The associated JUnit test is annotated `@DisabledOnOs(OS.WINDOWS)`.

---

## 3. System directory protection

**Class:** `PathSecurity.validateSourceDir()`
**Triggered by:** Source directory validation before any operation

Blocks the use of system directories as source:

**Unix / macOS:**
```
/etc  /bin  /sbin  /usr/bin  /usr/sbin
/var  /sys  /proc  /dev  /boot  /root
```

**Windows:**
```
C:\Windows
C:\Program Files
C:\Program Files (x86)
C:\ProgramData
C:\Users\All Users
```

**Behavior:** `SecurityException` thrown before any scan.

---

## 4. Git repository protection

**Class:** `FileOrganizationExecutor.enforceGitRepositoryPolicy()`
**Triggered by:** Before any execution with `--apply`

Detects if the source directory (or one of its parents) is a versioned code repository:

| Detected marker | VCS |
|---|---|
| `.git` | Git |
| `.hg` | Mercurial |
| `.svn` | Subversion |
| `.bzr` | Bazaar |
| `_darcs` | Darcs |
| `.pijul` | Pijul |
| `.fslckout` | Fossil |
| `.repo` | Android repo tool |

If a marker is found and `--allow-inside-git` is not specified, `--apply` is rejected with an explicit message.

In dry-run, a **warning** is displayed but the operation continues.

**Explicit override:**
```bash
java -jar target/neatify.jar --source ~/project/assets -r rules.properties \
  --apply --allow-inside-git
```

During planning, subdirectories containing a VCS marker are also skipped by default (`skipGitRepos=true` in `FilePlanner`).

---

## 5. File count quota (anti-DoS)

**Class:** `FilePlanner.plan()`
**Triggered by:** During directory tree traversal

Limits the number of scanned files to **100,000 by default**. If this threshold is exceeded, an `IllegalStateException` is thrown and the scan stops.

Configurable with `--max-files <n>`.

**Purpose:** Prevents accidental scans of entire partitions or mounted archives.

---

## 6. Atomic operations (TOCTOU prevention)

**Class:** `RulesFileCreator`
**Triggered by:** Rules file creation

File creation uses `StandardOpenOption.CREATE_NEW` (syscall `O_CREAT | O_EXCL`), which fails atomically if the file already exists. This prevents TOCTOU (Time-Of-Check Time-Of-Use) race conditions.

Similarly, `UndoExecutor.appendRun()` creates journals with `CREATE_NEW`, with a retry loop on timestamp collisions.

---

## 7. File collision strategies

**Class:** `FileMover.CollisionStrategy`
**Triggered by:** On every move when the destination already exists

| Strategy | Behavior |
|---|---|
| `RENAME` | Generates `file_1.pdf`, `file_2.pdf`… up to 1000 attempts |
| `SKIP` | Ignores the file without error |
| `OVERWRITE` | Replaces with `ATOMIC_MOVE` if possible, otherwise `REPLACE_EXISTING` |

Ensures no destination file is accidentally overwritten with the default `RENAME` strategy.

---

## 8. Undo scope validation

**Class:** `UndoExecutor.undoRunFile()`
**Triggered by:** On every undo move

Verifies that `from` and `to` paths recorded in the journal are **inside the current source directory**. If a path points outside the scope (corrupted or relocated journal), the move is skipped with an error message.

---

## 9. Folder name sanitization

**Class:** `Rules.sanitizeFolderName()`
**Triggered by:** On every rules file load

Illegal characters in folder names are replaced by `_`:

```
< > : " \ | ? *
```

Slashes `/` are preserved to allow subfolders (`Documents/Spreadsheets`). Spaces are preserved.

---

## 10. Target path containment check

**Class:** `PathSecurity.safeResolveWithin()`, `FilePlanner.planFor()`
**Triggered by:** For every planned file

After resolving the target path, Neatify explicitly verifies that the resolved path starts with the normalized source path. Even if sanitization failed to catch a pattern, this final check prevents any move outside the source directory.

---

## 11. JSON mode output isolation

**Class:** `Neatify.main()`, `logback.xml`
**Triggered by:** `--json`

In JSON mode, an MDC flag (`jsonMode=true`) is set before any execution. A Logback `TurboFilter` suppresses all log messages from the console. Only structured JSON is emitted on `stdout`. Logs are redirected to `stderr` and log files.

The MDC flag is **always cleaned up** in a `finally` block, even on exception.

---

## 12. Security violation logging

**Class:** `FilePlanner`, `UndoExecutor`
**Triggered by:** Any detected violation

Security violations are logged with the SLF4J `SECURITY` marker, to a separate file `logs/security.<date>.log` (configured in `logback.xml`). This enables independent auditing of security events.

---

## 13. Hidden and extension-less files ignored

**Class:** `FilePlanner.planFor()`
**Triggered by:** For every file during scan

- Files whose name starts with `.` are ignored (`.gitconfig`, `.env`…)
- Files without an extension are ignored (`Makefile`, `LICENSE`, `README`…)

These exclusions reduce the risk of accidentally moving system or configuration files.
