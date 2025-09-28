<!--
  Kotlin Hybrid Architecture Template
  Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
  SPDX-License-Identifier: BSD-3-Clause
  See LICENSE file in the project root.
-->

# Kotlin Hybrid Architecture Template

A production-ready Kotlin starter project implementing a hybrid architecture that combines the best principles from Domain-Driven Design (DDD), Clean Architecture, and Hexagonal Architecture with strict adherence to the Dependency Inversion Principle (DIP).

## Why This Template?

This template provides a robust foundation for building maintainable, testable, and scalable Kotlin applications. It enforces architectural boundaries through automated tests and provides clear patterns for organizing code according to business capabilities rather than technical layers.

## Architecture Overview

This project implements a hybrid architecture with strict layer separation:

- **Domain Layer** - Pure business logic with no external dependencies
- **Application Layer** - Use cases that orchestrate domain objects  
- **Infrastructure Layer** - Technical implementations and external integrations
- **Presentation Layer** - User interface and API endpoints
- **Bootstrap Layer** - Application startup and dependency injection

### Key Architectural Principles

- **Dependency Inversion Principle (DIP)** - High-level modules don't depend on low-level modules
- **Domain-Driven Design** - Business logic is central and isolated
- **Clean Architecture** - Clear separation of concerns with unidirectional dependencies
- **Hexagonal Architecture** - Ports and adapters for external integrations

### Recent Enhancements

- **Self-Validating DTOs** - Reduced boilerplate with validation encapsulated in DTOs
- **Buffered File Operations** - High-performance I/O with configurable buffering
- **OWASP Dependency Scanning** - Built-in security vulnerability checking
- **Comprehensive KDoc** - Educational documentation throughout, including test files
- **Async-First Design** - Coroutines and structured concurrency patterns
- **Security Hardening** - Input sanitization and attack vector prevention

### Architecture Diagrams

Comprehensive architecture diagrams are available in [`docs/diagrams/`](docs/diagrams/):
- [Architecture Overview](docs/diagrams/architecture-overview.svg) - High-level system view
- [Layer Dependencies](docs/diagrams/layer-dependencies.svg) - Clean Architecture dependency rules
- [Domain Model](docs/diagrams/domain-model.svg) - Core domain classes and relationships
- [Package Structure](docs/diagrams/package-structure.svg) - Module organization
- [Vertical Slice](docs/diagrams/vertical-slice.svg) - Complete feature implementation

## Project Structure

```
hybrid_architecture_template/
├── domain/                 # Core business logic (no dependencies)
├── application/            # Use cases and application services  
├── infrastructure/         # External integrations and adapters
├── presentation/           # User interfaces and controllers
├── bootstrap/              # Application entry point and composition root
├── architecture-tests/     # ArchUnit tests enforcing architecture rules
├── buildSrc/              # Gradle convention plugins
├── docs/                  # Documentation and diagrams
├── scripts/               # Development and maintenance scripts
├── standards/             # Coding standards and conventions
└── config/                # Tool configurations (Detekt, etc.)
    └── diagrams/          # PlantUML architecture diagrams
```

## Getting Started

### Prerequisites
- JDK 21 or higher
- Gradle (included via wrapper)

### Quick Start

1. **Clone and customize the template:**
   ```bash
   git clone <repository-url> my-project
   cd my-project
   ./customize-template.sh
   ```

2. **Build the project:**
   ```bash
   ./gradlew build
   ```

3. **Run the application:**
   ```bash
   ./gradlew :bootstrap:run
   # Or with a custom name:
   ./gradlew :bootstrap:run --args="John"
   ```

4. **Run tests:**
   ```bash
   ./gradlew test
   ```

5. **Verify architecture:**
   ```bash
   ./gradlew :architecture-tests:test
   ```

## Common Tasks

```bash
# Build the project
./gradlew build

# Run the application
./gradlew :bootstrap:run
./gradlew :bootstrap:run --args="Alice"  # With custom name

# Run all tests
./gradlew test

# Run architecture tests only
./gradlew :architecture-tests:test

# Format code
./gradlew ktlintFormat

# Run static analysis
./gradlew detekt

# Check for dependency vulnerabilities
./gradlew dependencyCheckAggregate
# Or use Make:
make security

# Generate/update architecture diagrams
make diagrams

# View all available tasks
./gradlew tasks
```

## Module Descriptions

### Domain Module
The heart of the application containing pure business logic:
- Domain models and entities
- Value objects
- Domain services
- Port interfaces (for DIP)
- Business rules and validations

### Application Module
Orchestrates domain logic through use cases:
- Use case implementations
- Application services
- Self-validating DTOs with encapsulated validation
- Transaction boundaries
- Error translation between layers

