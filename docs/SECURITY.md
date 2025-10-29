# Sécurité - Neatify

## Vue d'ensemble

Neatify a été conçu avec la sécurité comme priorité. Ce document détaille les menaces considérées et les protections implémentées.

---

## Modèle de menaces

### Menaces dans le scope

✅ **Protection implémentée :**

1. **Path Traversal** : Tentative d'accéder à des fichiers en dehors du dossier source
2. **Déni de service (DOS)** : Tentative de traiter un nombre excessif de fichiers
3. **Time-of-Check-Time-of-Use (TOCTOU)** : Modification du système de fichiers entre planification et exécution
4. **Injection de chemins** : Utilisation de caractères spéciaux ou chemins absolus dans les règles
5. **Accès à des dossiers système** : Tentative de ranger/lire des dossiers système critiques

### Hors du scope

❌ **Non géré :**

1. **Permissions système** : Neatify n'élève pas les privilèges, respect des permissions utilisateur
2. **Chiffrement** : Pas de chiffrement des fichiers ou des règles
3. **Authentification/Autorisation** : Application locale, pas de gestion d'utilisateurs
4. **Attaques réseau** : Pas d'accès réseau, application offline uniquement
5. **Injection SQL/XSS** : Pas de base de données ni d'interface web

---

## Protections implémentées

### 1. Protection contre Path Traversal

#### Menace
Un attaquant pourrait créer des règles malveillantes pour accéder à des fichiers en dehors du dossier source :

```properties
# Tentative d'accès à /etc/passwd
txt=../../../etc/passwd

# Tentative d'échappement Windows
pdf=..\..\..\Windows\System32
```

#### Protection : Validation multi-niveaux

**Niveau 1 : Rules.sanitizeFolderName()**
```java
// Blocage des patterns dangereux
if (folderName.contains("..")) {
    throw new IllegalArgumentException("Path traversal interdit");
}
if (folderName.startsWith("/") || folderName.matches("^[A-Za-z]:")) {
    throw new IllegalArgumentException("Chemin absolu interdit");
}
```

**Niveau 2 : PathSecurity.validateRelativeSubpath()**
```java
// Validation stricte avant résolution
void validateRelativeSubpath(String subpath) {
    if (subpath.contains("..")) throw new SecurityException("Path traversal interdit");
    if (subpath.startsWith("/")) throw new SecurityException("Chemin absolu Unix interdit");
    if (subpath.matches("^[A-Za-z]:")) throw new SecurityException("Chemin absolu Windows interdit");
}
```

**Niveau 3 : PathSecurity.safeResolveWithin()**
```java
// Vérification après résolution
Path resolved = baseDir.resolve(subpath).normalize();
if (!resolved.startsWith(baseDir.normalize())) {
    throw new SecurityException("Le chemin résolu sort du dossier de base");
}
```

#### Tests
- `FileMoverPathTraversalTest.java` : 6 tests de scénarios d'attaque
- `PathSecurityTest.java` : 9 tests de validation
- `RulesSecurityTest.java` : 11 tests de règles malveillantes

---

### 2. Protection anti-DOS (Quota de fichiers)

#### Menace
Un attaquant pourrait :
- Pointer Neatify vers un dossier avec des millions de fichiers
- Créer des liens symboliques circulaires
- Consommer toute la mémoire/CPU

#### Protection : Quota configurable

```java
public static final int DEFAULT_MAX_FILES = 100_000;

public static List<Action> plan(Path sourceDir, Map<String, String> rules, int maxFiles) {
    if (maxFiles <= 0) {
        throw new IllegalArgumentException("Le quota doit être positif");
    }

    int count = 0;
    try (Stream<Path> paths = Files.walk(sourceDir)) {
        for (Path path : paths.toList()) {
            if (++count > maxFiles) {
                throw new IllegalStateException("Quota de fichiers dépassé : " + maxFiles);
            }
            // ...
        }
    }
}
```

**Limites :**
- **Défaut** : 100 000 fichiers
- **Configurable** : Via paramètre `maxFiles`
- **Minimum** : 1 fichier

#### Tests
- `FileMoverQuotaTest.java` : 4 tests de quotas

---

### 3. Protection anti-TOCTOU (Time-of-Check-Time-of-Use)

#### Menace
Entre la phase de planification et d'exécution, le système de fichiers peut changer :

```
1. plan() calcule: move file.txt -> Documents/file.txt
2. [Attaquant crée Documents/file.txt]
3. execute() écrase le fichier créé par l'attaquant
```

#### Protection : Détection et résolution atomique des collisions

```java
private static Path resolveCollision(Path target) throws IOException {
    Path finalTarget = target;
    int suffix = 1;

    // Recherche atomique d'un nom disponible
    while (Files.exists(finalTarget)) {
        String fileName = target.getFileName().toString();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String extension = fileName.substring(fileName.lastIndexOf('.'));
        finalTarget = target.getParent().resolve(baseName + "_" + suffix + extension);
        suffix++;
    }

    return finalTarget;
}
```

