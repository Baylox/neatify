# JSON output

`--json` makes Neatify emit a single JSON document on `stdout` for machine consumption.
On Windows use `.\neatify.cmd`.

- Only the JSON document goes to `stdout` (no human-readable messages).
- Logs and warnings go to `stderr` and the log files.
- With nothing to do, Neatify still emits a valid envelope (`planned: 0`, `actions: []`,
  empty `result`).

```bash
./neatify --source ~/Downloads --rules rules.properties --json
```

## Schema

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

| Field | Meaning |
|-------|---------|
| `source` | The scanned directory |
| `apply` | `true` if files were actually moved, `false` for a dry-run |
| `onCollision` | Collision strategy in effect |
| `planned` | Number of planned moves |
| `actions[]` | Each planned move: `source`, `target`, `reason` |
| `result` | `moved`, `skipped` counts and an `errors` array |

Zero-action envelope:

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

## With jq

```bash
# Count planned moves
./neatify --source ~/Downloads --rules rules.properties --json | jq '.planned'

# First 5 planned moves (source -> target)
./neatify --source ~/Downloads --rules rules.properties --json \
  | jq -r '.actions[] | "\(.source) -> \(.target)"' | head -n 5

# Unique target folders
./neatify --source ~/Downloads --rules rules.properties --json \
  | jq -r '.actions[].target | split("/")[:-1] | join("/")' | sort -u
```