### Infrastructure Module
Technical implementations and external integrations:
- Output adapters (Console, File, Buffered File)
- External API clients
- Framework-specific adapters
- High-performance buffered I/O implementations

### Presentation Module
User-facing interfaces:
- REST controllers
- GraphQL resolvers
- CLI commands
- View models and mappers

### Bootstrap Module
Application entry point and dependency wiring:
- Async and sync entry points
- Secure command-line argument parsing
- Composition root for manual dependency injection
- Signal handling for graceful shutdown
- Security hardening against injection attacks

### Architecture Tests Module
Automated architecture verification:
- Layer dependency rules
- Package structure validation
- Naming conventions
- Cyclic dependency checks

## Development Guidelines

### 1. Respect Layer Dependencies
- Domain → No dependencies
- Application → Domain only
- Infrastructure → Domain, Application
- Presentation → Application only (not Domain!)
- Bootstrap → All layers (composition root)

### 2. Follow the Ports and Adapters Pattern
- Define ports (interfaces) in domain/application layers
- Implement adapters in infrastructure layer
- Use manual dependency injection in bootstrap module to wire them together

### 3. Write Tests at Every Layer
- Domain: Unit tests for business logic
- Application: Integration tests for use cases
- Infrastructure: Integration tests with real dependencies
- Presentation: API/UI tests

### 4. Use Architecture Tests
Always run architecture tests to ensure compliance:
```bash
./gradlew :architecture-tests:test
```

## Testing

The project includes comprehensive test coverage with educational KDoc:

### Test Types
- **Unit Tests** - Domain logic and isolated components
- **Integration Tests** - Use case orchestration
- **Architecture Tests** - Automated rule enforcement
- **Security Tests** - Input validation and attack prevention

### Test Features
- BDD-style tests with Kotest
- Comprehensive KDoc documentation in all test files
- Property-based testing support
- Async testing with coroutines
- Mock support with MockK

### Running Tests
```bash
# All tests
./gradlew test

# With coverage
./gradlew test jacocoTestReport
make test-coverage

# Architecture tests only
./gradlew :architecture-tests:test
```

## Best Practices

### Domain Modeling
- Use value objects for concepts without identity
- Keep domain models free of framework annotations
- Express business rules explicitly in code

### Error Handling
- Use functional error handling (Arrow's Either)
- Define domain-specific exceptions
- Handle errors at appropriate layers

### Testing Strategy
- Test pyramid: Many unit tests, fewer integration tests
- Use test fixtures for common test data
- Mock external dependencies at infrastructure boundary
- Write educational KDoc in tests

### Code Organization
- Package by feature, not by layer within modules
- Keep related code close together
- Use clear, business-oriented naming

## Customization

### Adding a New Feature

1. Start with domain models
2. Define ports for external dependencies
3. Implement use cases in application layer
4. Create adapters in infrastructure
5. Add presentation layer endpoints
6. Write tests at each layer
7. Verify with architecture tests

### Modifying Architecture Rules

Edit `architecture-tests/src/test/kotlin/.../architecture/` to:
- Add new architectural constraints
- Modify existing rules
- Exclude specific packages

## Security

### Dependency Vulnerability Scanning

This project includes OWASP Dependency Check to scan for known vulnerabilities in dependencies:

```bash
# Run vulnerability scan
./gradlew dependencyCheckAggregate

# Or use Make
make security
```

The scan will:
- Check all dependencies against the National Vulnerability Database (NVD)
- Fail the build if vulnerabilities with CVSS score ≥ 7.0 are found
- Generate reports in HTML, JSON, and JUnit formats
- Reports are available in each module's `build/reports/` directory

To suppress false positives, add them to `config/owasp/suppressions.xml` with proper justification.

## Architecture Standards

This project follows strict architectural guidelines documented in [CLAUDE-Kotlin.md](CLAUDE-Kotlin.md), including:
- Kotlin idioms and best practices
- Functional programming principles
- Comprehensive testing requirements
- Documentation standards

## Contributing

1. Follow the architecture rules
2. Ensure all tests pass
3. Update documentation and diagrams
4. Use conventional commits
5. Run architecture tests before submitting

## Resources

### Project Documentation
- [CLAUDE-Kotlin.md](CLAUDE-Kotlin.md) - Detailed architecture guidelines
- [docs/](docs/) - Comprehensive documentation
- [docs/diagrams/](docs/diagrams/) - Architecture diagrams (PlantUML + SVG)
- [standards/](standards/) - Coding standards and conventions
- Test files - Educational KDoc throughout all tests

### Architecture References
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html) - Uncle Bob's Clean Architecture
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/) - Alistair Cockburn's Hexagonal Architecture
- [Domain-Driven Design](https://www.domainlanguage.com/ddd/) - Eric Evans' DDD

## License

This project is licensed under the BSD 3-Clause License. See [LICENSE](LICENSE) file for details.