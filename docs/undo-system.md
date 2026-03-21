# Système d'annulation

Neatify journalise chaque opération d'organisation dans un répertoire `.neatify/` créé automatiquement dans le dossier source. Cela permet d'annuler intégralement ou partiellement toute opération passée.

---

## Structure du répertoire `.neatify/`

```
<source>/
└── .neatify/
    ├── .gitignore          # Exclut les journaux du versioning Git
    └── runs/
        ├── 1710953471234.json          # Run du 20/03/2026 15:51:11
        ├── 1710953502891.json          # Run du 20/03/2026 15:51:42
        └── 1710953502891_1.json        # Collision (même milliseconde)
```

Le `.gitignore` contient :
```
*
!.gitignore
```
Tous les journaux sont exclus du versioning Git, mais le répertoire lui-même peut être commité.

---

## Format d'un journal de run

Chaque fichier `<timestamp>.json` enregistre les mouvements effectués :

```json
{
  "time": 1710953471234,
  "onCollision": "rename",
  "moves": [
    {
      "from": "/home/user/Downloads/rapport.pdf",
      "to": "/home/user/Downloads/Documents/rapport.pdf"
    },
    {
      "from": "/home/user/Downloads/photo.jpg",
      "to": "/home/user/Downloads/Images/photo.jpg"
    }
  ]
}
```

- `time` : timestamp Unix en millisecondes du moment de l'exécution
- `onCollision` : stratégie utilisée lors du run
- `moves[].from` : chemin absolu original du fichier
- `moves[].to` : chemin absolu de destination

Seuls les **mouvements réellement effectués** sont journalisés. Les fichiers skippés ou en erreur n'apparaissent pas.

---

## Commandes d'annulation

### Annuler le dernier run

```bash
java -jar target/neatify.jar --source ~/Downloads --undo
```

Cherche le run le plus récent dans `.neatify/runs/` et inverse tous ses mouvements.

### Lister les runs journalisés

```bash
java -jar target/neatify.jar --source ~/Downloads --undo-list
```

Affiche la liste des runs disponibles :

```
Run history for /home/user/Downloads:

  [1] 2026-03-20 15:51:42  —  9 moves  (rename)   [1710953502891]
  [2] 2026-03-20 15:51:11  —  3 moves  (skip)      [1710953471234]
```

### Annuler un run spécifique

```bash
java -jar target/neatify.jar --source ~/Downloads --undo-run 1710953471234
```

Annule uniquement le run identifié par son timestamp. Les autres runs ne sont pas affectés.

---

## Logique d'annulation

Pour chaque mouvement `from → to` enregistré :

1. Vérifie que `to` (destination actuelle) existe
2. Vérifie que `from` (destination d'undo) n'existe pas déjà
3. Vérifie que les deux chemins restent dans le dossier source (scope check)
4. Vérifie l'absence de symlinks dans les chemins parents
5. Crée les répertoires parents de `from` si nécessaire
6. Déplace `to → from`

Après succès : le fichier journal est supprimé.

### Résultat d'une annulation

```
[OK] Restored: 9
[--] Skipped:  1    (file already exists at original location)
[!!] Errors:   0
```

| Compteur | Signification |
|---|---|
| `Restored` | Fichiers replacés à leur emplacement original |
| `Skipped` | Fichiers ignorés (destination absente, ou original déjà présent) |
| `Errors` | Erreurs I/O pendant le déplacement |

L'annulation est partielle en cas d'erreur : les autres mouvements continuent.

---

## Compatibilité legacy

Les versions antérieures de Neatify utilisaient un fichier `manifest.json` dans `.neatify/`. Ce format est automatiquement détecté en fallback si aucun run v2 n'existe :

```
.neatify/
└── manifest.json    # Format legacy : {"runs": [{"moves": [...]}]}
```

Le fallback est transparent pour l'utilisateur. Il est recommandé de migrer en réexécutant Neatify pour générer des journaux au format v2.

---

## Gestion des collisions de timestamp

Si deux runs se terminent dans la même milliseconde (rare mais possible en test), Neatify utilise un suffixe numérique :

```
1710953471234.json
1710953471234_1.json
1710953471234_2.json
```

L'ordre est déterminé numériquement lors de `--undo-list` et `--undo`.
