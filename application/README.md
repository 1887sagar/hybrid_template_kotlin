# Application Module

**Version:** 1.0.0  
**Date:** September 29, 2025
**License:** BSD-3-Clause  
**Copyright:** © 2025 Michael Gardner, A Bit of Help, Inc.  
**Authors:** Michael Gardner  
**Status:** Released

## What is the Application Module?

Think of the application module as the "coordinator" or "conductor" of your application. It doesn't contain business rules (that's the domain layer) and it doesn't know about databases or web servers (that's infrastructure). Instead, it orchestrates - it tells other parts what to do and when.

## The Orchestra Metaphor

Imagine an orchestra:
- **Domain Layer** = The musicians (they know how to play)
- **Application Layer** = The conductor (coordinates the performance)
- **Infrastructure Layer** = The instruments (the tools used)
- **Presentation Layer** = The audience interface (concert hall)

The conductor doesn't play instruments but knows when each section should play!

## Core Components Explained

### Use Cases - Your Application's Capabilities

A use case represents one thing your application can do. Each use case is like a recipe that describes the steps to accomplish a specific goal.

#### Real Example: Creating a Greeting

Let's look at our `CreateGreetingUseCase`:

```kotlin
class CreateGreetingUseCase(
    private val greetingService: GreetingService,     // From domain layer
    private val outputPort: OutputPort,               // Interface we define
) : CreateGreetingInputPort {
    
    override suspend fun execute(
        command: CreateGreetingCommand
    ): Either<ApplicationError, GreetingResult> {
        
        // Step 1: Transform input into domain objects
        val nameResult = if (command.name.isNullOrBlank()) {
            Either.Right(PersonName.anonymous())
        } else {
            PersonName.create(command.name)
                .mapLeft { ApplicationError.DomainErrorWrapper(it) }
        }
        
        // Step 2: Use domain logic to create greeting
        return nameResult.flatMap { personName ->
            greetingService.createGreeting(personName)
                .mapLeft { ApplicationError.DomainErrorWrapper(it) }
                .flatMap { greeting ->
                    // Step 3: Send output through port
                    if (!command.silent) {
                        outputPort.send(greeting)
                            .map { GreetingResult(greeting, personName.value) }
                    } else {
                        Either.Right(GreetingResult(greeting, personName.value))
                    }
                }
        }
    }
}
```

**What's happening here?**
1. Takes raw input from the user
2. Converts it to domain objects (with validation)
3. Calls domain services to do the actual work
4. Sends results to output
5. Returns success or error

### Commands - Instructions for Use Cases

Commands are simple data classes that carry the information needed to execute a use case:

```kotlin
data class CreateGreetingCommand(
    val name: String? = null,      // Optional - can greet anonymously
    val silent: Boolean = false,   // Flag to suppress output
)
```

**Why use commands?**
- Clear contract of what input is needed
- Easy to validate
- Great for testing
- Can be queued or logged

### DTOs - Data Transfer Objects

DTOs are simple containers for moving data between layers. They're like shipping boxes - they don't care what's inside, just that it gets from A to B safely.

#### Input DTO (Command)
```kotlin
data class RegisterUserCommand(
    val email: String,
    val password: String,
    val name: String,
    val acceptedTerms: Boolean,
)
```

#### Output DTO (Result)
```kotlin
data class UserRegistrationResult(
    val userId: String,
    val email: String,
    val welcomeMessage: String,
)
```

**Important:** DTOs use simple types (String, Int, Boolean), not domain objects!

### Ports - Defining What We Need

Ports are interfaces that define what the application needs from the outside world. They're like electrical outlets - they define the shape of the plug, not what powers it.

#### Input Port (What Others Can Ask Us to Do)
```kotlin
interface CreateOrderInputPort {
    suspend fun execute(
        command: CreateOrderCommand
    ): Either<ApplicationError, OrderResult>
}
```

#### Output Port (What We Need Others to Do)
```kotlin
interface EmailPort {
    suspend fun sendEmail(
        to: String,
        subject: String,
        body: String
    ): Either<ApplicationError, Unit>
}
```

The beauty? The application doesn't know if emails are sent via SMTP, API, or carrier pigeon!

## Real-World Example: E-Commerce Order Processing

Let's build a more complex example - processing an order:

### Step 1: Define the Command

```kotlin
data class ProcessOrderCommand(
    val customerId: String,
    val items: List<OrderItemDto>,
    val shippingAddress: AddressDto,
    val paymentMethod: String,
)

data class OrderItemDto(
    val productId: String,
    val quantity: Int,
)

data class AddressDto(
    val street: String,
    val city: String,
    val postalCode: String,
    val country: String,
)
```

### Step 2: Define Required Ports

