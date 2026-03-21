# Référence CLI

```
java -jar target/neatify.jar [OPTIONS]
```

Sans aucun argument, Neatify démarre en **mode interactif**. Avec des arguments, il fonctionne en **mode CLI**.

---

## Comportements par défaut

| Paramètre | Valeur par défaut |
|---|---|
| Mode | Dry-run (aucun fichier déplacé) |
| Stratégie de collision | `rename` (ajoute `_1`, `_2`…) |
| Quota max fichiers | 100 000 |
| Aperçu par dossier | 5 fichiers |
| Tri de l'aperçu | `alpha` (alphabétique) |
| Couleurs ANSI | Activées (auto-détection terminal) |
| Symboles | Unicode (auto-détection encoding) |

---

## Sélection du mode

| Option | Description |
|---|---|
| *(aucun argument)* | Lance le mode interactif |
| `--interactive`, `-i` | Force le mode interactif même avec d'autres flags |
| `--undo` | Annule le dernier run journalisé |
| `--undo-list` | Liste tous les runs journalisés avec leurs métadonnées |
| `--undo-run <timestamp>` | Annule le run identifié par son timestamp (ms Unix) |
| `--help`, `-h` | Affiche l'aide et quitte |
| `--version`, `-v` | Affiche la version et quitte |

---

## Chemins (obligatoires en mode organisation)

| Option | Description |
|---|---|
| `--source <dir>`, `-s <dir>` | **Obligatoire.** Dossier à organiser. |
| `--rules <file>`, `-r <file>` | Fichier de règles `.properties` à utiliser. |
| `--use-default-rules` | Utilise les règles intégrées (67 extensions prédéfinies). Remplace `--rules`. |

`--rules` et `--use-default-rules` sont mutuellement exclusifs. L'un des deux est obligatoire (sauf pour `--undo`).

---

## Exécution

| Option | Description |
|---|---|
| `--apply`, `-a` | Applique les changements. Sans ce flag : dry-run. |
| `--on-collision <mode>` | Stratégie si un fichier destination existe déjà. Voir ci-dessous. |
| `--max-files <n>` | Limite le nombre de fichiers scannés (défaut : 100 000). |

### Stratégies de collision (`--on-collision`)

| Valeur | Comportement |
|---|---|
| `rename` *(défaut)* | Renomme la destination : `file.pdf` → `file_1.pdf`, `file_2.pdf`… (max 1000 tentatives) |
| `skip` | Ignore les fichiers dont la destination existe déjà. Ils ne sont ni déplacés ni comptés comme erreur. |
| `overwrite` | Remplace le fichier destination (opération atomique si le système le permet). |

---

## Filtres

Les filtres utilisent la syntaxe **glob** de Java NIO (`**` pour n'importe quelle profondeur).

| Option | Description |
|---|---|
| `--include <glob>` | N'inclut que les fichiers correspondant au pattern. Répétable. |
| `--exclude <glob>` | Exclut les fichiers correspondant au pattern. Répétable. |

```bash
# Inclure uniquement PDFs et DOCX
--include "**/*.pdf" --include "**/*.docx"

# Exclure le dossier node_modules et les fichiers temporaires
--exclude "**/node_modules/**" --exclude "**/*.tmp"
```

Quand `--include` est spécifié, seuls les fichiers correspondants sont candidats. Les `--exclude` s'appliquent ensuite en soustraction.

---

## Affichage de l'aperçu

| Option | Description |
|---|---|
| `--per-folder-preview <n>` | Nombre maximum de fichiers affichés par dossier dans l'aperçu (défaut : 5). |
| `--sort <mode>` | Ordre de tri de l'aperçu : `alpha` (défaut), `ext` (par extension), `size` (par taille décroissante). |
| `--no-color` | Désactive les couleurs ANSI dans la sortie. |
| `--ascii` | Utilise des symboles ASCII à la place des caractères Unicode (pour terminaux basiques). |

---

## Sortie JSON

| Option | Description |
|---|---|
| `--json` | Émet un objet JSON sur `stdout`. Les logs sont redirigés sur `stderr`. |

Format de la sortie JSON :

```json
{
  "source": "/home/user/Downloads",
  "apply": true,
  "onCollision": "rename",
  "planned": 5,
  "actions": [
    {
      "source": "/home/user/Downloads/rapport.pdf",
      "target": "/home/user/Downloads/Documents/rapport.pdf",
      "reason": "extension: pdf -> Documents"
    }
  ],
  "result": {
    "moved": 5,
    "skipped": 0,
    "errors": []
  }
}
```

En dry-run, `result.moved` correspond au nombre d'actions planifiées ; aucun fichier n'est réellement déplacé.

---

## Logs

| Option | Description |
|---|---|
| `--debug` | Niveau de log DEBUG (très verbeux). |
| `--verbose` | Niveau de log INFO. |
| `--quiet`, `-q` | Niveau de log WARN (minimal). |

Sans ces flags, le niveau par défaut est INFO (configuré dans `logback.xml`).

Les logs sont écrits dans `logs/` :
- `logs/neatify.<date>.log` — logs applicatifs
- `logs/security.<date>.log` — violations de sécurité uniquement

---

## Sécurité

| Option | Description |
|---|---|
| `--allow-inside-git` | Autorise `--apply` à l'intérieur d'un dépôt Git. **Dangereux.** Par défaut, `--apply` est bloqué dans un repo Git pour éviter de réorganiser du code source versionné. |

---

## Exemples

```bash
# Aperçu rapide avec règles par défaut
java -jar target/neatify.jar -s ~/Downloads --use-default-rules

# Appliquer avec règles custom
java -jar target/neatify.jar -s ~/Downloads -r rules.properties --apply

# Filtrer et appliquer (PDFs uniquement, SKIP si collision)
java -jar target/neatify.jar -s ~/Documents -r rules.properties \
  --include "**/*.pdf" --on-collision skip --apply

# Sortie JSON (pour scripts)
java -jar target/neatify.jar -s ~/Downloads --use-default-rules --json 2>/dev/null

# Undo du dernier run
java -jar target/neatify.jar -s ~/Downloads --undo

# Lister tous les runs journalisés
java -jar target/neatify.jar -s ~/Downloads --undo-list

# Annuler un run spécifique
java -jar target/neatify.jar -s ~/Downloads --undo-run 1710953471234

# Mode debug avec aperçu étendu
java -jar target/neatify.jar -s ~/Downloads --use-default-rules \
  --per-folder-preview 20 --sort size --debug

# Dans un repo Git (avec confirmation explicite)
java -jar target/neatify.jar -s ~/project/assets -r rules.properties \
  --apply --allow-inside-git
```
