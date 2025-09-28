<!--
  Kotlin Hybrid Architecture Template - Documentation
  Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
  SPDX-License-Identifier: BSD-3-Clause
  See LICENSE file in the project root.
-->

# Architecture Overview

## Introduction

This project implements a hybrid architecture that combines the best practices from Domain-Driven Design (DDD), Clean Architecture, and Hexagonal Architecture. The architecture ensures that business logic remains independent of technical details, making the system flexible, testable, and maintainable.

## Core Principles

### 1. Dependency Inversion Principle (DIP)
Dependencies always point inward. Outer layers depend on inner layers, but never the reverse. This ensures that business logic doesn't depend on technical implementation details.

### 2. Separation of Concerns
Each layer has a specific responsibility:
- **Domain**: Business logic and rules
- **Application**: Use case orchestration
- **Infrastructure**: Technical implementations
- **Presentation**: User interface
- **Bootstrap**: Application startup and dependency wiring

### 3. Functional Error Handling
The system uses functional programming principles for error handling, specifically Arrow's `Either` type, avoiding exceptions across layer boundaries.

## Architecture Layers

### Domain Layer (Core)
The innermost layer containing pure business logic:
- **No external dependencies** - Only uses Kotlin standard library and Arrow
- **Business rules** - All business logic lives here
- **Value objects** - Immutable objects representing domain concepts
- **Domain services** - Business operations that don't fit in entities
- **Port interfaces** - Contracts for external dependencies

### Application Layer
Orchestrates the flow of data through the system:
- **Use cases** - Single business operations
- **DTOs** - Data transfer objects for input/output
- **Application services** - Orchestration logic
- **Port definitions** - Additional contracts for infrastructure

### Infrastructure Layer
Implements technical details:
- **Adapters** - Implementations of port interfaces
- **External integrations** - Database, file system, APIs
- **Technical services** - Framework-specific code

### Presentation Layer
Handles user interaction:
- **CLI/API controllers** - User interface components
- **View models** - Presentation-specific data structures
- **Input validation** - User input processing

### Bootstrap Module
The composition root of the application:
- **Dependency wiring** - Manual dependency injection
- **Configuration** - Application startup configuration
- **Entry point** - Main function

## Data Flow

1. **User Request** → Presentation Layer receives input
2. **Command Creation** → Presentation creates application DTOs
3. **Use Case Execution** → Application layer orchestrates domain logic
4. **Domain Processing** → Business rules are applied
5. **Infrastructure Actions** → Technical operations are performed
6. **Response Formatting** → Results flow back through layers

## Error Handling Strategy

The system uses functional error handling throughout:
- **Domain errors** - Business rule violations
- **Application errors** - Use case failures
- **Infrastructure errors** - Technical failures
- **Presentation errors** - User input errors

Each layer transforms errors appropriately, ensuring that technical details don't leak into business logic.

## Testing Strategy

### Unit Tests
- **Domain tests** - Pure logic testing without dependencies
- **Application tests** - Use case testing with mocked ports
- **Infrastructure tests** - Adapter testing with real dependencies
- **Presentation tests** - UI logic testing

### Integration Tests
- **End-to-end tests** - Full system flow testing
- **Contract tests** - Port/adapter contract verification

### Architecture Tests
- **Dependency rules** - Automated verification of layer dependencies
- **Naming conventions** - Code structure validation
- **Package organization** - Module boundary enforcement

## Key Design Patterns

### 1. Port and Adapter Pattern
- Ports define contracts in inner layers
- Adapters implement contracts in outer layers
- Enables dependency inversion

### 2. Factory Pattern
- Used for creating complex objects
- Encapsulates creation logic
- Example: `CliFactory` in presentation layer

### 3. Composition Root Pattern
- All dependencies wired in one place
- No dependency injection framework needed
- Located in bootstrap module

### 4. Value Object Pattern
- Immutable objects for domain concepts
- Self-validating with factory methods
- Example: `PersonName` value object

## Benefits of This Architecture

1. **Testability** - Each layer can be tested in isolation
2. **Flexibility** - Easy to swap implementations
3. **Maintainability** - Clear separation of concerns
4. **Business Focus** - Domain logic is protected from technical details
5. **Scalability** - Easy to add new features without affecting existing code

## Common Scenarios

### Adding a New Feature
1. Define domain models and rules
2. Create use case in application layer
3. Define necessary ports
4. Implement adapters in infrastructure
5. Add presentation layer interface
6. Wire dependencies in bootstrap

### Changing Infrastructure
1. Create new adapter implementation
2. Update wiring in bootstrap
3. No changes needed in domain or application

### Adding Validation
1. Domain validation in value objects
2. Business rule validation in domain services
3. Input validation in presentation layer
4. Technical validation in infrastructure adapters

## Next Steps

For detailed information about each component, refer to:
- [Domain Layer Documentation](./domain-layer.md)
- [Application Layer Documentation](./application-layer.md)
- [Infrastructure Layer Documentation](./infrastructure-layer.md)
- [Presentation Layer Documentation](./presentation-layer.md)
- [Bootstrap Module Documentation](./bootstrap-module.md)