```kotlin
// What we need from infrastructure
interface InventoryPort {
    suspend fun checkAvailability(
        productId: String, 
        quantity: Int
    ): Either<ApplicationError, Boolean>
    
    suspend fun reserveItems(
        items: List<OrderItemDto>
    ): Either<ApplicationError, ReservationId>
}

interface PaymentPort {
    suspend fun processPayment(
        amount: Money,
        method: PaymentMethod
    ): Either<ApplicationError, PaymentConfirmation>
}

interface OrderRepositoryPort {
    suspend fun save(order: Order): Either<ApplicationError, Order>
}

interface NotificationPort {
    suspend fun sendOrderConfirmation(
        email: String,
        orderDetails: OrderResult
    ): Either<ApplicationError, Unit>
}
```

### Step 3: Implement the Use Case

```kotlin
class ProcessOrderUseCase(
    private val inventoryPort: InventoryPort,
    private val paymentPort: PaymentPort,
    private val orderRepository: OrderRepositoryPort,
    private val notificationPort: NotificationPort,
    private val pricingService: PricingService,  // Domain service
) : ProcessOrderInputPort {
    
    override suspend fun execute(
        command: ProcessOrderCommand
    ): Either<ApplicationError, OrderResult> {
        
        // 1. Check inventory for all items
        val availabilityChecks = command.items.map { item ->
            inventoryPort.checkAvailability(item.productId, item.quantity)
        }
        
        // If any item unavailable, fail fast
        val allAvailable = availabilityChecks.all { it.isRight() }
        if (!allAvailable) {
            return ApplicationError.BusinessRuleError(
                "ItemsUnavailable",
                "Some items are out of stock"
            ).left()
        }
        
        // 2. Calculate total price (domain logic)
        val orderItems = createOrderItems(command.items)  // Convert DTOs to domain
        val totalPrice = pricingService.calculateTotal(orderItems)
        
        // 3. Process payment
        val paymentResult = paymentPort.processPayment(
            totalPrice,
            PaymentMethod.parse(command.paymentMethod)
        )
        
        return paymentResult.flatMap { confirmation ->
            // 4. Reserve inventory
            inventoryPort.reserveItems(command.items).flatMap { reservationId ->
                // 5. Create and save order
                val order = Order.create(
                    customerId = CustomerId(command.customerId),
                    items = orderItems,
                    totalPrice = totalPrice,
                    paymentConfirmation = confirmation,
                    reservationId = reservationId
                )
                
                orderRepository.save(order).flatMap { savedOrder ->
                    // 6. Send confirmation (don't fail order if email fails)
                    val orderResult = OrderResult(
                        orderId = savedOrder.id.value,
                        total = savedOrder.total.format(),
                        estimatedDelivery = calculateDeliveryDate()
                    )
                    
                    // Send email but don't fail if it doesn't work
                    notificationPort.sendOrderConfirmation(
                        command.customerEmail,
                        orderResult
                    ).recover { 
                        // Log error but continue
                        logger.warn("Failed to send confirmation email", it)
                        Unit 
                    }
                    
                    Either.Right(orderResult)
                }
            }
        }
    }
}
```

## Error Handling Patterns

The application layer is responsible for translating errors into something meaningful for users:

### Domain to Application Error Translation

```kotlin
sealed class ApplicationError {
    abstract val userMessage: String
    
    data class ValidationError(
        val field: String,
        val issue: String
    ) : ApplicationError() {
        override val userMessage = "Invalid $field: $issue"
    }
    
    data class BusinessRuleError(
        val rule: String,
        val reason: String
    ) : ApplicationError() {
        override val userMessage = reason
    }
    
    data class ExternalServiceError(
        val service: String,
        val cause: String
    ) : ApplicationError() {
        override val userMessage = "$service is currently unavailable. Please try again later."
    }
    
    data class DomainErrorWrapper(
        val domainError: DomainError
    ) : ApplicationError() {
        override val userMessage = when (domainError) {
            is DomainError.ValidationError -> 
                "Invalid input: ${domainError.message}"
            is DomainError.BusinessRuleViolation -> 
                "Cannot proceed: ${domainError.reason}"
            is DomainError.NotFound -> 
                "${domainError.entity} not found"
        }
    }
}
```

### Graceful Error Handling

```kotlin
// Don't let non-critical failures break the flow
suspend fun processWithFallback(): Either<ApplicationError, Result> {
    return primaryService.process()
        .recover { error ->
            // Try fallback
            fallbackService.process()
                .getOrElse { 
                    // Last resort - return degraded result
                    Result.partial("Some features unavailable")
                }
        }
}
```

## Testing Strategies

Application layer tests focus on orchestration, not business logic:

### Example: Testing Order Processing

