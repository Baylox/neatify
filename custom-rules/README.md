# Custom Rules

This folder is for your **custom rules files**.

## Usage

1. Create your own `.properties` files in this folder
2. Use them with Neatify:
   ```bash
   java -jar target/neatify.jar --source ~/Downloads --rules custom-rules/my-rules.properties --apply
   ```

## Rules Format

Rules files follow the `.properties` format:

```properties
# Comment
extension=TargetFolder

# Examples
pdf=Documents
jpg=Images
mp4=Videos
```

## Template

Copy the `example.properties.template` file and rename it to create your own rules:

```bash
cp example.properties.template my-rules.properties
```

## Note

The `*.properties` files in this folder are **ignored by Git** (not version-controlled), allowing you to create your custom rules without polluting the repository.

Only this README and the template are version-controlled.
