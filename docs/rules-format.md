# Format des fichiers de règles

Les règles définissent comment Neatify organise les fichiers : pour chaque extension, quel sous-dossier de destination utiliser.

---

## Syntaxe

Un fichier de règles est un fichier `.properties` Java standard :

```properties
extension=DossierDestination
```

- Une règle par ligne
- Les lignes commençant par `#` sont des commentaires
- Les lignes vides sont ignorées
- L'extension est insensible à la casse (`PDF` et `pdf` sont équivalents)
- Le point initial n'est pas inclus : écrire `pdf`, pas `.pdf`

### Sous-dossiers

Le dossier destination peut contenir des slashes pour créer des sous-dossiers :

```properties
xls=Documents/Spreadsheets
pptx=Documents/Presentations
```

---

## Validation

Neatify rejette ou corrige silencieusement les règles invalides :

| Problème | Comportement |
|---|---|
| Extension vide | Règle ignorée |
| Dossier vide | Règle ignorée |
| Path traversal (`..`) | Erreur — règle rejetée |
| Chemin absolu (`/`, `C:\`) | Erreur — règle rejetée |
| Caractères illégaux (`< > : " \ \| ? *`) | Remplacés par `_` automatiquement |
| Extension avec point (`.pdf`) | Le point est retiré automatiquement |

---

## Règles par défaut (`--use-default-rules`)

Ces 67 règles sont intégrées dans le JAR et disponibles via `--use-default-rules` :

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

# Documents texte
pdf=Documents
doc=Documents
docx=Documents
txt=Documents
odt=Documents
rtf=Documents
md=Documents

# Tableurs
xls=Documents/Spreadsheets
xlsx=Documents/Spreadsheets
csv=Documents/Spreadsheets
ods=Documents/Spreadsheets

# Présentations
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

# Vidéos
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

# Code source
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

# Exécutables
exe=Executables
msi=Executables
dmg=Executables
pkg=Executables
deb=Executables
rpm=Executables

# Images disque
iso=Disk_Images

# Torrents
torrent=Torrents
```

---

## Fichier de règles custom

### Créer un fichier manuellement

```properties
# Mon organisation personnelle
pdf=Travail/Documents
docx=Travail/Documents
jpg=Photos
png=Photos
mp4=Medias/Videos
mp3=Medias/Musique
zip=Archives
exe=Logiciels
```

### Créer un fichier via Neatify

En mode interactif (option `2` du menu), ou via CLI à venir. Neatify génère un fichier `.properties` pré-rempli avec toutes les règles par défaut, prêt à être modifié.

---

## Utilisation

```bash
# Avec fichier de règles custom
java -jar target/neatify.jar --source ~/Downloads --rules mon-fichier.properties

# Avec règles intégrées
java -jar target/neatify.jar --source ~/Downloads --use-default-rules
```

---

## Notes importantes

- Les fichiers **sans extension** sont toujours ignorés (ex: `Makefile`, `LICENSE`)
- Les fichiers **cachés** (nom commençant par `.`) sont toujours ignorés
- Le dossier `.neatify/` (journal d'annulation) est toujours exclu du scan
- Si une extension n'a pas de règle, le fichier est laissé en place
- Un fichier déjà dans son dossier cible ne génère pas de mouvement (no-op détecté)
