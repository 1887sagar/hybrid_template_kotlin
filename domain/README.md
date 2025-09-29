# Domain Module

**Version:** 1.0.0  
**Date:** September 29, 2025
**License:** BSD-3-Clause  
**Copyright:** © 2025 Michael Gardner, A Bit of Help, Inc.  
**Authors:** Michael Gardner  
**Status:** Released

## What is the Domain Module?

The domain module is the beating heart of your application - it contains all the business logic that makes your application valuable. This is where you model your business concepts, rules, and behaviors in pure code, completely isolated from technical concerns.

## Why is This Module Special?

Imagine you could take all the business knowledge from your domain experts and encode it into code that:
- Never needs to know about databases
- Doesn't care if it's called from a REST API or CLI
- Can be tested without any setup or mocks
- Is so clear that a business person could almost read it

That's what the domain module gives you!

## Zero Dependencies Rule

The domain module has **ZERO external dependencies**. This means:
- ✅ Kotlin standard library
- ✅ Arrow-kt for functional programming
- ❌ No Spring, no Ktor, no frameworks
- ❌ No database libraries
- ❌ No HTTP clients
- ❌ No logging frameworks

This independence is your superpower - it keeps your business logic pure and portable.

## What Goes in the Domain Module?

### 1. Value Objects - Your Data Building Blocks

Value objects are immutable pieces of data that are defined by their values, not an ID. They always validate themselves to ensure they're in a valid state.

#### Current Example: PersonName

```kotlin
@JvmInline
value class PersonName private constructor(val value: String) {
    companion object {
        // Smart constructor - ensures valid names only
        fun create(value: String): Either<DomainError.ValidationError, PersonName> {
            val trimmed = value.trim()
            return when {
                trimmed.isBlank() -> 
                    DomainError.ValidationError("name", "Name cannot be blank").left()
                !trimmed.matches(Regex("[a-zA-Z\\s'-]+")) -> 
                    DomainError.ValidationError("name", "Name contains invalid characters").left()
                else -> PersonName(trimmed).right()
            }
        }
        
        // Convenience factory for anonymous users
        fun anonymous(): PersonName = PersonName("Anonymous User")
    }
}

// Usage:
val nameResult = PersonName.create("John O'Brien")
nameResult.fold(
    { error -> println("Invalid: ${error.message}") },
    { name -> println("Welcome, ${name.value}!") }
)
```

#### More Value Object Examples

```kotlin
// Email with validation
@JvmInline
value class Email private constructor(val value: String) {
    companion object {
        private val EMAIL_REGEX = Regex(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        )
        
        fun create(value: String): Either<DomainError, Email> {
            val normalized = value.trim().lowercase()
            return if (EMAIL_REGEX.matches(normalized)) {
                Email(normalized).right()
            } else {
                DomainError.ValidationError("email", "Invalid email format").left()
            }
        }
    }
    
    val domain: String get() = value.substringAfter('@')
    val username: String get() = value.substringBefore('@')
}

// Money that prevents currency mixing
data class Money(
    val amount: BigDecimal,
    val currency: Currency
) {
    init {
        require(amount.scale() <= currency.defaultFractionDigits) {
            "Too many decimal places for ${currency.currencyCode}"
        }
    }
    
    operator fun plus(other: Money): Money {
        require(currency == other.currency) { 
            "Cannot add ${other.currency} to $currency" 
        }
        return Money(amount + other.amount, currency)
    }
    
    operator fun times(multiplier: BigDecimal): Money =
        Money(amount * multiplier, currency)
}

// Physical measurements with units
sealed class Distance {
    abstract val value: Double
    
    data class Meters(override val value: Double) : Distance()
    data class Kilometers(override val value: Double) : Distance()
    data class Miles(override val value: Double) : Distance()
    
    fun toMeters(): Meters = when (this) {
        is Meters -> this
        is Kilometers -> Meters(value * 1000)
        is Miles -> Meters(value * 1609.34)
    }
}
```

### 2. Entities - Objects with Identity

Entities have a unique identity that persists over time. Even if all their attributes change, they're still the same entity.

