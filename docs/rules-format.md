# Rules Format

Rules tell Neatify how to organize files: for each file extension, which destination subfolder to use.

---

## Syntax

A rules file is a standard Java `.properties` file:

```properties
extension=DestinationFolder
```

- One rule per line
- Lines starting with `#` are comments
- Empty lines are ignored
- Extensions are case-insensitive (`PDF` and `pdf` are equivalent)
- No leading dot: write `pdf`, not `.pdf`

### Subfolders

The destination folder can contain slashes to create subfolders:

```properties
xls=Documents/Spreadsheets
pptx=Documents/Presentations
```

---

## Validation

Neatify rejects or silently corrects invalid rules:

| Issue | Behavior |
|---|---|
| Empty extension | Rule ignored |
| Empty folder | Rule ignored |
| Path traversal (`..`) | Error — rule rejected |
| Absolute path (`/`, `C:\`) | Error — rule rejected |
| Illegal characters (`< > : " \ \| ? *`) | Replaced by `_` automatically |
| Extension with dot (`.pdf`) | Dot is stripped automatically |

---

## Default rules (`--use-default-rules`)

These 67 rules are bundled in the JAR and available via `--use-default-rules`:

```properties
# Images
jpg=Images
jpeg=Images
png=Images
gif=Images
bmp=Images
svg=Images
webp=Images
ico=Images

# Text documents
pdf=Documents
doc=Documents
docx=Documents
txt=Documents
odt=Documents
rtf=Documents
md=Documents

# Spreadsheets
xls=Documents/Spreadsheets
xlsx=Documents/Spreadsheets
csv=Documents/Spreadsheets
ods=Documents/Spreadsheets

# Presentations
ppt=Documents/Presentations
pptx=Documents/Presentations
odp=Documents/Presentations

# Archives
zip=Archives
rar=Archives
7z=Archives
tar=Archives
gz=Archives
bz2=Archives

# Videos
mp4=Videos
avi=Videos
mkv=Videos
mov=Videos
wmv=Videos
flv=Videos
webm=Videos

# Audio
mp3=Music
wav=Music
flac=Music
aac=Music
ogg=Music
m4a=Music

# Source code
java=Code
py=Code
js=Code
ts=Code
cpp=Code
c=Code
h=Code
cs=Code
go=Code
rs=Code
php=Code
rb=Code
html=Code
css=Code
json=Code
xml=Code
yaml=Code
yml=Code

# Executables
exe=Executables
msi=Executables
dmg=Executables
pkg=Executables
deb=Executables
rpm=Executables

# Disk images
iso=Disk_Images

# Torrents
torrent=Torrents
```

---

## Custom rules file

### Creating a file manually

```properties
# My personal organization
pdf=Work/Documents
docx=Work/Documents
jpg=Photos
png=Photos
mp4=Media/Videos
mp3=Media/Music
zip=Archives
exe=Software
```

### Creating a file via Neatify

In interactive mode (menu option `2`), Neatify generates a pre-filled `.properties` file with all default rules, ready to edit.

---

## Usage

```bash
# With a custom rules file
java -jar target/neatify.jar --source ~/Downloads --rules my-rules.properties

# With built-in rules
java -jar target/neatify.jar --source ~/Downloads --use-default-rules
```

---

## Important notes

- Files **without an extension** are always ignored (e.g. `Makefile`, `LICENSE`)
- **Hidden files** (name starting with `.`) are always ignored
- The `.neatify/` folder (undo journal) is always excluded from the scan
- If an extension has no matching rule, the file is left in place
- A file already in its target folder does not generate a move (no-op detected)
