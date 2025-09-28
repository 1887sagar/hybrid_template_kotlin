<!--
  Kotlin Hybrid Architecture Template - Documentation
  Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
  SPDX-License-Identifier: BSD-3-Clause
  See LICENSE file in the project root.
-->

# Application Layer Documentation

## Overview

The application layer orchestrates the flow of data through the system. It coordinates domain objects to perform specific use cases while remaining independent of technical implementation details. This layer defines the application's behavior by implementing use cases that fulfill user requirements.

## Key Components

### Use Cases

Use cases represent single business operations that the system can perform. Each use case is a class that encapsulates one specific user intent.

#### CreateGreetingUseCase

```kotlin
class CreateGreetingUseCase(
    private val greetingService: GreetingService,
    private val outputPort: OutputPort,
) : CreateGreetingInputPort {
    override suspend fun execute(
        command: CreateGreetingCommand
    ): Either<ApplicationError, GreetingResult>
}
```

**Purpose**: Orchestrates the creation and delivery of a greeting
- **Dependencies**: Injected through constructor (manual DI)
- **Interface Implementation**: Implements `CreateGreetingInputPort` for abstraction
- **Error Handling**: Transforms domain errors to application errors
- **Flow Control**: Coordinates multiple operations in sequence

**Execution Flow**:
1. Validates and transforms input command
2. Creates domain value objects
3. Calls domain service for business logic
4. Sends output through port
5. Returns result or error

### Data Transfer Objects (DTOs)

DTOs carry data between layers without exposing domain internals.

#### Input DTOs

```kotlin
data class CreateGreetingCommand(
    val name: String? = null,
    val silent: Boolean = false,
)
```

**Purpose**: Encapsulates user input for the use case
- **Optional Fields**: Name can be null (anonymous greeting)
- **Defaults**: Sensible defaults for better usability
- **Simple Types**: Uses primitives, not domain objects
- **Immutable**: Data classes are immutable by default

#### Output DTOs

```kotlin
data class GreetingResult(
    val message: String,
    val recipient: String,
)
```

**Purpose**: Represents the result of a successful greeting creation
- **Presentation Data**: Contains data ready for display
- **No Domain Objects**: Doesn't expose internal domain types
- **Success Case Only**: Errors use the ApplicationError hierarchy

### Port Interfaces

Ports define contracts for external dependencies, following the Dependency Inversion Principle.

#### Input Ports

```kotlin
interface CreateGreetingInputPort {
    suspend fun execute(
        command: CreateGreetingCommand
    ): Either<ApplicationError, GreetingResult>
}
```

**Purpose**: Contract that presentation layer uses to invoke use cases
- **Single Method**: Each port typically has one method
- **Clear Naming**: Method names indicate the action
- **Consistent Return Type**: Always returns Either for errors
- **Async Support**: Uses suspend for non-blocking operations

#### Output Ports

```kotlin
interface OutputPort {
    suspend fun send(message: String): Either<ApplicationError, Unit>
}
```

**Purpose**: Contract for sending output to external systems
- **Technology Agnostic**: Doesn't specify how output is sent
- **Error Handling**: Can fail, returns Either
- **Simple Interface**: Single responsibility
- **Implementation Freedom**: Can be console, file, network, etc.

### Application Errors

Application layer defines its own error types that wrap or transform domain errors:

```kotlin
sealed class ApplicationError {
    data class UseCaseError(
        val useCase: String,
        val cause: String,
    ) : ApplicationError()
    
    data class OutputError(
        val message: String,
    ) : ApplicationError()
    
    data class DomainErrorWrapper(
        val domainError: DomainError,
    ) : ApplicationError() {
        val userMessage: String = // Format domain error for users
    }
}
```

**Error Types**:
- **UseCaseError**: Use case execution failures
- **OutputError**: Output port failures
- **DomainErrorWrapper**: Wraps domain errors with user-friendly messages

## Design Patterns

### Command Pattern

Commands encapsulate all information needed to perform an action:

```kotlin
data class CreateGreetingCommand(
    val name: String? = null,
    val silent: Boolean = false,
)
```

**Benefits**:
- Decouples sender from receiver
- Enables command queuing and logging
- Simplifies testing with clear inputs

### Use Case Pattern

Each use case follows a consistent structure:

```kotlin
class SomeUseCase(
    // Dependencies injected
) : SomeInputPort {
    override suspend fun execute(command: SomeCommand): Either<ApplicationError, SomeResult> {
        // 1. Validate/transform input
        // 2. Call domain logic
        // 3. Handle side effects
        // 4. Return result
    }
}
```

### Port and Adapter Pattern

The application layer defines ports (interfaces) that are implemented by adapters in other layers:

```kotlin
// Application layer defines the port
interface NotificationPort {
    suspend fun notify(user: UserId, message: String): Either<ApplicationError, Unit>
}

// Infrastructure layer implements the adapter
class EmailNotificationAdapter : NotificationPort {
    override suspend fun notify(user: UserId, message: String): Either<ApplicationError, Unit> {
        // Email implementation
    }
}
```