```kotlin
class ProcessOrderUseCaseTest : DescribeSpec({
    
    // Create mocks for all dependencies
    val inventoryPort = mockk<InventoryPort>()
    val paymentPort = mockk<PaymentPort>()
    val orderRepository = mockk<OrderRepositoryPort>()
    val notificationPort = mockk<NotificationPort>()
    val pricingService = mockk<PricingService>()
    
    val useCase = ProcessOrderUseCase(
        inventoryPort, paymentPort, orderRepository, 
        notificationPort, pricingService
    )
    
    describe("Order Processing") {
        it("should process order successfully") {
            // Given - Set up the scenario
            val command = ProcessOrderCommand(
                customerId = "CUST123",
                items = listOf(OrderItemDto("PROD1", 2)),
                shippingAddress = testAddress(),
                paymentMethod = "CREDIT_CARD"
            )
            
            every { 
                inventoryPort.checkAvailability(any(), any()) 
            } returns true.right()
            
            every { 
                pricingService.calculateTotal(any()) 
            } returns Money(BigDecimal("99.99"), Currency.getInstance("USD"))
            
            every { 
                paymentPort.processPayment(any(), any()) 
            } returns PaymentConfirmation("PAY123").right()
            
            every { 
                inventoryPort.reserveItems(any()) 
            } returns ReservationId("RES123").right()
            
            every { 
                orderRepository.save(any()) 
            } returns testOrder().right()
            
            every { 
                notificationPort.sendOrderConfirmation(any(), any()) 
            } returns Unit.right()
            
            // When
            val result = runBlocking { useCase.execute(command) }
            
            // Then
            result.shouldBeRight()
            result.value.orderId shouldBe "ORDER123"
            
            // Verify the flow
            verify(exactly = 1) { inventoryPort.checkAvailability("PROD1", 2) }
            verify(exactly = 1) { paymentPort.processPayment(any(), any()) }
            verify(exactly = 1) { orderRepository.save(any()) }
            verify(exactly = 1) { notificationPort.sendOrderConfirmation(any(), any()) }
        }
        
        it("should handle payment failure") {
            // Given
            every { inventoryPort.checkAvailability(any(), any()) } returns true.right()
            every { pricingService.calculateTotal(any()) } returns testPrice()
            every { 
                paymentPort.processPayment(any(), any()) 
            } returns ApplicationError.ExternalServiceError(
                "PaymentGateway",
                "Connection timeout"
            ).left()
            
            // When
            val result = runBlocking { useCase.execute(testCommand()) }
            
            // Then
            result.shouldBeLeft()
            result.leftOrNull()!!.userMessage shouldContain "unavailable"
            
            // Verify we didn't proceed after payment failure
            verify(exactly = 0) { orderRepository.save(any()) }
        }
    }
})
```

## Common Patterns and Solutions

### Pattern: Saga/Compensation

When operations span multiple services, implement compensation for failures:

```kotlin
class TransferMoneyUseCase(
    private val accountRepository: AccountRepositoryPort,
    private val auditLog: AuditLogPort
) {
    suspend fun execute(
        command: TransferMoneyCommand
    ): Either<ApplicationError, TransferResult> {
        
        // Track what we've done for rollback
        var sourceDebited = false
        
        return either {
            // 1. Load accounts
            val source = accountRepository.findById(command.sourceId).bind()
            val target = accountRepository.findById(command.targetId).bind()
            
            // 2. Debit source
            val debitedSource = source.debit(command.amount).bind()
            accountRepository.save(debitedSource).bind()
            sourceDebited = true
            
            // 3. Credit target (might fail!)
            try {
                val creditedTarget = target.credit(command.amount).bind()
                accountRepository.save(creditedTarget).bind()
                
                // 4. Log success
                auditLog.logTransfer(command).bind()
                
                TransferResult(
                    transactionId = generateId(),
                    amount = command.amount.format()
                )
            } catch (e: Exception) {
                // Compensation: Reverse the debit
                if (sourceDebited) {
                    val revertedSource = debitedSource.credit(command.amount).bind()
                    accountRepository.save(revertedSource).bind()
                }
                raise(ApplicationError.TransferFailed(e.message ?: "Unknown error"))
            }
        }
    }
}
```

### Pattern: Batch Operations

Process multiple items efficiently:

```kotlin
class BulkUpdateUseCase(
    private val repository: ItemRepositoryPort,
    private val validator: ItemValidator
) {
    suspend fun execute(
        command: BulkUpdateCommand
    ): Either<ApplicationError, BulkUpdateResult> {
        
        // Validate all items first
        val validationResults = command.items.map { item ->
            validator.validate(item).mapLeft { error ->
                BatchValidationError(item.id, error)
            }
        }
        
        // Check if any failed
        val failures = validationResults.filterIsInstance<Either.Left<*>>()
        if (failures.isNotEmpty()) {
            return ApplicationError.BatchValidationFailed(
                failures.map { it.leftOrNull()!! }
            ).left()
        }
        
        // Process in parallel for efficiency
        val updateResults = coroutineScope {
            command.items.map { item ->
                async {
                    repository.update(item)
                }
            }.awaitAll()
        }
        
        // Return summary
        return BulkUpdateResult(
            totalProcessed = updateResults.size,
            successful = updateResults.count { it.isRight() },
            failed = updateResults.count { it.isLeft() }
        ).right()
    }
}
```

