# Undo System

Neatify journals every organization operation in a `.neatify/` directory automatically created inside the source folder. This allows fully or partially undoing any past operation.

---

## `.neatify/` directory structure

```
<source>/
└── .neatify/
    ├── .gitignore          # Excludes journals from Git versioning
    └── runs/
        ├── 1710953471234.json          # Run from 2026-03-20 15:51:11
        ├── 1710953502891.json          # Run from 2026-03-20 15:51:42
        └── 1710953502891_1.json        # Collision (same millisecond)
```

The `.gitignore` contains:
```
*
!.gitignore
```
All journals are excluded from Git versioning, but the directory itself can be committed.

---

## Run journal format

Each `<timestamp>.json` file records the moves performed:

```json
{
  "time": 1710953471234,
  "onCollision": "rename",
  "moves": [
    {
      "from": "/home/user/Downloads/report.pdf",
      "to": "/home/user/Downloads/Documents/report.pdf"
    },
    {
      "from": "/home/user/Downloads/photo.jpg",
      "to": "/home/user/Downloads/Images/photo.jpg"
    }
  ]
}
```

- `time`: Unix timestamp in milliseconds at execution time
- `onCollision`: strategy used during the run
- `moves[].from`: original absolute path of the file
- `moves[].to`: absolute destination path

Only **actually performed moves** are journaled. Skipped or errored files are not recorded.

---

## Undo commands

### Undo the last run

```bash
java -jar target/neatify.jar --source ~/Downloads --undo
```

Finds the most recent run in `.neatify/runs/` and reverses all its moves.

### List journaled runs

```bash
java -jar target/neatify.jar --source ~/Downloads --undo-list
```

Displays available runs:

```
Run history for /home/user/Downloads:

  [1] 2026-03-20 15:51:42  —  9 moves  (rename)   [1710953502891]
  [2] 2026-03-20 15:51:11  —  3 moves  (skip)      [1710953471234]
```

### Undo a specific run

```bash
java -jar target/neatify.jar --source ~/Downloads --undo-run 1710953471234
```

Undoes only the run identified by its timestamp. Other runs are not affected.

---

## Undo logic

For each recorded move `from → to`:

1. Checks that `to` (current destination) exists
2. Checks that `from` (undo destination) does not already exist
3. Checks that both paths stay within the source folder (scope check)
4. Checks for symlinks in parent paths
5. Creates parent directories of `from` if needed
6. Moves `to → from`

On success: the journal file is deleted.

### Undo result

```
[OK] Restored: 9
[--] Skipped:  1    (file already exists at original location)
[!!] Errors:   0
```

| Counter | Meaning |
|---|---|
| `Restored` | Files moved back to their original location |
| `Skipped` | Files ignored (destination absent, or original already present) |
| `Errors` | I/O errors during the move |

Undo is partial on error: remaining moves continue.

---

## Legacy compatibility

Older versions of Neatify used a `manifest.json` file inside `.neatify/`. This format is automatically detected as a fallback if no v2 run exists:

```
.neatify/
└── manifest.json    # Legacy format: {"runs": [{"moves": [...]}]}
```

The fallback is transparent to the user. It is recommended to let Neatify generate v2 journals by running a new operation.

---

## Timestamp collision handling

If two runs complete within the same millisecond (rare, mainly in tests), Neatify appends a numeric suffix:

```
1710953471234.json
1710953471234_1.json
1710953471234_2.json
```

Ordering is determined numerically during `--undo-list` and `--undo`.
