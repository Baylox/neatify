# Custom Rules

Ce dossier est destiné à vos **fichiers de règles personnalisés**.

## Utilisation

1. Créez vos propres fichiers `.properties` dans ce dossier
2. Utilisez-les avec Neatify :
   ```bash
   java -jar target/neatify.jar --source ~/Downloads --rules custom-rules/my-rules.properties --apply
   ```

## Format des règles

Les fichiers de règles suivent le format `.properties` :

```properties
# Commentaire
extension=DossierCible

# Exemples
pdf=Documents
jpg=Images
mp4=Videos
```

## Template

Copiez le fichier `example.properties.template` et renommez-le pour créer vos propres règles :

```bash
cp example.properties.template my-rules.properties
```

## Note

Les fichiers `*.properties` de ce dossier sont **ignorés par Git** (non versionnés), ce qui vous permet de créer vos règles personnalisées sans polluer le dépôt.

Seuls ce README et le template sont versionnés.
