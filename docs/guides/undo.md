# Undo

Neatify journals every organization run in a `.neatify/` directory created inside the
source folder, so any past run can be reversed. Examples use `./neatify`; on Windows
use `.\neatify.cmd`.

## `.neatify/` layout

```
<source>/
└── .neatify/
    ├── .gitignore          # excludes journals from Git
    └── runs/
        ├── 1710953471234.json      # one file per run (Unix ms timestamp)
        ├── 1710953502891.json
        └── 1710953502891_1.json    # same-millisecond collision
```

The `.gitignore` ignores everything but itself, so the folder can be committed while
the journals stay out of Git.

## Run journal format

Each `<timestamp>.json` records the moves that were actually performed:

```json
{
  "time": 1710953471234,
  "onCollision": "rename",
  "moves": [
    { "from": "/home/user/Downloads/report.pdf", "to": "/home/user/Downloads/Documents/report.pdf" },
    { "from": "/home/user/Downloads/photo.jpg",  "to": "/home/user/Downloads/Images/photo.jpg" }
  ]
}
```

Only moves that succeeded are journaled; skipped or errored files are not.

## Commands

```bash
# Undo the most recent run
./neatify --source ~/Downloads --undo

# List journaled runs
./neatify --source ~/Downloads --undo-list

# Undo a specific run by timestamp
./neatify --source ~/Downloads --undo-run 1710953471234
```

`--undo-list` shows something like:

```
Run history for /home/user/Downloads:
  [1] 2026-03-20 15:51:42  —  9 moves  (rename)  [1710953502891]
  [2] 2026-03-20 15:51:11  —  3 moves  (skip)    [1710953471234]
```

## How undo works

For each recorded move `from → to`:

1. `to` (current location) must exist.
2. `from` (original location) must not already exist.
3. Both paths must stay within the source folder (scope check).
4. Both paths are validated with `assertResolvedWithin` (symlink-safe).
5. Parent directories of `from` are created if needed.
6. `to` is moved back to `from`.

On success the journal file is deleted. Undo is partial on error — remaining moves
continue. The result reports `Restored`, `Skipped` and `Errors` counts.

## Legacy journals

Older versions wrote a single `.neatify/manifest.json`
(`{"runs":[{"moves":[...]}]}`). Neatify falls back to it automatically when no
per-run journal exists; running a new operation produces the current format.