### Pattern: Query with Filters

Handle complex queries cleanly:

```kotlin
class SearchProductsUseCase(
    private val productRepository: ProductRepositoryPort,
    private val pricingService: PricingService
) {
    suspend fun execute(
        query: SearchProductsQuery
    ): Either<ApplicationError, SearchResult> {
        
        // Build search criteria from query
        val criteria = SearchCriteria(
            keywords = query.keywords?.split(" ") ?: emptyList(),
            priceRange = query.minPrice?.let { min ->
                query.maxPrice?.let { max ->
                    PriceRange(Money(min), Money(max))
                }
            },
            categories = query.categories ?: emptyList(),
            inStock = query.inStockOnly
        )
        
        // Execute search
        return productRepository.search(criteria)
            .flatMap { products ->
                // Enrich with current pricing
                val enrichedProducts = products.map { product ->
                    val currentPrice = pricingService.getCurrentPrice(product.id)
                    ProductDto(
                        id = product.id.value,
                        name = product.name.value,
                        price = currentPrice.format(),
                        available = product.stock > 0
                    )
                }
                
                SearchResult(
                    products = enrichedProducts,
                    totalCount = enrichedProducts.size,
                    facets = buildFacets(products)
                ).right()
            }
    }
}
```

## Best Practices Checklist

### ✅ DO:
- Keep use cases focused on one operation
- Transform errors to be user-friendly
- Use dependency injection via constructor
- Test orchestration, not business logic
- Handle partial failures gracefully
- Use coroutines for concurrent operations

### ❌ DON'T:
- Put business logic in use cases
- Expose domain objects in DTOs
- Let infrastructure exceptions bubble up
- Create "god" use cases that do everything
- Ignore error cases
- Block threads with synchronous calls

## Adding Your Own Use Cases

Here's a template to get started:

```kotlin
// 1. Define your command
data class YourCommand(
    // Input fields
)

// 2. Define your result
data class YourResult(
    // Output fields
)

// 3. Define your input port
interface YourUseCaseInputPort {
    suspend fun execute(
        command: YourCommand
    ): Either<ApplicationError, YourResult>
}

// 4. Implement your use case
class YourUseCase(
    // Inject dependencies
) : YourUseCaseInputPort {
    
    override suspend fun execute(
        command: YourCommand
    ): Either<ApplicationError, YourResult> {
        return either {
            // Step 1: Validate and transform input
            
            // Step 2: Call domain logic
            
            // Step 3: Perform side effects
            
            // Step 4: Return result
        }
    }
}

// 5. Wire in bootstrap module
class CompositionRoot {
    fun createYourUseCase(): YourUseCaseInputPort {
        return YourUseCase(
            // Provide real dependencies
        )
    }
}
```

## Module Structure

```
application/
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── com/abitofhelp/hybrid/application/
│   │           ├── coroutines/         # Coroutine handlers
│   │           ├── dto/                # Data transfer objects
│   │           ├── error/              # Error definitions
│   │           ├── port/               # Port interfaces
│   │           │   ├── input/          # Use case interfaces
│   │           │   └── output/         # Infrastructure interfaces
│   │           └── usecase/            # Use case implementations
│   └── test/
│       └── kotlin/
│           └── com/abitofhelp/hybrid/application/
│               ├── dto/                # DTO tests
│               ├── usecase/            # Use case tests
│               └── integration/        # Integration tests
└── build.gradle.kts
```

## Troubleshooting

### Problem: "Use case is getting too complex"
**Solution**: Break it into smaller use cases that can be composed together.

### Problem: "Too many dependencies in constructor"
**Solution**: Consider if the use case is doing too much. Each use case should have a single responsibility.

### Problem: "Domain objects appearing in DTOs"
**Solution**: Always map domain objects to DTOs. Never expose domain internals.

### Problem: "Tests are testing domain logic"
**Solution**: Mock domain services. Test only the orchestration logic.

## Summary

The application layer is your orchestration layer. It:

- **Coordinates** the flow of operations
- **Transforms** data between layers  
- **Handles** errors gracefully
- **Defines** what it needs via ports
- **Remains** ignorant of implementation details

Remember: If you're writing business rules, you're in the wrong layer! The application layer orchestrates; it doesn't decide.