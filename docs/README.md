# Documentation Neatify

Bienvenue dans la documentation détaillée de Neatify !

---

## 📚 Table des matières

### Pour les utilisateurs

- **[../README.md](../README.md)** - Guide de démarrage rapide et utilisation de base

### Pour les développeurs

1. **[ARCHITECTURE.md](ARCHITECTURE.md)** - Architecture du projet
   - Structure des packages (cli/ et core/)
   - API des composants principaux
   - Patterns de conception utilisés
   - Flux de données et workflow

2. **[TESTING.md](TESTING.md)** - Guide des tests
   - Exécution des tests
   - Architecture des tests (60+ tests)
   - Conventions et bonnes pratiques
   - Ajouter de nouveaux tests

3. **[SECURITY.md](SECURITY.md)** - Documentation de sécurité
   - Modèle de menaces
   - Protections implémentées (path traversal, DOS, TOCTOU)
   - Tests de sécurité (33 tests dédiés)
   - Signaler une vulnérabilité

4. **[DEVELOPMENT.md](DEVELOPMENT.md)** - Guide du développeur
   - Configuration de l'environnement
   - Workflow de développement
   - Conventions de code
   - Ajouter une fonctionnalité
   - Release et contribution

---

## 🚀 Quick Start par profil

### Je suis un utilisateur
👉 Commencez par [README.md](../README.md)

### Je veux contribuer
1. Lire [DEVELOPMENT.md](DEVELOPMENT.md) - Configuration et workflow
2. Lire [ARCHITECTURE.md](ARCHITECTURE.md) - Comprendre le code
3. Lire [TESTING.md](TESTING.md) - Écrire des tests

### Je veux auditer la sécurité
1. Lire [SECURITY.md](SECURITY.md) - Modèle de menaces
2. Lire [TESTING.md](TESTING.md#tests-de-sécurité) - Tests de sécurité
3. Lire [ARCHITECTURE.md](ARCHITECTURE.md#validationde-sécurité) - Architecture de validation

### Je veux comprendre le code
1. Lire [ARCHITECTURE.md](ARCHITECTURE.md) - Vue d'ensemble
2. Explorer le code dans `src/main/java/io/neatify/`
3. Lire [TESTING.md](TESTING.md) - Voir les tests pour des exemples d'utilisation

---

## 📖 Liens rapides

### Architecture
- [Structure des packages](ARCHITECTURE.md#structure-des-packages)
- [Package core/](ARCHITECTURE.md#package-core--logique-métier)
- [Package cli/](ARCHITECTURE.md#package-cli--interface-utilisateur)
- [Patterns utilisés](ARCHITECTURE.md#patterns-utilisés)

### Tests
- [Exécuter les tests](TESTING.md#exécution-des-tests)
- [Architecture des tests](TESTING.md#architecture-des-tests)
- [Tests de sécurité](TESTING.md#tests-de-sécurité)
- [Ajouter un test](TESTING.md#ajouter-de-nouveaux-tests)

### Sécurité
- [Protections implémentées](SECURITY.md#protections-implémentées)
- [Path Traversal](SECURITY.md#1-protection-contre-path-traversal)
- [Anti-DOS](SECURITY.md#2-protection-anti-dos-quota-de-fichiers)
- [Anti-TOCTOU](SECURITY.md#3-protection-anti-toctou-time-of-check-time-of-use)
- [Signaler une vulnérabilité](SECURITY.md#signaler-une-vulnérabilité)

### Développement
- [Configuration](DEVELOPMENT.md#configuration-de-lenvironnement)
- [Commandes Maven](DEVELOPMENT.md#commandes-maven)
- [Workflow](DEVELOPMENT.md#workflow-de-développement)
- [Ajouter une fonctionnalité](DEVELOPMENT.md#ajouter-une-nouvelle-fonctionnalité)
- [Contribution](DEVELOPMENT.md#contribution)

---

## 🔍 Navigation

```
docs/
├── README.md              # Ce fichier (index de la documentation)
├── ARCHITECTURE.md        # Architecture du projet
├── TESTING.md             # Guide des tests
├── SECURITY.md            # Documentation de sécurité
└── DEVELOPMENT.md         # Guide du développeur
```

---

## 💡 Conseil

**Lecture recommandée dans cet ordre :**

1. **[../README.md](../README.md)** - Comprendre ce que fait Neatify
2. **[ARCHITECTURE.md](ARCHITECTURE.md)** - Comprendre comment c'est construit
3. **[TESTING.md](TESTING.md)** - Comprendre comment c'est testé
4. **[SECURITY.md](SECURITY.md)** - Comprendre les protections
5. **[DEVELOPMENT.md](DEVELOPMENT.md)** - Commencer à contribuer

---

## 📞 Besoin d'aide ?

- **Questions générales** : Voir le [README principal](../README.md)
- **Questions techniques** : Voir [DEVELOPMENT.md](DEVELOPMENT.md)
- **Questions de sécurité** : Voir [SECURITY.md](SECURITY.md)
- **Issues/Bugs** : Ouvrir une issue sur GitHub/GitLab
