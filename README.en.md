# Neatify

Automatic file organization tool with rule-based sorting.

---

## Description

Neatify is a command-line utility written in Java that automatically organizes files into categorized folders based on customizable extension rules. Built with simplicity and safety in mind, it provides a dry-run mode by default to preview changes before applying them.

**Key Features:**
- Simple architecture: 4 core classes, zero external dependencies
- Safe operations: dry-run mode by default, collision handling with auto-rename
- Extensible: rule-based configuration via `.properties` files
- Robust: comprehensive input validation and error handling

---

## Installation

### Prerequisites

- Java 21 or higher
- Maven 3.8+ (or use the provided Maven Wrapper)

### Building from Source

```bash
# Clone the repository
git clone <repository-url>
cd neatify

# Build the executable JAR
mvn clean package

# Or use Maven Wrapper (Windows)
.\mvnw.cmd clean package

# The JAR will be created at target/neatify.jar
```

---

## Usage

### Interactive Mode (Recommended)

Simply launch the program without arguments to access the interactive menu:

```bash
java -jar target/neatify.jar
```

**Available menu options:**
1. Organize files (with preview and confirmation)
2. Create a rules file
3. Display help
4. Display version
5. Exit

The interactive mode guides users step by step, displays a preview of changes, and asks for confirmation before any modification.

### Command Line Mode

```bash
# Preview mode (dry-run)
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties

# Apply changes
java -jar target/neatify.jar --source ~/Downloads --rules rules.properties --apply

# Display help
java -jar target/neatify.jar --help

# Display version
java -jar target/neatify.jar --version

# Launch interactive mode explicitly
java -jar target/neatify.jar --interactive
```

### Development Mode

```bash
# Run directly with Maven
mvn exec:java
```

---

## Configuration

Create a `rules.properties` file with the following format:

```properties
# Images
jpg=Images
png=Images
gif=Images

# Documents
pdf=Documents
docx=Documents
txt=Documents

# Code
java=Code
py=Code
js=Code

# Nested folders supported
csv=Documents/Spreadsheets
pptx=Documents/Presentations
```

**Format:** `extension=TargetFolder`

**Rules:**
- Extensions are automatically normalized (lowercase, without dot)
- Target folders are created automatically if they don't exist
- Invalid characters in folder names are replaced with `_`
- Files without matching rules are ignored

---

## Architecture

```
src/main/java/io/neatify/
├── Neatify.java          # CLI entry point (argument parsing)
├── FileMetadata.java     # File metadata record
├── Rules.java            # Rules loading and validation
└── FileMover.java        # Core business logic (plan + execute)
```

**Design Principle:**
1. **Plan Phase:** Scans directory and computes required actions
2. **Execute Phase:** Applies actions (or simulates in dry-run mode)

---

## Testing

```bash
# Run unit tests
mvn test
```

---

## Security Features

- **Dry-run by default:** No files are moved without explicit `--apply` flag
- **No overwriting:** File collisions are resolved with `_1`, `_2`, etc. suffixes
- **Strict validation:** All paths and rules are validated before execution
- **Hidden files ignored:** Files starting with `.` are skipped by default
- **Atomic moves:** Uses `ATOMIC_MOVE` when available for safer operations

---

## Examples

### Example 1: Organize Downloads Folder

```bash
# Create custom rules
cat > my-rules.properties << EOF
pdf=Documents
jpg=Images
mp4=Videos
zip=Archives
EOF

# Preview changes
java -jar target/neatify.jar --source ~/Downloads --rules my-rules.properties

# Apply changes
java -jar target/neatify.jar --source ~/Downloads --rules my-rules.properties --apply
```

### Example 2: Organize Code Project

```properties
java=SourceCode/Java
py=SourceCode/Python
js=SourceCode/JavaScript
md=Documentation
json=Config
yaml=Config
```

---

## Development

### Project Structure

```
neatify/
├── pom.xml                       # Maven configuration
├── rules.properties              # Example rules
├── README.md                     # Documentation (French)
├── README.en.md                  # Documentation (English)
├── LICENSE                       # MIT License
└── src/
    ├── main/
    │   ├── java/io/neatify/     # Source code
    │   └── resources/            # Resources
    └── test/
        └── java/io/neatify/     # Unit tests
```

### Future Enhancements

- [ ] `Rule` interface for complex rules (by date, size, regex patterns)
- [ ] `--by-date` option to organize files by year/month
- [ ] `--report` option to generate JSON report of actions performed
- [ ] `--include-hidden` flag to process hidden files
- [ ] Multi-module Maven structure for external plugin support

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Contributing

Contributions, issues, and feature requests are welcome.

---

## Disclaimer

Always test with dry-run mode before applying changes to important data. This tool does not create automatic backups.

---

## Documentation

- [Documentation française](README.md)