```kotlin
class Customer(
    val id: CustomerId,  // This never changes
    private var name: PersonName,
    private var email: Email,
    private var status: CustomerStatus = CustomerStatus.ACTIVE
) {
    // Business behavior, not just getters/setters
    fun changeEmail(newEmail: Email): Either<DomainError, EmailChanged> {
        if (email == newEmail) {
            return DomainError.NoChange("Email is already $newEmail").left()
        }
        
        val oldEmail = email
        email = newEmail
        
        return EmailChanged(
            customerId = id,
            oldEmail = oldEmail,
            newEmail = newEmail
        ).right()
    }
    
    fun deactivate(): Either<DomainError, CustomerDeactivated> {
        if (status == CustomerStatus.INACTIVE) {
            return DomainError.InvalidStateTransition(
                "Customer is already inactive"
            ).left()
        }
        
        status = CustomerStatus.INACTIVE
        return CustomerDeactivated(id).right()
    }
}
```

### 3. Aggregates - Consistency Boundaries

Aggregates are clusters of entities and value objects that must remain consistent together.

```kotlin
class ShoppingCart(
    val id: CartId,
    val customerId: CustomerId,
    private val items: MutableList<CartItem> = mutableListOf()
) {
    private var status: CartStatus = CartStatus.ACTIVE
    
    fun addItem(
        product: Product, 
        quantity: Quantity
    ): Either<DomainError, ItemAdded> {
        // Business rule: Can't add to checked-out cart
        if (status != CartStatus.ACTIVE) {
            return DomainError.BusinessRuleViolation(
                "AddToInactiveCart",
                "Cannot add items to ${status.name} cart"
            ).left()
        }
        
        // Business rule: Max 10 of any item
        val existingQuantity = items
            .find { it.productId == product.id }
            ?.quantity ?: Quantity(0)
            
        if (existingQuantity + quantity > Quantity(10)) {
            return DomainError.BusinessRuleViolation(
                "QuantityLimit",
                "Cannot have more than 10 of any item"
            ).left()
        }
        
        // Add or update item
        val existingItem = items.find { it.productId == product.id }
        if (existingItem != null) {
            existingItem.increaseQuantity(quantity)
        } else {
            items.add(CartItem(product.id, product.name, product.price, quantity))
        }
        
        return ItemAdded(id, product.id, quantity).right()
    }
    
    fun checkout(): Either<DomainError, CheckoutStarted> {
        if (items.isEmpty()) {
            return DomainError.BusinessRuleViolation(
                "EmptyCart",
                "Cannot checkout empty cart"
            ).left()
        }
        
        status = CartStatus.CHECKING_OUT
        return CheckoutStarted(id, calculateTotal()).right()
    }
    
    private fun calculateTotal(): Money =
        items.fold(Money.ZERO_USD) { acc, item ->
            acc + (item.price * item.quantity.value.toBigDecimal())
        }
}
```

### 4. Domain Services - Complex Business Operations

When business logic doesn't naturally fit in an entity, use a domain service.

```kotlin
class PricingService(
    private val discountPolicy: DiscountPolicy,
    private val taxCalculator: TaxCalculator
) {
    fun calculateOrderTotal(
        items: List<OrderItem>,
        customer: Customer,
        shippingAddress: Address
    ): Either<DomainError, OrderPricing> {
        // Base total
        val subtotal = items.fold(Money.ZERO_USD) { acc, item ->
            acc + item.totalPrice()
        }
        
        // Apply discounts based on customer tier
        val discount = discountPolicy.calculateDiscount(customer, items)
        val discountedTotal = subtotal - discount
        
        // Calculate tax based on shipping address
        val tax = taxCalculator.calculateTax(discountedTotal, shippingAddress)
        
        // Final total
        val total = discountedTotal + tax
        
        return OrderPricing(
            subtotal = subtotal,
            discount = discount,
            tax = tax,
            total = total
        ).right()
    }
}
```

### 5. Domain Events - Things That Happened

Domain events capture important business moments.