## Best Practices

### 1. Keep Use Cases Focused
```kotlin
// Good: Single responsibility
class CreateOrderUseCase
class CancelOrderUseCase
class UpdateOrderStatusUseCase

// Bad: Too many responsibilities
class OrderManagementUseCase {
    fun create() { }
    fun cancel() { }
    fun update() { }
    // Too much!
}
```

### 2. Transform Errors Appropriately
```kotlin
// Good: Transform domain errors with context
domainResult.mapLeft { domainError ->
    ApplicationError.DomainErrorWrapper(domainError)
}

// Bad: Expose domain errors directly
return domainResult // Don't leak domain errors
```

### 3. Use Functional Composition
```kotlin
// Good: Chain operations with flatMap
return nameResult.flatMap { personName ->
    greetingService.createGreeting(personName)
        .mapLeft { ApplicationError.DomainErrorWrapper(it) }
        .flatMap { greeting ->
            outputPort.send(greeting)
                .map { GreetingResult(greeting, personName.value) }
        }
}
```

### 4. Define Clear Port Contracts
```kotlin
// Good: Specific, focused port
interface PaymentPort {
    suspend fun processPayment(
        amount: Money,
        method: PaymentMethod
    ): Either<PaymentError, PaymentReceipt>
}

// Bad: Generic, unclear port
interface ServicePort {
    suspend fun doSomething(data: Map<String, Any>): Any
}
```

## Testing Application Logic

Application layer tests should mock ports and verify orchestration:

```kotlin
class CreateGreetingUseCaseTest {
    private val greetingService = mockk<GreetingService>()
    private val outputPort = mockk<OutputPort>()
    private val useCase = CreateGreetingUseCase(greetingService, outputPort)
    
    @Test
    fun `should create and send greeting successfully`() = runTest {
        // Given
        val command = CreateGreetingCommand(name = "Alice")
        val greeting = "Hello World from Alice!"
        
        every { greetingService.createGreeting(any()) } returns greeting.right()
        every { outputPort.send(greeting) } returns Unit.right()
        
        // When
        val result = useCase.execute(command)
        
        // Then
        result.shouldBeRight()
        result.value.message shouldBe greeting
        result.value.recipient shouldBe "Alice"
        
        verify { greetingService.createGreeting(any()) }
        verify { outputPort.send(greeting) }
    }
}
```

## Common Patterns

### Handling Optional Values
```kotlin
val nameResult = if (command.name.isNullOrBlank()) {
    Either.Right(PersonName.anonymous())
} else {
    PersonName.create(command.name)
        .mapLeft { ApplicationError.DomainErrorWrapper(it) }
}
```

### Conditional Side Effects
```kotlin
if (!command.silent) {
    outputPort.send(greeting)
        .map { GreetingResult(greeting, personName.value) }
} else {
    Either.Right(GreetingResult(greeting, personName.value))
}
```

### Error Context Enhancement
```kotlin
domainService.performOperation()
    .mapLeft { domainError ->
        ApplicationError.UseCaseError(
            useCase = "CreateGreeting",
            cause = "Failed during greeting creation: ${domainError.message}"
        )
    }
```

## Guidelines for Extension

When adding new use cases:

1. **Define the Command** - What input does the use case need?
2. **Define the Result** - What does success look like?
3. **Identify Dependencies** - What ports/services are needed?
4. **Create Input Port** - Define the contract for invokers
5. **Implement Use Case** - Orchestrate the operation
6. **Handle All Error Cases** - Transform errors appropriately
7. **Write Tests** - Mock dependencies, verify orchestration

### Example: Adding a New Use Case

```kotlin
// 1. Define command
data class SearchGreetingsCommand(
    val query: String,
    val limit: Int = 10,
)

// 2. Define result
data class SearchResult(
    val greetings: List<GreetingInfo>,
    val totalCount: Int,
)

// 3. Define input port
interface SearchGreetingsInputPort {
    suspend fun execute(
        command: SearchGreetingsCommand
    ): Either<ApplicationError, SearchResult>
}

// 4. Implement use case
class SearchGreetingsUseCase(
    private val repository: GreetingRepositoryPort,
) : SearchGreetingsInputPort {
    override suspend fun execute(
        command: SearchGreetingsCommand
    ): Either<ApplicationError, SearchResult> {
        // Implementation
    }
}
```

## Summary

The application layer is the orchestrator of your system. It:
- **Coordinates** domain objects to fulfill use cases
- **Transforms** data between layers
- **Defines** contracts for external dependencies
- **Handles** error transformation and flow control
- **Remains** independent of technical details

By keeping this layer focused on orchestration rather than business logic or technical details, you create a system that is flexible, testable, and aligned with business needs.