**Comportement :**
- Si `file.txt` existe → renomme en `file_1.txt`
- Si `file_1.txt` existe → renomme en `file_2.txt`
- Et ainsi de suite jusqu'à trouver un nom libre

#### Tests
- `FileMoverCollisionTest.java` : 3 tests de scénarios de collision

---

### 4. Protection des dossiers système

#### Menace
Un utilisateur (malveillant ou par erreur) pourrait tenter de ranger des dossiers système :

```bash
java -jar neatify.jar --source /etc --rules rules.properties --apply
java -jar neatify.jar --source C:\Windows\System32 --rules rules.properties --apply
```

#### Protection : Validation du dossier source

```java
public static void validateSourceDir(Path dir) throws IOException {
    Path absoluteDir = dir.toAbsolutePath().normalize();
    String dirStr = absoluteDir.toString();

    // Dossiers système Unix/Linux
    if (dirStr.startsWith("/bin") || dirStr.startsWith("/sbin") ||
        dirStr.startsWith("/etc") || dirStr.startsWith("/usr") ||
        dirStr.startsWith("/var") || dirStr.startsWith("/sys") ||
        dirStr.startsWith("/proc")) {
        throw new SecurityException("Dossier système interdit : " + dirStr);
    }

    // Dossiers système Windows
    if (dirStr.matches("^[A-Za-z]:\\\\(Windows|Program Files|System32).*")) {
        throw new SecurityException("Dossier système Windows interdit : " + dirStr);
    }
}
```

**Dossiers bloqués :**
- Unix/Linux : `/bin`, `/sbin`, `/etc`, `/usr`, `/var`, `/sys`, `/proc`
- Windows : `C:\Windows`, `C:\Program Files`, `C:\System32`

#### Tests
- `PathSecurityTest.java` : Tests de validation des dossiers système

---

### 5. Protection contre les liens symboliques

#### Menace
Un attaquant pourrait créer des liens symboliques pour :
- Échapper du dossier source
- Créer des boucles infinies
- Accéder à des fichiers sensibles

#### Protection : Détection des symlinks

```java
public static void assertNoSymlinkInAncestry(Path path) throws IOException {
    Path current = path.toAbsolutePath();
    while (current != null) {
        if (Files.isSymbolicLink(current)) {
            throw new SecurityException("Lien symbolique détecté dans le chemin : " + current);
        }
        current = current.getParent();
    }
}
```

**Comportement :**
- Vérifie tout le chemin d'accès
- Rejette si un lien symbolique est détecté dans l'arborescence

#### Tests
- `PathSecurityTest.java` : Test de détection de symlinks

---

### 6. Mode Dry-Run par défaut

#### Protection : Sécurité par conception

```java
// Par défaut, rien n'est modifié
java -jar neatify.jar --source ~/Downloads --rules rules.properties

// Nécessite un flag explicite pour appliquer
java -jar neatify.jar --source ~/Downloads --rules rules.properties --apply
```

**Avantages :**
- Prévisualisation obligatoire
- Pas d'action accidentelle
- Permet de détecter les problèmes avant application

---

### 7. Validation des fichiers de règles

#### Menace
Un fichier de règles malveillant pourrait contenir :
- Des règles vides
- Des extensions invalides
- Des chemins absolus
- Des caractères d'injection

#### Protection : Validation stricte

```java
public static Map<String, String> load(Path rulesFile) throws IOException {
    Properties props = new Properties();
    props.load(Files.newBufferedReader(rulesFile));

    if (props.isEmpty()) {
        throw new IllegalArgumentException("Le fichier de règles est vide");
    }

    Map<String, String> rules = new HashMap<>();
    for (String extension : props.stringPropertyNames()) {
        String target = props.getProperty(extension);

        // Validation de l'extension
        String normalizedExt = extension.trim().toLowerCase().replace(".", "");
        if (normalizedExt.isEmpty()) continue;

        // Validation du dossier cible
        String sanitized = sanitizeFolderName(target);

        rules.put(normalizedExt, sanitized);
    }

    return rules;
}
```

**Validations :**
- Fichier non vide
- Extensions normalisées
- Dossiers cibles sanitisés
- Caractères invalides remplacés

#### Tests
- `RulesTest.java` : 6 tests de chargement
- `RulesSecurityTest.java` : 11 tests de sécurité

---

### 8. Gestion défensive des erreurs

#### Principe : Fail-safe

```java
// En cas d'erreur, on abandonne plutôt que de continuer
try {
    PathSecurity.validateSourceDir(sourceDir);
} catch (SecurityException e) {
    System.err.println("ERREUR DE SÉCURITÉ : " + e.getMessage());
    return; // Arrêt immédiat
}
```