```kotlin
sealed class DomainEvent {
    abstract val aggregateId: String
    abstract val occurredAt: Instant
    abstract val eventId: EventId
}

data class OrderPlaced(
    override val aggregateId: String,
    val customerId: CustomerId,
    val items: List<OrderItem>,
    val total: Money,
    override val occurredAt: Instant = Clock.System.now(),
    override val eventId: EventId = EventId.generate()
) : DomainEvent()

data class PaymentProcessed(
    override val aggregateId: String,
    val amount: Money,
    val paymentMethod: PaymentMethod,
    override val occurredAt: Instant = Clock.System.now(),
    override val eventId: EventId = EventId.generate()
) : DomainEvent()

// Aggregate that emits events
class Order(
    val id: OrderId,
    private var status: OrderStatus = OrderStatus.PENDING
) {
    private val events = mutableListOf<DomainEvent>()
    
    fun place(items: List<OrderItem>, total: Money): Either<DomainError, Unit> {
        if (status != OrderStatus.PENDING) {
            return DomainError.InvalidStateTransition(
                "Can only place pending orders"
            ).left()
        }
        
        status = OrderStatus.PLACED
        events.add(OrderPlaced(id.value, customerId, items, total))
        return Unit.right()
    }
    
    fun getUncommittedEvents(): List<DomainEvent> = events.toList()
    fun markEventsAsCommitted() = events.clear()
}
```

### 6. Policies - Business Rules

Encapsulate complex business rules in policy objects.

```kotlin
object ShippingPolicy {
    fun calculateShippingCost(
        items: List<OrderItem>,
        destination: Address,
        customerTier: CustomerTier
    ): Money {
        val totalWeight = items.sumOf { it.weight.toKilograms() }
        
        return when {
            // VIP customers get free shipping
            customerTier == CustomerTier.VIP -> Money.ZERO_USD
            
            // Free shipping over $100
            items.totalValue() > Money(100, USD) -> Money.ZERO_USD
            
            // Express shipping to certain zones
            destination.isExpressZone() -> Money(25, USD)
            
            // Standard shipping by weight
            totalWeight < 5 -> Money(10, USD)
            totalWeight < 20 -> Money(20, USD)
            else -> Money(50, USD)
        }
    }
}

object ReturnPolicy {
    private const val STANDARD_RETURN_DAYS = 30
    private const val EXTENDED_RETURN_DAYS = 90
    
    fun canReturn(
        order: Order,
        item: OrderItem,
        currentDate: LocalDate
    ): Either<DomainError, ReturnAuthorization> {
        val daysSincePurchase = ChronoUnit.DAYS.between(
            order.placedDate, 
            currentDate
        )
        
        val returnWindow = when (item.category) {
            ProductCategory.ELECTRONICS -> EXTENDED_RETURN_DAYS
            ProductCategory.CLOTHING -> STANDARD_RETURN_DAYS
            ProductCategory.FOOD -> 0 // No returns
            else -> STANDARD_RETURN_DAYS
        }
        
        return when {
            returnWindow == 0 -> DomainError.BusinessRuleViolation(
                "NoReturns",
                "${item.category} items cannot be returned"
            ).left()
            
            daysSincePurchase > returnWindow -> DomainError.BusinessRuleViolation(
                "ReturnWindowExpired",
                "Return window of $returnWindow days has expired"
            ).left()
            
            else -> ReturnAuthorization(
                orderId = order.id,
                itemId = item.id,
                reason = "Customer requested",
                expiresAt = currentDate.plusDays(7)
            ).right()
        }
    }
}
```

## Testing Your Domain Logic

Domain tests should be simple, fast, and focused on business behavior:

```kotlin
class ShoppingCartTest : DescribeSpec({
    
    describe("Adding items to cart") {
        it("should add new item successfully") {
            // Given
            val cart = ShoppingCart(CartId("123"), CustomerId("456"))
            val product = Product(
                ProductId("P1"), 
                "Laptop", 
                Money(999.99, USD)
            )
            
            // When
            val result = cart.addItem(product, Quantity(1))
            
            // Then
            result.shouldBeRight()
            cart.itemCount shouldBe 1
        }
        
        it("should prevent adding more than 10 of an item") {
            // Given
            val cart = ShoppingCart(CartId("123"), CustomerId("456"))
            val product = Product(ProductId("P1"), "Mouse", Money(29.99, USD))
            cart.addItem(product, Quantity(8))
            
            // When
            val result = cart.addItem(product, Quantity(3))
            
            // Then
            result.shouldBeLeft()
            val error = result.leftOrNull() as DomainError.BusinessRuleViolation
            error.rule shouldBe "QuantityLimit"
        }
    }
    
    describe("Checkout process") {
        it("should calculate total correctly with multiple items") {
            // Property-based test
            checkAll(
                Arb.list(Arb.money(1.0..1000.0), 1..10)
            ) { prices ->
                val cart = createCartWithItems(prices)
                val expectedTotal = prices.reduce { a, b -> a + b }
                
                cart.checkout().map { event ->
                    event.total shouldBe expectedTotal
                }
            }
        }
    }
})
```

