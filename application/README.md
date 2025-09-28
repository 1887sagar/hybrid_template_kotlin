<!--
  Kotlin Hybrid Architecture Template
  Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
  SPDX-License-Identifier: BSD-3-Clause
  See LICENSE file in the project root.
-->

# Application Module

## Overview

The Application module contains the **use cases and application services** that orchestrate the domain logic. It defines the application's boundaries and coordinates between the domain layer and the outside world through ports (interfaces).

## Core Principles

- **Depends only on Domain module**
- **Defines use cases** (application-specific business rules)
- **Defines output ports** for infrastructure implementations
- **Orchestrates domain objects**
- **Manages transaction boundaries**
- **No framework-specific code**

## Structure

```
application/
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── com/
│   │           └── abitofhelp/
│   │               └── hybrid/
│   │                   └── application/
│   │                       ├── constants/       # Application-specific constants
│   │                       ├── dto/            # Data transfer objects
│   │                       ├── error/          # Application-specific errors  
│   │                       ├── mapper/         # Domain-DTO mappers
│   │                       ├── port/           # Port interfaces for infrastructure
│   │                       │   ├── input/      # Input ports (use case interfaces)
│   │                       │   └── output/     # Output ports (for infrastructure)
│   │                       ├── service/        # Application services
│   │                       └── use_case/       # Use case implementations
│   └── test/
│       └── kotlin/
│           └── com/
│               └── abitofhelp/
│                   └── hybrid/
│                       └── application/       # Application layer tests
└── build.gradle.kts                       # Module build configuration
```

## Key Concepts

### Use Cases (Input Ports)
Define the application's capabilities - what it can do:

```kotlin
interface CreateOrderUseCase {
    suspend operator fun invoke(command: CreateOrderCommand): Either<ApplicationError, OrderCreatedDto>
}

interface ProcessPaymentUseCase {
    suspend operator fun invoke(orderId: String, payment: PaymentDto): Either<ApplicationError, PaymentResultDto>
}

interface GetOrderDetailsUseCase {
    suspend operator fun invoke(orderId: String): Either<ApplicationError, OrderDetailsDto>
}
```

### Output Port Interfaces
Define contracts for infrastructure services:

```kotlin
// Database operations
interface OrderPersistencePort {
    suspend fun save(order: Order): Either<ApplicationError, Order>
    suspend fun findById(id: OrderId): Either<ApplicationError, Order?>
    suspend fun findByCustomerId(customerId: CustomerId): Either<ApplicationError, List<Order>>
}

// External services
interface PaymentServicePort {
    suspend fun processPayment(amount: Money, card: CreditCard): Either<ApplicationError, PaymentResult>
    suspend fun refundPayment(paymentId: PaymentId): Either<ApplicationError, RefundResult>
}

// Messaging
interface NotificationPort {
    suspend fun sendEmail(to: Email, subject: String, body: String): Either<ApplicationError, Unit>
    suspend fun sendSms(to: PhoneNumber, message: String): Either<ApplicationError, Unit>
}

// Event publishing
interface EventPublisherPort {
    suspend fun publish(event: DomainEvent): Either<ApplicationError, Unit>
}
```

### Use Case Implementation
```kotlin
class CreateOrderUseCaseImpl(
    private val orderRepository: OrderPersistencePort,
    private val inventoryService: InventoryServicePort,
    private val pricingService: PricingService, // Domain service
    private val eventPublisher: EventPublisherPort
) : CreateOrderUseCase {
    
    override suspend operator fun invoke(
        command: CreateOrderCommand
    ): Either<ApplicationError, OrderCreatedDto> = either {
        // 1. Load domain objects
        val customer = customerRepository.findById(command.customerId)
            .mapLeft { ApplicationError.RepositoryError("Customer not found") }
            .bind()
            
        // 2. Check inventory
        val availableItems = inventoryService.checkAvailability(command.items)
            .bind()
            
        // 3. Apply domain logic
        val order = Order.create(customer, availableItems)
            .mapLeft { ApplicationError.fromDomain(it) }
            .bind()
            
        // 4. Calculate pricing using domain service
        val pricedOrder = pricingService.applyPricing(order, customer)
            .mapLeft { ApplicationError.fromDomain(it) }
            .bind()
            
        // 5. Persist
        val savedOrder = orderRepository.save(pricedOrder)
            .bind()
            
        // 6. Publish event
        eventPublisher.publish(OrderCreatedEvent(savedOrder.id, savedOrder.total))
            .bind()
            
        // 7. Map to DTO
        OrderCreatedDto.fromDomain(savedOrder)
    }
}
```