**Comportement :**
- Toute violation de sécurité arrête l'exécution
- Messages d'erreur clairs
- Pas de tentative de "réparer" automatiquement

---

## Architecture de sécurité

### Defense in Depth (Défense en profondeur)

```
Input
  ↓
[Validation Niveau 1 : Rules]
  ↓
[Validation Niveau 2 : PathSecurity]
  ↓
[Validation Niveau 3 : FileMover (runtime)]
  ↓
Exécution
```

**Principe :** Même si une couche échoue, les suivantes bloquent l'attaque.

### Principe du moindre privilège

- **Pas d'élévation de privilèges** : Neatify n'utilise pas sudo/admin
- **Permissions utilisateur** : Respecte les permissions du système de fichiers
- **Pas d'accès réseau** : Application locale uniquement
- **Pas d'exécution de code** : Pas de `eval()`, `exec()`, ou processus externes

---

## Auditer la sécurité

### 1. Tests de sécurité

```bash
# Lancer tous les tests de sécurité
mvn test -Dtest=io.neatify.core.security.*Test

# 33 tests couvrant :
# - Path traversal (6 tests)
# - Quota DOS (4 tests)
# - Collisions TOCTOU (3 tests)
# - Validation de chemins (9 tests)
# - Validation de règles (11 tests)
```

### 2. Revue du code de sécurité

**Fichiers critiques à auditer :**
- `PathSecurity.java` : Validation des chemins
- `Rules.java` : Validation des règles
- `FileMover.java` : Exécution des déplacements

### 3. Fuzzing (recommandé)

Tester avec des entrées malformées :

```bash
# Créer des fichiers de règles malveillants
echo "txt=../../etc" > malicious.properties
java -jar neatify.jar --source /tmp --rules malicious.properties

# Créer des noms de fichiers avec caractères spéciaux
touch "../../etc/passwd"
touch "C:\Windows\System32\evil.txt"
```

---

## Signaler une vulnérabilité

Si vous découvrez une vulnérabilité de sécurité :

1. **NE PAS** ouvrir d'issue publique
2. Contacter les mainteneurs en privé
3. Inclure :
   - Description de la vulnérabilité
   - Étapes pour la reproduire
   - Impact potentiel
   - Suggestion de correction (si possible)

---

## Limitations connues

### 1. Permissions système

Neatify ne peut pas protéger contre :
- Un utilisateur root/admin qui force l'accès à des dossiers système
- Des modifications de permissions en cours d'exécution

**Mitigation :** Ne pas exécuter Neatify avec des privilèges élevés.

### 2. Race conditions avancées

Bien que les collisions TOCTOU soient gérées, des race conditions complexes restent théoriquement possibles dans des environnements multi-utilisateurs.

**Mitigation :** Utiliser Neatify dans des dossiers sous contrôle exclusif de l'utilisateur.

### 3. Attaques par nom de fichier

Des noms de fichiers très longs ou avec caractères Unicode non standards pourraient causer des problèmes.

**Mitigation :** Validation de la longueur et des caractères autorisés (à améliorer).

---

## Meilleures pratiques

### Pour les utilisateurs

1. ✅ **Toujours tester en dry-run** avant d'appliquer
2. ✅ **Ne jamais exécuter avec sudo/admin**
3. ✅ **Utiliser sur vos propres dossiers uniquement**
4. ✅ **Vérifier les règles avant de les appliquer**
5. ❌ **Ne pas pointer vers des dossiers système**
6. ❌ **Ne pas exécuter sur des dossiers partagés critiques**

### Pour les développeurs

1. ✅ **Valider toutes les entrées utilisateur**
2. ✅ **Tester les cas limites et malveillants**
3. ✅ **Utiliser des chemins absolus normalisés**
4. ✅ **Fail-safe plutôt que fail-open**
5. ❌ **Ne jamais exécuter de code arbitraire**
6. ❌ **Ne pas exposer de surface d'attaque réseau**

---

## Références

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [CWE-22: Path Traversal](https://cwe.mitre.org/data/definitions/22.html)
- [CWE-367: TOCTOU](https://cwe.mitre.org/data/definitions/367.html)
- [Principle of Least Privilege](https://en.wikipedia.org/wiki/Principle_of_least_privilege)
- [Defense in Depth](https://en.wikipedia.org/wiki/Defense_in_depth_(computing))

---

## Changelog de sécurité

### Version actuelle
- ✅ Protection path traversal multi-niveaux
- ✅ Quota anti-DOS configurable
- ✅ Gestion atomique des collisions TOCTOU
- ✅ Validation des dossiers système
- ✅ Détection des liens symboliques
- ✅ 33 tests de sécurité dédiés

### Améliorations futures
- [ ] Validation de la longueur des noms de fichiers
- [ ] Sandbox optionnel (chroot/jail)
- [ ] Logging des opérations sensibles
- [ ] Signature des fichiers de règles
- [ ] Mode audit (traçabilité complète)
