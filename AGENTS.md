# AGENTS.md

This repository contains a small Java backend codebase without a formal build system (no Maven/Gradle detected). The guidance below is intended for agentic coding assistants operating in this repo.

---

## Repository Overview

- Language: Java
- Build system: None detected (plain `javac` / `java`)
- Test framework: None detected
- Entry points:
  - `PredictionMarketGame.java`
  - Classes under `engine/`, `core/`, `logic/`, and `model/`

If a build tool is added in the future (Maven/Gradle), this file should be updated accordingly.

---

## Build, Run, Lint, and Test Commands

### Compile (All Sources)

From the repository root:

```
javac $(find . -name "*.java")
```

This compiles all Java files in-place and produces `.class` files alongside sources.

### Run Main Class

Example:

```
java PredictionMarketGame
```

If packages are introduced later, adjust the classpath accordingly:

```
java -cp . com.example.MainClass
```

### Clean Build Artifacts

```
find . -name "*.class" -delete
```

### Linting

No linter is currently configured.

Optional (recommended) if installed locally:

- `checkstyle`
- `spotbugs`
- `google-java-format` (format-only)

Agents should not introduce mandatory linting unless explicitly requested.

### Tests

- No automated tests detected in this repository
- No test framework (JUnit/TestNG) configured

If tests are added:

- Prefer JUnit 5
- Keep tests under `src/test/java`
- Name tests as `ClassNameTest`

#### Running a Single Test (Future JUnit Example)

```
./gradlew test --tests ClassNameTest
```

(This is illustrative only; Gradle is not currently present.)

---

## Code Style Guidelines

### General Principles

- Keep code simple and explicit
- Prefer readability over cleverness
- Avoid introducing frameworks unless requested
- Minimize side effects and global state

### File Organization

- One top-level class per file
- File name must match public class name
- Group related classes by domain (`engine`, `logic`, `model`, etc.)

### Packages

- Currently inconsistent / minimal package usage
- If adding packages:
  - Use all-lowercase names
  - Follow domain structure (e.g. `core.lmsr`, `model.player`)
  - Do not reorganize existing files unless requested

### Imports

- Use explicit imports (no wildcard imports)
- Group imports in this order:
  1. `java.*`
  2. `javax.*`
  3. Project-local imports
- Separate groups with a single blank line

### Formatting

- Indentation: 4 spaces
- No tabs
- Max line length: ~100 characters (soft limit)
- One statement per line
- Opening brace on same line

Example:

```
public class Example {
    public void run() {
        doWork();
    }
}
```

### Naming Conventions

- Classes: `PascalCase`
- Methods: `camelCase`
- Variables: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Packages: `lowercase`

Avoid:

- Single-letter variable names (except simple loops)
- Abbreviations unless domain-standard (e.g. LMSR)

### Types

- Prefer concrete types where clarity matters
- Avoid raw types; always use generics
- Use `final` for constants and immutable references when appropriate

### Error Handling

- Prefer checked exceptions for recoverable errors
- Use unchecked exceptions for programmer errors
- Do not swallow exceptions

Bad:

```
catch (Exception e) {}
```

Good:

```
catch (IOException e) {
    throw new RuntimeException(e);
}
```

### Logging / Output

- No logging framework currently in use
- Prefer `System.out.println` only for CLI/demo code
- Do not add logging dependencies unless requested

### Comments

- Avoid redundant comments
- Comment *why*, not *what*
- Public classes and non-obvious methods should have brief Javadoc

### State and Mutability

- Minimize mutable shared state
- Prefer passing data explicitly via method parameters
- Avoid static mutable fields

---

## Agent-Specific Guidance

- Do not add build tools, dependencies, or tests unless explicitly requested
- Do not refactor package structure without permission
- Do not commit `.class` files
- Keep changes minimal and scoped to the task
- Follow existing patterns unless clearly incorrect

---

## Cursor / Copilot Rules

- No `.cursor/rules/` directory detected
- No `.cursorrules` file detected
- No `.github/copilot-instructions.md` detected

If any of these are added later, their rules take precedence over this file.

---

## Updating This File

If you:

- Add a build system
- Add tests
- Introduce formatting or linting tools

Update this `AGENTS.md` accordingly so future agents operate correctly.