### Application Services
For operations that span multiple use cases:

```kotlin
class OrderApplicationService(
    private val orderRepository: OrderPersistencePort,
    private val paymentService: PaymentServicePort,
    private val notificationPort: NotificationPort
) {
    suspend fun completeOrderFlow(
        createCommand: CreateOrderCommand,
        paymentDetails: PaymentDto
    ): Either<ApplicationError, OrderCompletedDto> = either {
        // Coordinate multiple use cases
        val order = createOrderUseCase(createCommand).bind()
        val payment = processPaymentUseCase(order.id, paymentDetails).bind()
        val notification = notificationPort.sendEmail(
            order.customerEmail,
            "Order Confirmed",
            buildOrderConfirmationEmail(order, payment)
        ).bind()
        
        OrderCompletedDto(order, payment, notification)
    }
}
```

### Self-Validating DTOs
DTOs can encapsulate their own validation logic for better cohesion:

```kotlin
// Self-validating DTO with validation methods
data class CreateOrderCommand(
    val customerId: String,
    val items: List<OrderItemCommand>,
    val couponCode: String? = null,
    val deliveryInstructions: String? = null
) {
    /**
     * Validates structural correctness of the command.
     * Business rules are still validated in the domain layer.
     */
    fun validate(): Either<ApplicationError, Unit> {
        // Check required fields
        if (customerId.isBlank()) {
            return ApplicationError.ValidationError(
                field = "customerId",
                message = "Customer ID is required"
            ).left()
        }
        
        // Check collection constraints
        if (items.isEmpty()) {
            return ApplicationError.ValidationError(
                field = "items",
                message = "Order must contain at least one item"
            ).left()
        }
        
        if (items.size > 100) {
            return ApplicationError.ValidationError(
                field = "items",
                message = "Order cannot exceed 100 items"
            ).left()
        }
        
        // Validate nested DTOs
        items.forEachIndexed { index, item ->
            item.validate().fold(
                { error -> 
                    return ApplicationError.ValidationError(
                        field = "items[$index]",
                        message = error.message
                    ).left()
                },
                { /* valid */ }
            )
        }
        
        // Format validations
        couponCode?.let { code ->
            if (!code.matches(Regex("[A-Z0-9]{4,12}"))) {
                return ApplicationError.ValidationError(
                    field = "couponCode",
                    message = "Invalid coupon format"
                ).left()
            }
        }
        
        return Unit.right()
    }
    
    /**
     * Transforms to domain object with proper error handling.
     */
    fun toCustomerId(): Either<ApplicationError, CustomerId> =
        CustomerId.create(customerId)
            .mapLeft { ApplicationError.DomainErrorWrapper(it) }
    
    /**
     * Convenience method for checking discount eligibility.
     */
    fun hasDiscount(): Boolean = couponCode != null
}

// Usage in use case becomes cleaner
class CreateOrderUseCaseImpl(
    private val orderRepository: OrderPersistencePort,
    private val couponService: CouponServicePort
) : CreateOrderUseCase {
    
    override suspend operator fun invoke(
        command: CreateOrderCommand
    ): Either<ApplicationError, OrderCreatedDto> = either {
        // First validate the command structure
        command.validate().bind()
        
        // Then transform to domain objects
        val customerId = command.toCustomerId().bind()
        
        // Apply business logic...
    }
}
```

### DTOs and Mappers
Keep domain models isolated:

