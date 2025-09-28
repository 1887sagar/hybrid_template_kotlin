<!--
  Kotlin Hybrid Architecture Template - Documentation
  Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
  SPDX-License-Identifier: BSD-3-Clause
  See LICENSE file in the project root.
-->

# Domain Layer Documentation

## Overview

The domain layer is the heart of the application, containing all business logic and rules. This layer has no dependencies on other layers or external frameworks, making it pure and easily testable.

## Key Concepts

### Value Objects

Value objects are immutable objects that represent domain concepts. They are defined by their values rather than their identity.

#### PersonName Value Object

```kotlin
@JvmInline
value class PersonName private constructor(val value: String) {
    companion object {
        fun create(value: String): Either<DomainError.ValidationError, PersonName>
        fun anonymous(): PersonName
    }
}
```

**Purpose**: Represents a person's name with validation rules
- **Validation**: Names must contain only letters, spaces, hyphens, and apostrophes
- **Factory Methods**: Use `create()` for validated creation, `anonymous()` for default
- **Immutability**: Once created, cannot be changed
- **Type Safety**: Prevents passing raw strings where PersonName is expected

### Domain Services

Domain services encapsulate business logic that doesn't naturally fit within a single entity or value object.

#### GreetingService Interface

```kotlin
interface GreetingService {
    suspend fun createGreeting(name: PersonName): Either<DomainError, String>
}
```

**Purpose**: Defines the contract for creating greetings
- **Asynchronous**: Uses `suspend` for non-blocking operations
- **Error Handling**: Returns `Either` for functional error handling
- **Domain Focus**: Accepts domain types (PersonName) not primitives

#### GreetingPolicy Object

```kotlin
object GreetingPolicy {
    private const val LONG_NAME_THRESHOLD = 20
    const val MAX_GREETING_LENGTH = 200
    
    fun determineGreetingFormat(name: PersonName): GreetingFormat
}
```

**Purpose**: Encapsulates business rules for greeting formatting
- **Business Rules**: Names over 20 characters get formal treatment
- **Constants**: Defines limits and thresholds
- **Pure Logic**: No side effects, just decision making

### Domain Errors

The domain layer defines its own error hierarchy using sealed classes:

```kotlin
sealed class DomainError {
    data class ValidationError(val field: String, val message: String) : DomainError()
    data class BusinessRuleViolation(val rule: String, val reason: String) : DomainError()
    data class NotFound(val entity: String, val id: String) : DomainError()
}
```

**Error Types**:
- **ValidationError**: Input validation failures
- **BusinessRuleViolation**: Business rule constraint violations
- **NotFound**: Entity lookup failures

## Design Patterns

### Factory Method Pattern

Used in value objects to ensure valid object creation:

```kotlin
companion object {
    fun create(value: String): Either<DomainError.ValidationError, PersonName> {
        val trimmed = value.trim()
        return when {
            trimmed.isBlank() -> 
                DomainError.ValidationError("name", "Name cannot be blank").left()
            !trimmed.matches(NAME_PATTERN) -> 
                DomainError.ValidationError("name", "Name can only contain...").left()
            else -> PersonName(trimmed).right()
        }
    }
}
```

**Benefits**:
- Ensures objects are always in a valid state
- Centralizes validation logic
- Returns errors instead of throwing exceptions

### Strategy Pattern

The GreetingFormat enum acts as a strategy for different greeting styles:

```kotlin
enum class GreetingFormat {
    DEFAULT,    // Standard greeting
    FRIENDLY,   // Casual greeting
    FORMAL      // More formal greeting
}
```

## Best Practices

### 1. Always Validate Input
```kotlin
// Good: Validation in factory method
fun create(value: String): Either<DomainError, PersonName> {
    // Validation logic here
}

// Bad: Public constructor without validation
class PersonName(val value: String) // Don't do this
```

### 2. Use Sealed Classes for Errors
```kotlin
// Good: Type-safe error handling
sealed class DomainError {
    // Specific error types
}

// Bad: Generic exceptions
throw Exception("Something went wrong") // Avoid this
```

### 3. Keep Domain Logic Pure
```kotlin
// Good: Pure function with no side effects
fun calculateDiscount(amount: Money, tier: CustomerTier): Money

// Bad: Function with side effects
fun calculateDiscount(amount: Money): Money {
    logger.info("Calculating discount") // Side effect!
    database.save(amount) // External dependency!
}
```

### 4. Use Domain-Specific Types
```kotlin
// Good: Type-safe domain concepts
value class EmailAddress(val value: String)
value class PhoneNumber(val value: String)

// Bad: Primitive obsession
fun sendMessage(email: String, phone: String) // Strings everywhere!
```

## Testing Domain Logic

Domain tests should be simple and focused:

```kotlin
class PersonNameTest {
    @Test
    fun `should create valid person name`() {
        val result = PersonName.create("John Doe")
        
        result.shouldBeRight()
        result.value.value shouldBe "John Doe"
    }
    
    @Test
    fun `should reject blank names`() {
        val result = PersonName.create("   ")
        
        result.shouldBeLeft()
        val error = result.leftOrNull() as DomainError.ValidationError
        error.field shouldBe "name"
        error.message shouldContain "blank"
    }
}
```

## Common Patterns and Solutions

### Handling Optional Values
```kotlin
// Use nullable types for optional domain concepts
data class Customer(
    val id: CustomerId,
    val name: PersonName,
    val email: EmailAddress? = null // Optional
)
```

### Aggregating Business Rules
```kotlin
object PricingPolicy {
    fun calculatePrice(
        basePrice: Money,
        quantity: Quantity,
        customerTier: CustomerTier
    ): Either<DomainError, Money> {
        // Complex business logic here
    }
}
```

### Domain Events (Future Enhancement)
```kotlin
sealed class DomainEvent {
    data class OrderPlaced(val orderId: OrderId, val timestamp: Instant) : DomainEvent()
    data class PaymentReceived(val amount: Money, val timestamp: Instant) : DomainEvent()
}
```

## Guidelines for Extension

When adding new domain concepts:

1. **Start with the domain model** - What business concept are you modeling?
2. **Define value objects** - What immutable data represents this concept?
3. **Add validation rules** - What makes the data valid?
4. **Create domain services** - What operations act on these concepts?
5. **Define error cases** - What can go wrong?
6. **Write tests first** - Ensure your domain logic is correct

## Summary

The domain layer is the most important layer in the application. It contains all business logic and must remain pure and independent. By following these patterns and practices, you ensure that your business logic is:

- **Testable** - No external dependencies to mock
- **Reusable** - Can be used in different contexts
- **Maintainable** - Clear and focused on business concerns
- **Reliable** - Always in a valid state