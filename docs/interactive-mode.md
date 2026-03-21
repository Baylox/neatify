# Mode interactif

Le mode interactif est activé en lançant Neatify sans argument :

```bash
java -jar target/neatify.jar
```

Un banneau s'affiche, suivi du menu principal.

---

## Menu principal

```
╔══════════════════════════════════════════════════╗
║                    NEATIFY 1.0.0                 ║
║          Automatic organization tool             ║
╚══════════════════════════════════════════════════╝

MAIN MENU
═════════════════════════════════════════════════════════════════

  1. Organize files
  2. Create a rules file
  3. Undo last run
  4. Show help
  5. Show version
  6. Quit

> _
```

Saisissez le numéro de l'option souhaitée et appuyez sur Entrée. `q` ou `6` quittent l'application.

---

## Option 1 — Organiser les fichiers

Flux complet guidé par prompts :

### Étape 1 : Dossier source

```
Source directory:
> /home/user/Downloads
```

Neatify valide que le chemin existe et est un dossier. Les répertoires système sont refusés.

### Étape 2 : Règles

```
Rules file (leave empty to use default rules):
> _
```

- Laissez vide pour utiliser les **règles intégrées** (67 extensions)
- Ou saisissez le chemin vers un fichier `.properties` custom

### Étape 3 : Filtres (optionnel)

```
Include patterns (glob, comma-separated, leave empty for all):
> _

Exclude patterns (glob, comma-separated, leave empty for none):
> _
```

Exemples de patterns : `**/*.pdf`, `**/*.jpg,**/*.png`, `**/node_modules/**`

### Étape 4 : Aperçu

Neatify affiche les changements planifiés :

```
═══════════════════════════════════════════════════════════════
                      CHANGES PREVIEW
═══════════════════════════════════════════════════════════════

→ Documents/  (3 files)
  • rapport.pdf
  • notes.txt
  • lettre.docx

→ Images/  (5 files)
  • photo.jpg
  • screenshot.png
  • banner.svg
  • + 2 more...

→ Archives/  (1 file)
  • backup.zip

[########################################] 100% (9/9)
```

### Étape 5 : Confirmation

```
Apply these changes? [y/N]:
> _
```

Répondez `y` pour appliquer, ou Entrée / `n` pour annuler.

### Étape 6 : Stratégie de collision

```
Collision strategy [rename/skip/overwrite] (default: rename):
> _
```

- `rename` — Renomme automatiquement si le fichier destination existe
- `skip` — Ignore les fichiers en conflit
- `overwrite` — Remplace les fichiers existants

### Résultat

```
[OK] Moved:   9
[--] Skipped: 0
[!!] Errors:  0
```

L'opération est journalisée dans `.neatify/runs/` et peut être annulée avec l'option `3`.

---

## Option 2 — Créer un fichier de règles

Génère un fichier `.properties` pré-rempli avec toutes les règles par défaut.

```
Rules file name (default: custom-rules/my-rules.properties):
> _
```

Le fichier est créé dans le répertoire courant, sous `custom-rules/`. Il peut ensuite être modifié selon les besoins.

Voir [Format des règles](rules-format.md) pour la syntaxe complète.

---

## Option 3 — Annuler le dernier run

```
Undoing last run...

[OK] Restored: 9
[--] Skipped:  0
[!!] Errors:   0
```

Inverse exactement les mouvements effectués lors du dernier run journalisé. Si aucun journal n'existe, un message l'indique.

Voir [Système d'annulation](undo-system.md) pour les détails.

---

## Option 4 — Aide

Affiche la même aide que `--help` en mode CLI.

---

## Option 5 — Version

```
Neatify version 1.0.0
```

---

## Quitter

Option `6`, touche `q`, ou `Ctrl+C`.

---

## Comportement sur terminal limité

Si le terminal ne supporte pas Unicode, les symboles de boîte (`╔`, `═`, `→`, `•`) sont remplacés par leurs équivalents ASCII (`+`, `-`, `>`, `*`). Forçable avec `--ascii` en CLI, ou auto-détecté selon l'encodage du terminal.