```kotlin
// Simple DTO for output
data class OrderDto(
    val id: String,
    val customerId: String,
    val items: List<OrderItemDto>,
    val total: BigDecimal,
    val status: String
)

// Mapper for transformations
object OrderMapper {
    fun toDto(order: Order): OrderDto = OrderDto(
        id = order.id.value,
        customerId = order.customerId.value,
        items = order.items.map { OrderItemMapper.toDto(it) },
        total = order.total.amount,
        status = order.status.name
    )
}
```

### Error Handling
```kotlin
sealed class ApplicationError {
    data class ValidationError(val field: String, val message: String) : ApplicationError()
    data class UseCaseError(val useCase: String, val cause: DomainError) : ApplicationError()
    data class RepositoryError(val operation: String, val message: String) : ApplicationError()
    data class ExternalServiceError(val service: String, val message: String) : ApplicationError()
    data class UnauthorizedError(val resource: String) : ApplicationError()
    
    companion object {
        fun fromDomain(error: DomainError): ApplicationError = 
            UseCaseError("Domain", error)
    }
}
```

## Dependencies

- **Domain module**: For domain models, services, and business rules
- **No infrastructure dependencies**: Only defines interfaces (ports)
- **Arrow-kt**: For functional error handling
- **Kotlinx coroutines**: For async operations

## Port Pattern Explained

The Application layer follows the Hexagonal Architecture pattern:

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│                  (Controllers, CLI, GraphQL)                 │
└────────────────────────┬───────────────────────────────────┘
                         │ calls input ports
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Input Ports (Use Cases)                │   │
│  │         Implemented by use case classes             │   │
│  └──────────────────────┬──────────────────────────────┘   │
│                         │ orchestrates                      │
│  ┌──────────────────────▼──────────────────────────────┐   │
│  │                  Domain Layer                        │   │
│  └──────────────────────┬──────────────────────────────┘   │
│                         │ calls output ports               │
│  ┌──────────────────────▼──────────────────────────────┐   │
│  │              Output Port Interfaces                  │   │
│  │         Defined here, implemented in infra          │   │
│  └─────────────────────────────────────────────────────┘   │
└────────────────────────┬───────────────────────────────────┘
                         │ implemented by
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                  Infrastructure Layer                        │
│              (Adapters, Database, External APIs)            │
└─────────────────────────────────────────────────────────────┘
```

## Testing Strategy

### Use Case Tests
Test orchestration logic with mocked output ports:
```kotlin
class CreateOrderUseCaseTest {
    private val orderRepository = mockk<OrderPersistencePort>()
    private val inventoryService = mockk<InventoryServicePort>()
    private val useCase = CreateOrderUseCaseImpl(orderRepository, inventoryService)
    
    @Test
    fun `should create order when inventory available`() {
        // Given
        coEvery { inventoryService.checkAvailability(any()) } returns Right(items)
        coEvery { orderRepository.save(any()) } returns Right(order)
        
        // When
        val result = runBlocking { useCase(command) }
        
        // Then
        result.shouldBeRight()
        coVerify { orderRepository.save(any()) }
    }
}
```

### Integration Tests
Test use cases with real domain objects but mocked infrastructure:
```kotlin
class OrderFlowIntegrationTest {
    @Test
    fun `should complete full order flow`() {
        // Test complete flow with in-memory repositories
    }
}
```

## Best Practices

1. **Keep Use Cases Focused**: One use case per user intent
2. **Transaction Boundaries**: Define clearly in use cases
3. **Error Mapping**: Convert all errors at boundaries
4. **No Business Logic**: Keep it in the domain layer
5. **Thin DTOs**: Simple data structures, no behavior
6. **Test Orchestration**: Focus tests on coordination logic
7. **Async by Default**: Use suspend functions throughout

## Common Pitfalls to Avoid

- Don't put business logic in use cases - only orchestration
- Don't let domain objects leak through DTOs
- Don't create "god" use cases that do too much
- Don't bypass the domain layer for "simple" operations
- Don't mix different concerns in a single use case