# Kotlin Hybrid Architecture Template

**Version:** 1.0.0  
**Date:** January 2025  
**License:** BSD-3-Clause  
**Copyright:** © 2025 Michael Gardner, A Bit of Help, Inc.  
**Authors:** Michael Gardner  
**Status:** Released

A production-ready Kotlin template implementing a hybrid of Domain-Driven Design (DDD), Clean Architecture, and Hexagonal Architecture principles. This template provides a solid foundation for building maintainable, testable, and scalable applications.

## Table of Contents

- [Quick Start](#quick-start)
- [Architecture Overview](#architecture-overview)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Testing Strategy](#testing-strategy)
- [Common Patterns](#common-patterns)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## Quick Start

```bash
# Clone the repository
git clone https://github.com/yourusername/kotlin-hybrid-architecture-template.git
cd kotlin-hybrid-architecture-template

# Build the project
./gradlew build

# Run tests
./gradlew test

# Run the application
./gradlew :bootstrap:run --args="YourName"

# Additional CLI options
./gradlew :bootstrap:run --args="--help"                    # Show help
./gradlew :bootstrap:run --args="--version"                 # Show version
./gradlew :bootstrap:run --args="--quiet YourName"          # Quiet mode
./gradlew :bootstrap:run --args="--verbose YourName"        # Verbose output
./gradlew :bootstrap:run --args="--out=output.txt YourName" # File output
```

## Architecture Overview

This template combines the best aspects of three proven architectural patterns:

### Why Hybrid Architecture?

Traditional architectures often force you to choose between different approaches. This template recognizes that each pattern has strengths:

- **Domain-Driven Design (DDD)**: Focuses on modeling your business domain accurately
- **Clean Architecture**: Ensures dependencies point inward toward business logic
- **Hexagonal Architecture**: Isolates your application from external concerns through ports and adapters

### Core Principles

1. **Dependency Rule**: Dependencies only point inward toward the domain layer
2. **Isolation of Concerns**: Each layer has a specific responsibility
3. **Testability**: Every component can be tested in isolation
4. **Flexibility**: Easy to swap implementations without affecting business logic

### Layer Responsibilities

```
┌─────────────────────────────────────────────┐
│            Presentation Layer               │
│  (CLI, REST Controllers, GraphQL)           │
├─────────────────────────────────────────────┤
│            Application Layer                │
│    (Use Cases, Application Services)        │
├─────────────────────────────────────────────┤
│             Domain Layer                    │
│  (Entities, Value Objects, Domain Services) │
├─────────────────────────────────────────────┤
│          Infrastructure Layer               │
│   (Database, File System, External APIs)   │
└─────────────────────────────────────────────┘
```

## Project Structure

```
kotlin-hybrid-architecture-template/
├── domain/                 # Core business logic (no dependencies)
│   └── src/main/kotlin/
│       ├── model/         # Entities and aggregates
│       ├── value/         # Value objects
│       ├── service/       # Domain services
│       └── error/         # Domain-specific errors
│
├── application/           # Use cases and orchestration
│   └── src/main/kotlin/
│       ├── usecase/       # Business use cases
│       ├── port/          # Interface definitions
│       │   ├── input/     # Incoming ports (use cases)
│       │   └── output/    # Outgoing ports (repositories)
│       └── dto/           # Data transfer objects
│
├── infrastructure/        # External implementations
│   └── src/main/kotlin/
│       └── adapter/       # Port implementations
│           ├── output/    # File, database adapters
│           └── input/     # REST, CLI adapters
│
├── presentation/          # User interfaces
│   └── src/main/kotlin/
│       └── cli/           # Command-line interface
│
├── bootstrap/             # Application startup
│   └── src/main/kotlin/
│       └── config/        # Dependency injection
│
└── architecture-tests/    # Architecture verification
```

## Getting Started

### Prerequisites

Before you begin, ensure you have:

- **JDK 21 or higher**: The project uses modern Java features
- **Gradle**: Build automation (wrapper included)
- **Git**: Version control

### Initial Setup

1. **Clone and Navigate**
   ```bash
   git clone <repository-url>
   cd kotlin-hybrid-architecture-template
   ```

2. **Verify Setup**
   ```bash
   ./gradlew --version
   java -version
   ```

3. **Build the Project**
   ```bash
   ./gradlew clean build
   ```

### Understanding the Flow

Let's trace a simple request through the architecture:

1. **User Input**: User runs `./gradlew :bootstrap:run --args="Alice"` (or with options like `--verbose`, `--quiet`, `--version`)
2. **Bootstrap**: The bootstrap module starts the application
3. **Presentation**: CLI parses the command and creates a request
4. **Application**: Use case orchestrates the business logic
5. **Domain**: Domain service creates a greeting
6. **Infrastructure**: Output adapter writes to console/file
7. **Response**: User sees "Hello, Alice!"

### Your First Modification

Let's add a new feature - personalizing greetings based on time of day:

1. **Start with the Domain** (domain/src/main/kotlin/...)
   ```kotlin
   // value/TimeOfDay.kt
   enum class TimeOfDay {
       MORNING, AFTERNOON, EVENING, NIGHT
   }
   
   // service/TimeAwareGreetingService.kt
   interface TimeAwareGreetingService {
       fun greetWithTime(name: String, timeOfDay: TimeOfDay): String
   }
   ```

2. **Implement in Application** (application/src/main/kotlin/...)
   ```kotlin
   // usecase/CreateTimeAwareGreetingUseCase.kt
   class CreateTimeAwareGreetingUseCase(
       private val greetingService: TimeAwareGreetingService,
       private val outputPort: OutputPort
   ) : CreateTimeAwareGreetingInputPort {
       // Implementation
   }
   ```

3. **Add Infrastructure Support** (infrastructure/src/main/kotlin/...)
   ```kotlin
   // adapter/service/DefaultTimeAwareGreetingService.kt
   class DefaultTimeAwareGreetingService : TimeAwareGreetingService {
       override fun greetWithTime(name: String, timeOfDay: TimeOfDay): String {
           return when (timeOfDay) {
               MORNING -> "Good morning, $name!"
               AFTERNOON -> "Good afternoon, $name!"
               EVENING -> "Good evening, $name!"
               NIGHT -> "Good night, $name!"
           }
       }
   }
   ```

## Development Workflow

### Step-by-Step Development Process

1. **Always Start with Domain**
   - Define your business concepts
   - Create value objects for type safety
   - Design interfaces for services
   - No external dependencies allowed!

2. **Move to Application Layer**
   - Create use cases for each business operation
   - Define ports (interfaces) for external needs
   - Keep orchestration logic here

3. **Implement Infrastructure**
   - Create adapters for your ports
   - Handle technical concerns (files, network, etc.)
   - Keep framework-specific code here

4. **Connect via Bootstrap**
   - Wire dependencies in CompositionRoot
   - Configure application settings
   - Handle startup/shutdown

### Code Quality Checks

Run these commands regularly:

```bash
# Format code
./gradlew ktlintFormat

# Check code style
./gradlew ktlintCheck

# Run static analysis
./gradlew detekt

# Run all checks
./gradlew check
```

### Common Development Tasks

**Adding a New Use Case:**
1. Define the interface in `application/port/input/`
2. Implement in `application/usecase/`
3. Add tests in `application/src/test/kotlin/`
4. Wire in `bootstrap/CompositionRoot.kt`

**Adding a New Output Adapter:**
1. Define port in `application/port/output/`
2. Implement in `infrastructure/adapter/output/`
3. Add integration tests
4. Update CompositionRoot

## Testing Strategy

### Test Pyramid

```
        ╱─────╲
       ╱  E2E  ╲      Few tests, high confidence
      ╱─────────╲
     ╱Integration╲    Test adapters and integration
    ╱─────────────╲
   ╱   Unit Tests  ╲  Many tests, fast feedback
  ╱─────────────────╲
```

### Writing Effective Tests

**Domain Tests** - Pure logic, no mocks needed:
```kotlin
class GreetingTest : DescribeSpec({
    describe("Greeting creation") {
        it("should create formal greeting") {
            val greeting = Greeting.formal("Alice")
            greeting.message shouldBe "Good day, Ms. Alice"
        }
    }
})
```

**Use Case Tests** - Mock external dependencies:
```kotlin
class CreateGreetingUseCaseTest : DescribeSpec({
    describe("CreateGreetingUseCase") {
        val mockOutput = mockk<OutputPort>()
        val useCase = CreateGreetingUseCase(mockOutput)
        
        it("should send greeting to output") {
            every { mockOutput.send(any()) } returns Unit.right()
            
            val result = useCase.execute(CreateGreetingCommand("Alice"))
            
            result.shouldBeRight()
            verify { mockOutput.send("Hello, Alice!") }
        }
    }
})
```

**Architecture Tests** - Verify architectural rules:
```kotlin
class ArchitectureTest : DescribeSpec({
    describe("Architecture Rules") {
        it("domain should not depend on other layers") {
            classes()
                .that().resideInPackage("..domain..")
                .should().onlyDependOnClassesThat()
                .resideInPackages("..domain..", "java..", "kotlin..")
                .check(allClasses)
        }
    }
})
```

## Common Patterns

### Error Handling with Either

Instead of throwing exceptions, use Either for explicit error handling:

```kotlin
// Don't do this
fun riskyOperation(): String {
    if (somethingBad) throw Exception("Failed!")
    return "Success"
}

// Do this
fun safeOperation(): Either<DomainError, String> {
    return if (somethingBad) {
        DomainError.ValidationError("Failed!").left()
    } else {
        "Success".right()
    }
}

// Usage
when (val result = safeOperation()) {
    is Either.Left -> handleError(result.value)
    is Either.Right -> processSuccess(result.value)
}
```

### Dependency Injection without Frameworks

The bootstrap module uses manual dependency injection:

```kotlin
object CompositionRoot {
    fun buildApplication(config: AppConfig): Application {
        // Create infrastructure
        val repository = FileRepository(config.dataPath)
        
        // Create domain services
        val domainService = DefaultDomainService()
        
        // Create use cases
        val useCase = CreateEntityUseCase(repository, domainService)
        
        // Create presentation
        return CliApplication(useCase)
    }
}
```

### Value Objects for Type Safety

Don't use primitive types for domain concepts:

```kotlin
// Don't do this
fun transfer(fromAccount: String, toAccount: String, amount: Double)

// Do this
fun transfer(from: AccountId, to: AccountId, amount: Money)

// Define value objects
@JvmInline
value class AccountId(val value: String) {
    init {
        require(value.matches(Regex("[A-Z]{2}\\d{8}"))) {
            "Invalid account ID format"
        }
    }
}

data class Money(
    val amount: BigDecimal,
    val currency: Currency
) {
    init {
        require(amount >= BigDecimal.ZERO) {
            "Amount cannot be negative"
        }
    }
}
```

## Troubleshooting

### Common Issues and Solutions

**Issue: "Cannot find symbol" compilation errors**
- **Cause**: Missing imports or incorrect package structure
- **Solution**: Ensure all imports are present and packages match directory structure
- **Prevention**: Use IDE auto-import features

**Issue: Circular dependency detected**
- **Cause**: Two modules depending on each other
- **Solution**: Extract common interfaces to a shared module
- **Prevention**: Follow the dependency rule strictly

**Issue: Tests fail with "No value present"**
- **Cause**: Trying to access empty Optional/Either without checking
- **Solution**: Always check before accessing: `either.fold({ error -> }, { value -> })`
- **Prevention**: Use sealed classes for exhaustive when expressions

**Issue: OutOfMemoryError during build**
- **Cause**: Insufficient heap space for Gradle
- **Solution**: Add to `gradle.properties`: `org.gradle.jvmargs=-Xmx2g`
- **Prevention**: Close unnecessary applications during build

### Debugging Tips

1. **Enable Detailed Logging**
   ```bash
   ./gradlew test --info --stacktrace
   ```

2. **Run Single Test**
   ```bash
   ./gradlew test --tests "com.example.MyTest.my test method"
   ```

3. **Check Dependencies**
   ```bash
   ./gradlew :domain:dependencies
   ```

### Performance Optimization

If you encounter performance issues:

1. **Profile First**: Use JVM profilers to identify bottlenecks
2. **Optimize Carefully**: Don't optimize prematurely
3. **Consider Caching**: Add caching at infrastructure layer
4. **Use Coroutines**: For I/O-bound operations

## Contributing

### Before You Contribute

1. Read the architecture guides in `/docs/guides/`
2. Run all tests: `./gradlew test`
3. Check code style: `./gradlew ktlintCheck detekt`
4. Update documentation for significant changes

### Contribution Process

1. Create a feature branch
2. Make your changes following the architecture
3. Add tests for new functionality
4. Ensure all checks pass
5. Submit a pull request

### Code Review Checklist

- [ ] Follows dependency rule (no outward dependencies)
- [ ] Includes appropriate tests
- [ ] Documentation updated
- [ ] No code smells detected by tools
- [ ] Follows Kotlin conventions

## Next Steps

1. **Explore the Guides**: Read `/docs/guides/` for detailed explanations
2. **Run the Examples**: Try the sample application
3. **Experiment**: Create a new use case
4. **Learn More**: Study the architecture tests

## Resources

- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Hexagonal Architecture by Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Kotlin Official Documentation](https://kotlinlang.org/docs/home.html)

## License

This project is licensed under the BSD-3-Clause License. See the LICENSE file for details.