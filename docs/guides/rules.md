# Rules

Neatify decides where a file goes by its **extension**. A rule maps an extension
to a destination folder.

## Format

Rules live in a `.properties` file, one mapping per line:

```properties
# extension = DestinationFolder
pdf=Documents
jpg=Images
mp4=Videos
```

- Extensions are matched **case-insensitively** and without the leading dot.
- A destination may contain subfolders, e.g. `xlsx=Documents/Spreadsheets`.
- Lines starting with `#` are comments.

Files without an extension, hidden files, and the `.neatify/` journal are never moved.

## Using rules

```bash
# Built-in defaults (no file needed)
./neatify --source ~/Downloads --use-default-rules

# A custom rules file
./neatify --source ~/Downloads --rules rules.properties
```

On Windows, use `.\neatify.cmd` instead of `./neatify`.

## Built-in default rules

`--use-default-rules` applies the mappings below. The table is generated from the
code, so it always matches what the app actually does.

<!-- AUTOGEN:rules START -->

| Destination folder | Extensions |
|--------------------|------------|
| `Archives` | `7z`, `bz2`, `gz`, `rar`, `tar`, `zip` |
| `Code` | `c`, `cpp`, `cs`, `css`, `go`, `h`, `html`, `java`, `js`, `json`, `php`, `py`, `rb`, `rs`, `ts`, `xml`, `yaml`, `yml` |
| `DiskImages` | `iso` |
| `Documents` | `doc`, `docx`, `md`, `odt`, `pdf`, `rtf`, `txt` |
| `Documents/Presentations` | `odp`, `ppt`, `pptx` |
| `Documents/Spreadsheets` | `csv`, `ods`, `xls`, `xlsx` |
| `Executables` | `deb`, `dmg`, `exe`, `msi`, `pkg`, `rpm` |
| `Images` | `bmp`, `gif`, `ico`, `jpeg`, `jpg`, `png`, `svg`, `webp` |
| `Music` | `aac`, `flac`, `m4a`, `mp3`, `ogg`, `wav` |
| `Torrents` | `torrent` |
| `Videos` | `avi`, `flv`, `mkv`, `mov`, `mp4`, `webm`, `wmv` |

<!-- AUTOGEN:rules END -->

## Validation

Invalid mappings are rejected for safety:

| Issue | Behavior |
|-------|----------|
| Empty extension or folder | Skipped |
| Path traversal (`..`) in the folder | Rejected |
| Absolute path as folder | Rejected |
| Illegal filename characters | Rejected |

See [security](../explanation/security.md) for the full rationale.
