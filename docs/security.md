# Sécurité

Neatify manipule des fichiers sur le système de l'utilisateur. Plusieurs protections sont en place pour éviter les dommages accidentels ou malveillants.

---

## 1. Protection contre le path traversal

**Classe :** `PathSecurity.validateRelativeSubpath()`
**Déclenché par :** Chargement des règles, résolution des chemins cibles

Bloque toute règle ou chemin contenant `..` (montée dans l'arborescence) ou un chemin absolu (`/`, `C:\`).

```properties
# Règle malveillante — rejetée
pdf=../../../etc
java=/usr/bin
```

**Comportement :** `SecurityException` levée, le fichier est ignoré. Un warning `[SECURITY]` est loggué.

---

## 2. Protection contre les symlinks

**Classe :** `PathSecurity.assertNoSymlinkInAncestry()`
**Déclenché par :** Avant chaque déplacement (source et destination), avant chaque annulation

Vérifie que le chemin lui-même **et tous ses parents** ne sont pas des liens symboliques. Un attaquant ne peut pas rediriger silencieusement une opération vers un emplacement arbitraire en créant un symlink intermédiaire.

```
/data/linked → /etc/   ← symlink malveillant
```

Si `/data/linked` est un symlink, toute opération sur ses enfants est refusée.

**Comportement :** `SecurityException` levée, l'opération est annulée.

> Note : Ce contrôle est désactivé sur Windows (les symlinks y nécessitent des droits administrateur et sont rares dans ce contexte). Le test JUnit associé est annoté `@DisabledOnOs(OS.WINDOWS)`.

---

## 3. Protection des répertoires système

**Classe :** `PathSecurity.validateSourceDir()`
**Déclenché par :** Validation du dossier source avant toute opération

Bloque l'utilisation de répertoires système comme source :

**Unix / macOS :**
```
/etc  /bin  /sbin  /usr/bin  /usr/sbin
/var  /sys  /proc  /dev  /boot  /root
```

**Windows :**
```
C:\Windows
C:\Program Files
C:\Program Files (x86)
C:\ProgramData
C:\Users\All Users
```

**Comportement :** `SecurityException` levée avant tout scan.

---

## 4. Protection des dépôts Git

**Classe :** `FileOrganizationExecutor.enforceGitRepositoryPolicy()`
**Déclenché par :** Avant toute exécution avec `--apply`

Détecte si le dossier source (ou un de ses parents) est un dépôt de code versionné :

| Marker détecté | VCS |
|---|---|
| `.git` | Git |
| `.hg` | Mercurial |
| `.svn` | Subversion |
| `.bzr` | Bazaar |
| `_darcs` | Darcs |
| `.pijul` | Pijul |
| `.fslckout` | Fossil |
| `.repo` | Android repo tool |

Si un marker est trouvé et que `--allow-inside-git` n'est pas spécifié, `--apply` est refusé avec un message explicite.

En dry-run, un **avertissement** est affiché mais l'opération continue.

**Contournement explicite :**
```bash
java -jar target/neatify.jar --source ~/project/assets -r rules.properties \
  --apply --allow-inside-git
```

Durant le planning, les sous-dossiers contenant un marker VCS sont aussi ignorés par défaut (via `skipGitRepos=true` dans `FilePlanner`).

---

## 5. Quota de fichiers (anti-DoS)

**Classe :** `FilePlanner.plan()`
**Déclenché par :** Durant le parcours de l'arborescence

Limite le nombre de fichiers scannés à **100 000 par défaut**. Si ce seuil est dépassé, une `IllegalStateException` est levée et le scan s'arrête.

Configurable avec `--max-files <n>`.

**But :** Éviter des scans involontaires de partitions entières ou d'archives montées.

---

## 6. Opérations atomiques (anti-TOCTOU)

**Classe :** `RulesFileCreator`
**Déclenché par :** Création d'un fichier de règles

La création de fichier utilise `StandardOpenOption.CREATE_NEW` (appel système `O_CREAT | O_EXCL`), qui échoue atomiquement si le fichier existe déjà. Cela évite les race conditions de type TOCTOU (Time-Of-Check Time-Of-Use).

De même, `UndoExecutor.appendRun()` crée les journaux avec `CREATE_NEW`, avec une boucle de retry sur collision de timestamp.

---

## 7. Stratégies de collision de fichiers

**Classe :** `FileMover.CollisionStrategy`
**Déclenché par :** À chaque déplacement si la destination existe

| Stratégie | Comportement |
|---|---|
| `RENAME` | Génère `file_1.pdf`, `file_2.pdf`… jusqu'à 1000 tentatives |
| `SKIP` | Ignore le fichier sans erreur |
| `OVERWRITE` | Remplace avec `ATOMIC_MOVE` si possible, sinon `REPLACE_EXISTING` |

Garantit qu'aucun fichier destination n'est écrasé accidentellement avec la stratégie par défaut `RENAME`.

---

## 8. Validation du scope dans l'annulation

**Classe :** `UndoExecutor.undoRunFile()`
**Déclenché par :** À chaque mouvement d'annulation

Vérifie que les chemins `from` et `to` enregistrés dans le journal sont bien **à l'intérieur du dossier source courant**. Si un chemin pointe hors du scope (journal corrompu ou déplacé), le mouvement est ignoré avec un message d'erreur.

---

## 9. Sanitisation des noms de dossiers

**Classe :** `Rules.sanitizeFolderName()`
**Déclenché par :** Chargement de tout fichier de règles

Les caractères illégaux dans les noms de dossiers sont remplacés par `_` :

```
< > : " \ | ? *
```

Les slashes `/` sont conservés pour permettre les sous-dossiers (`Documents/Spreadsheets`). Les espaces sont conservés.

---

## 10. Vérification que la destination reste dans la source

**Classe :** `PathSecurity.safeResolveWithin()`, `FilePlanner.planFor()`
**Déclenché par :** Pour chaque fichier planifié

Après résolution du chemin cible, Neatify vérifie explicitement que le chemin résolu commence bien par le chemin source normalisé. Même si la sanitisation a échoué à bloquer un pattern, cette vérification finale empêche tout déplacement hors du dossier source.

---

## 11. Isolation du mode JSON

**Classe :** `Neatify.main()`, `logback.xml`
**Déclenché par :** `--json`

En mode JSON, un flag MDC (`jsonMode=true`) est positionné avant toute exécution. Un `TurboFilter` Logback supprime tous les messages de log sur la console. Seul le JSON structuré est émis sur `stdout`. Les logs sont redirigés vers `stderr` et les fichiers.

Le flag MDC est **toujours nettoyé** dans un bloc `finally`, même en cas d'exception.

---

## 12. Logging des violations de sécurité

**Classe :** `FilePlanner`, `UndoExecutor`
**Déclenché par :** Toute détection de violation

Les violations de sécurité sont loggées avec le marker SLF4J `SECURITY`, dans un fichier séparé `logs/security.<date>.log` (configuré dans `logback.xml`). Cela permet un audit indépendant des événements de sécurité.

---

## 13. Fichiers cachés et sans extension ignorés

**Classe :** `FilePlanner.planFor()`
**Déclenché par :** Pour chaque fichier lors du scan

- Les fichiers dont le nom commence par `.` sont ignorés (`.gitconfig`, `.env`…)
- Les fichiers sans extension sont ignorés (`Makefile`, `LICENSE`, `README`…)

Ces exclusions réduisent le risque de déplacer accidentellement des fichiers de configuration système.
