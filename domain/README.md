<!--
  Kotlin Hybrid Architecture Template
  Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
  SPDX-License-Identifier: BSD-3-Clause
  See LICENSE file in the project root.
-->

# Domain Module

## Overview

The Domain module is the **innermost core** of our hybrid DDD/Clean/Hexagonal architecture. It contains the pure business logic and is completely independent of any external frameworks, libraries, or infrastructure concerns. This is where your business rules live.

## Core Principles

- **NO external dependencies** (only Kotlin standard library and Arrow for functional programming)
- **Pure business logic only**
- **Immutable by default**
- **No side effects**
- **Framework agnostic**
- **Technology agnostic**

## Current Structure

```
domain/
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── com/
│   │           └── abitofhelp/
│   │               └── hybrid/
│   │                   └── domain/
│   │                       ├── error/           # Domain error types (sealed classes)
│   │                       │   └── DomainError.kt
│   │                       ├── service/         # Domain services and policies
│   │                       │   ├── GreetingPolicy.kt
│   │                       │   └── GreetingService.kt
│   │                       └── value/           # Immutable value objects
│   │                           └── PersonName.kt
│   └── test/
│       └── kotlin/
│           └── com/
│               └── abitofhelp/
│                   └── hybrid/
│                       └── domain/           # Domain layer tests
└── build.gradle.kts                        # Module build configuration
```

### Planned Structure (for future expansion)
The domain package has subdirectories ready for growth:
- `entity/` - For domain entities with identity
- `event/` - For domain events
- `model/` - For complex domain models
- `port/` - For additional port interfaces
- `repository/` - For repository interfaces
- `value_object/` - Legacy location (use `value/`)

## Key Concepts

### Value Objects (Current Implementation)
Immutable objects that are defined by their attributes rather than identity:
```kotlin
// PersonName - validates and encapsulates a person's name
val nameResult = PersonName.create("Alice")
nameResult.fold(
    { error -> println("Invalid: ${error.message}") },
    { name -> println("Hello, ${name.value}!") }
)

// Using the anonymous factory
val anonymous = PersonName.anonymous() // Always valid
```

### Future Value Objects Examples
```kotlin
data class Email(val value: String) {
    init {
        require(value.matches(emailRegex)) { "Invalid email format" }
    }
}

data class Money(val amount: BigDecimal, val currency: Currency) {
    init {
        require(amount >= BigDecimal.ZERO) { "Amount cannot be negative" }
    }
}
```

### Entities
Objects with a distinct identity that persists over time:
```kotlin
class Customer(
    val id: CustomerId,
    var name: CustomerName,
    var email: Email
) {
    fun changeEmail(newEmail: Email): DomainEvent {
        email = newEmail
        return EmailChangedEvent(id, newEmail)
    }
}
```

### Aggregates
Clusters of entities and value objects with defined boundaries:
```kotlin
class Order(
    val id: OrderId,
    val customerId: CustomerId,
    private val items: MutableList<OrderItem> = mutableListOf()
) {
    fun addItem(product: Product, quantity: Quantity): Either<DomainError, OrderItemAdded> {
        // Business logic for adding items
    }
    
    fun calculateTotal(): Money {
        // Business logic for calculating order total
    }
}
```

### Domain Events
Represent something that has happened in the domain:
```kotlin
sealed class DomainEvent {
    abstract val occurredAt: Instant
}

data class OrderPlacedEvent(
    val orderId: OrderId,
    val customerId: CustomerId,
    val total: Money,
    override val occurredAt: Instant = Instant.now()
) : DomainEvent()
```

### Ports (Interfaces)
Define contracts for external dependencies:
```kotlin
interface CustomerRepository {
    suspend fun findById(id: CustomerId): Either<DomainError, Customer>
    suspend fun save(customer: Customer): Either<DomainError, Customer>
}

interface PaymentGateway {
    suspend fun processPayment(amount: Money, card: CreditCard): Either<PaymentError, PaymentResult>
}
```

### Domain Services
Encapsulate domain logic that doesn't naturally fit within entities:
```kotlin
class PricingService {
    fun calculateDiscount(
        customer: Customer,
        items: List<OrderItem>
    ): Either<DomainError, Discount> {
        // Complex pricing logic
    }
}
```

### Error Handling
Use sealed classes for domain errors:
```kotlin
sealed class DomainError {
    data class ValidationError(val field: String, val message: String) : DomainError()
    data class BusinessRuleViolation(val rule: String, val reason: String) : DomainError()
    data class NotFound(val entity: String, val id: String) : DomainError()
    data class Conflict(val message: String) : DomainError()
}
```

## Dependencies

This module has **NO dependencies** on other modules in the project. It only depends on:
- Kotlin standard library
- Arrow-kt core (for functional programming patterns)
- Kotlinx coroutines (for async operations)
- Kotlinx datetime (for time handling)

## Best Practices

1. **Keep it Pure**: No frameworks, no databases, no HTTP, no UI
2. **Model the Domain Language**: Use ubiquitous language from your business domain
3. **Fail Fast**: Validate invariants in constructors and methods
4. **Immutability First**: Prefer immutable value objects
5. **Rich Domain Model**: Put business logic in domain objects, not services
6. **Explicit Error Handling**: Use Arrow's Either for recoverable errors
7. **Event Sourcing Ready**: Emit domain events for significant state changes

## Testing Strategy

All domain logic should be thoroughly tested:
- **Property-based testing** for value objects and invariants
- **State-based testing** for entities and aggregates
- **Behavior verification** for domain services
- **Event verification** for domain events
- **Specification testing** for business rules

Example:
```kotlin
class OrderTest {
    @Test
    fun `should calculate total correctly with multiple items`() {
        // Given
        val order = Order(OrderId("123"), CustomerId("456"))
        
        // When
        order.addItem(Product("ABC", Money(10.0, USD)), Quantity(2))
        order.addItem(Product("DEF", Money(15.0, USD)), Quantity(1))
        
        // Then
        order.calculateTotal() shouldBe Money(35.0, USD)
    }
}
```

## Common Patterns

### Factory Pattern
```kotlin
object CustomerFactory {
    fun create(name: String, email: String): Either<DomainError, Customer> {
        return Either.catch {
            Customer(
                id = CustomerId.generate(),
                name = CustomerName(name),
                email = Email(email)
            )
        }.mapLeft { DomainError.ValidationError("customer", it.message ?: "") }
    }
}
```

### Specification Pattern
```kotlin
interface Specification<T> {
    fun isSatisfiedBy(candidate: T): Boolean
}

class PremiumCustomerSpecification : Specification<Customer> {
    override fun isSatisfiedBy(candidate: Customer): Boolean {
        return candidate.totalPurchases > Money(1000, USD)
    }
}
```

## Important Notes

1. **Never add framework dependencies** to this module
2. **All I/O operations** must be defined as port interfaces
3. **Keep business logic pure** - no side effects
4. **Use Arrow's Either** for error handling, not exceptions
5. **Model the domain language** - use terms from your business domain
6. **Validate eagerly** - fail fast with clear error messages