## Common Pitfalls and How to Avoid Them

### ❌ Anemic Domain Model
```kotlin
// BAD: Just a data bag
data class Product(
    val id: String,
    val name: String,
    val price: Double
)

// Service with all the logic
class ProductService {
    fun applyDiscount(product: Product, percent: Int): Product {
        return product.copy(price = product.price * (1 - percent/100.0))
    }
}
```

### ✅ Rich Domain Model
```kotlin
// GOOD: Behavior with the data
class Product(
    val id: ProductId,
    val name: ProductName,
    private var price: Money
) {
    fun applyDiscount(discount: Percentage): Either<DomainError, DiscountApplied> {
        if (discount > Percentage(50)) {
            return DomainError.BusinessRuleViolation(
                "ExcessiveDiscount",
                "Cannot apply more than 50% discount"
            ).left()
        }
        
        val oldPrice = price
        price = price * (1 - discount.asDecimal())
        
        return DiscountApplied(id, oldPrice, price, discount).right()
    }
}
```

### ❌ Leaking Technical Concerns
```kotlin
// BAD: Database concerns in domain
class User {
    @Id
    @GeneratedValue
    var id: Long? = null
    
    @Column(name = "user_name")
    var username: String? = null
    
    fun save() {
        database.persist(this) // NO! Domain shouldn't know about DB
    }
}
```

### ✅ Pure Domain Model
```kotlin
// GOOD: Pure business logic
class User(
    val id: UserId,
    private var username: Username,
    private var email: Email
) {
    fun changeUsername(
        newUsername: Username
    ): Either<DomainError, UsernameChanged> {
        if (username == newUsername) {
            return DomainError.NoChange("Username is already $newUsername").left()
        }
        
        val oldUsername = username
        username = newUsername
        
        return UsernameChanged(id, oldUsername, newUsername).right()
    }
}
```

## Module Structure

```
domain/
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── com/abitofhelp/hybrid/domain/
│   │           ├── error/          # Domain errors
│   │           │   └── DomainError.kt
│   │           ├── event/          # Domain events
│   │           │   └── DomainEvent.kt
│   │           ├── model/          # Aggregates and entities
│   │           │   ├── customer/
│   │           │   ├── order/
│   │           │   └── product/
│   │           ├── policy/         # Business policies
│   │           │   ├── PricingPolicy.kt
│   │           │   └── ShippingPolicy.kt
│   │           ├── service/        # Domain services
│   │           │   └── GreetingService.kt
│   │           └── value/          # Value objects
│   │               ├── PersonName.kt
│   │               ├── Email.kt
│   │               └── Money.kt
│   └── test/
│       └── kotlin/
│           └── com/abitofhelp/hybrid/domain/
│               ├── model/          # Entity/aggregate tests
│               ├── policy/         # Policy tests
│               ├── service/        # Service tests
│               └── value/          # Value object tests
```

## Quick Reference

### Creating a Value Object
```kotlin
@JvmInline
value class YourValueObject private constructor(val value: String) {
    companion object {
        fun create(value: String): Either<DomainError, YourValueObject> {
            // Validate
            // Return Either.Right or Either.Left
        }
    }
}
```

### Creating an Entity
```kotlin
class YourEntity(
    val id: EntityId,
    private var state: State
) {
    fun performAction(): Either<DomainError, DomainEvent> {
        // Check business rules
        // Update state
        // Return event or error
    }
}
```

### Creating a Domain Service
```kotlin
class YourDomainService {
    fun complexOperation(
        /* domain objects */
    ): Either<DomainError, Result> {
        // Orchestrate domain logic
        // No I/O, no side effects
    }
}
```

## Remember

The domain module is your business logic sanctuary. Keep it:
- **Pure**: No frameworks, no I/O
- **Rich**: Behavior with data
- **Validated**: Fail fast with clear errors
- **Testable**: No mocks needed
- **Readable**: Business people should understand it

Your domain model is the most important part of your application - treat it with care!