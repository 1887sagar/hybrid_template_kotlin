# Domain Layer Documentation

**Version:** 1.0.0  
**Date:** January 2025  
**License:** BSD-3-Clause  
**Copyright:** © 2025 Michael Gardner, A Bit of Help, Inc.  
**Authors:** Michael Gardner  
**Status:** Released

## What is the Domain Layer?

The domain layer is the heart of your application - it contains all the business logic that makes your application unique. Think of it as the "brain" that knows all the rules and decisions your business needs to make.

## Why is it Special?

The domain layer has one superpower: **it has zero dependencies**. This means:
- No frameworks
- No databases
- No web servers
- No external libraries (except Kotlin standard library)

This independence makes your business logic:
- Easy to test
- Easy to understand
- Easy to change
- Easy to reuse

## Core Concepts Explained

### Value Objects - Your Building Blocks

Value objects are like LEGO blocks for your domain. They represent simple concepts that are defined by their values, not by an ID.

#### Example: PersonName

Let's look at a real example from our codebase:

```kotlin
@JvmInline
value class PersonName private constructor(val value: String) {
    companion object {
        private val NAME_PATTERN = Regex("[a-zA-Z\\s'-]+")
        
        fun create(value: String): Either<DomainError.ValidationError, PersonName> {
            val trimmed = value.trim()
            return when {
                trimmed.isBlank() -> 
                    DomainError.ValidationError("name", "Name cannot be blank").left()
                !trimmed.matches(NAME_PATTERN) -> 
                    DomainError.ValidationError("name", "Invalid characters in name").left()
                else -> PersonName(trimmed).right()
            }
        }
        
        fun anonymous(): PersonName = PersonName("Anonymous User")
    }
}
```

**What's happening here?**
1. `private constructor` - You can't create invalid names
2. `create()` method - The only way to create a name, with validation
3. `NAME_PATTERN` - Defines what characters are allowed
4. `Either` return type - Returns either an error or a valid name

**Why do this instead of using String?**
```kotlin
// Without value objects (prone to errors):
fun greet(name: String, email: String, phone: String) {
    // Which string is which? Easy to mix up!
}

// With value objects (type-safe):
fun greet(name: PersonName, email: EmailAddress, phone: PhoneNumber) {
    // Compiler won't let you mix these up!
}
```

### Domain Services - Your Business Logic

Domain services contain business logic that doesn't belong to a single entity. Think of them as the "workers" that perform operations.

#### Example: GreetingService

```kotlin
interface GreetingService {
    suspend fun createGreeting(name: PersonName): Either<DomainError, String>
}

class DefaultGreetingService : GreetingService {
    override suspend fun createGreeting(name: PersonName): Either<DomainError, String> {
        return when (GreetingPolicy.determineGreetingFormat(name)) {
            GreetingFormat.DEFAULT -> "Hello, ${name.value}!".right()
            GreetingFormat.FRIENDLY -> "Hey there, ${name.value}! Welcome!".right()
            GreetingFormat.FORMAL -> "Good day, ${name.value}. How may I assist you?".right()
        }
    }
}
```

**Key points:**
- `interface` defines what the service does
- Implementation contains the actual logic
- Uses `suspend` for async operations
- Returns `Either` for error handling

### Business Policies - Your Rule Book

Policies encapsulate business rules and decisions. They answer questions like "How should we handle this situation?"

#### Example: GreetingPolicy

```kotlin
object GreetingPolicy {
    private const val LONG_NAME_THRESHOLD = 20
    private const val VIP_NAME_PREFIX = "Dr."
    
    fun determineGreetingFormat(name: PersonName): GreetingFormat {
        return when {
            name.value.startsWith(VIP_NAME_PREFIX) -> GreetingFormat.FORMAL
            name.value.length > LONG_NAME_THRESHOLD -> GreetingFormat.FORMAL
            name.value == "Anonymous User" -> GreetingFormat.FRIENDLY
            else -> GreetingFormat.DEFAULT
        }
    }
}
```

**This policy decides:**
- VIPs get formal greetings
- Long names get formal treatment
- Anonymous users get friendly greetings
- Everyone else gets default

### Domain Errors - What Can Go Wrong?

Instead of throwing exceptions, we use sealed classes to represent all possible errors:

```kotlin
sealed class DomainError {
    data class ValidationError(val field: String, val message: String) : DomainError()
    data class BusinessRuleViolation(val rule: String, val reason: String) : DomainError()
    data class NotFound(val entity: String, val id: String) : DomainError()
}
```

**Why sealed classes?**
- Compiler knows all possible errors
- Forces you to handle all cases
- No surprise exceptions at runtime

## Real-World Examples

### Example 1: Money Value Object

Here's how you might model money in your domain:

```kotlin
data class Money(
    val amount: BigDecimal,
    val currency: Currency
) {
    init {
        require(amount >= BigDecimal.ZERO) { "Amount cannot be negative" }
        require(amount.scale() <= currency.defaultFractionDigits) { 
            "Too many decimal places for ${currency.currencyCode}" 
        }
    }
    
    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Cannot add different currencies" }
        return Money(amount + other.amount, currency)
    }
    
    fun format(): String = NumberFormat.getCurrencyInstance().apply {
        currency = this@Money.currency
    }.format(amount)
}
```

**Features:**
- Can't create negative money
- Can't mix currencies
- Proper decimal precision
- Easy formatting

### Example 2: Email Address with Validation

```kotlin
@JvmInline
value class EmailAddress private constructor(val value: String) {
    companion object {
        private val EMAIL_REGEX = Regex(
            "[a-zA-Z0-9+._%\\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+"
        )
        
        fun create(value: String): Either<DomainError.ValidationError, EmailAddress> {
            val trimmed = value.trim().lowercase()
            return when {
                trimmed.isBlank() -> 
                    DomainError.ValidationError("email", "Email cannot be blank").left()
                !EMAIL_REGEX.matches(trimmed) -> 
                    DomainError.ValidationError("email", "Invalid email format").left()
                else -> EmailAddress(trimmed).right()
            }
        }
    }
    
    val domain: String get() = value.substringAfter('@')
    val localPart: String get() = value.substringBefore('@')
}
```

### Example 3: Business Rule Implementation

```kotlin
object DiscountPolicy {
    fun calculateDiscount(
        orderTotal: Money,
        customerType: CustomerType,
        promoCode: PromoCode?
    ): Either<DomainError, Discount> {
        // Check business rules
        if (orderTotal.amount < BigDecimal(10)) {
            return DomainError.BusinessRuleViolation(
                "MinimumOrderAmount",
                "Order must be at least $10 for discounts"
            ).left()
        }
        
        val baseDiscount = when (customerType) {
            CustomerType.NEW -> Percentage(5)
            CustomerType.REGULAR -> Percentage(10)
            CustomerType.VIP -> Percentage(20)
        }
        
        val promoDiscount = promoCode?.let { validatePromoCode(it) } ?: Percentage(0)
        
        val totalDiscount = (baseDiscount + promoDiscount).coerceAtMost(Percentage(30))
        
        return Discount(totalDiscount, "Customer + Promo discount").right()
    }
}
```

## Common Patterns and Solutions

### Pattern: Smart Constructors

Always validate input when creating domain objects:

```kotlin
// DON'T: Public constructor allows invalid states
class Age(val value: Int) // Could be negative!

// DO: Factory method ensures validity
@JvmInline
value class Age private constructor(val value: Int) {
    companion object {
        fun create(value: Int): Either<DomainError, Age> {
            return when {
                value < 0 -> DomainError.ValidationError("age", "Age cannot be negative").left()
                value > 150 -> DomainError.ValidationError("age", "Age seems unrealistic").left()
                else -> Age(value).right()
            }
        }
    }
}
```

### Pattern: Rich Domain Models

Put behavior in your domain objects:

```kotlin
// DON'T: Anemic model (just data, no behavior)
data class BankAccount(
    val id: AccountId,
    val balance: Money
)

// Somewhere else:
fun withdraw(account: BankAccount, amount: Money): BankAccount {
    // Logic here
}

// DO: Rich model (data + behavior)
data class BankAccount(
    val id: AccountId,
    private val balance: Money
) {
    fun withdraw(amount: Money): Either<DomainError, BankAccount> {
        return when {
            amount.amount <= BigDecimal.ZERO -> 
                DomainError.ValidationError("amount", "Amount must be positive").left()
            balance < amount -> 
                DomainError.BusinessRuleViolation("InsufficientFunds", "Not enough money").left()
            else -> 
                copy(balance = balance - amount).right()
        }
    }
    
    fun deposit(amount: Money): Either<DomainError, BankAccount> {
        require(amount.currency == balance.currency) { "Currency mismatch" }
        return copy(balance = balance + amount).right()
    }
}
```

### Pattern: Domain Events

Capture important business moments:

```kotlin
sealed class OrderEvent {
    abstract val orderId: OrderId
    abstract val occurredAt: Instant
    
    data class OrderPlaced(
        override val orderId: OrderId,
        override val occurredAt: Instant,
        val customerId: CustomerId,
        val total: Money
    ) : OrderEvent()
    
    data class OrderShipped(
        override val orderId: OrderId,
        override val occurredAt: Instant,
        val trackingNumber: TrackingNumber
    ) : OrderEvent()
}

class Order {
    private val events = mutableListOf<OrderEvent>()
    
    fun ship(trackingNumber: TrackingNumber): Either<DomainError, Order> {
        if (status != OrderStatus.PAID) {
            return DomainError.BusinessRuleViolation(
                "OrderNotPaid",
                "Can only ship paid orders"
            ).left()
        }
        
        status = OrderStatus.SHIPPED
        events.add(OrderShipped(id, Instant.now(), trackingNumber))
        
        return this.right()
    }
}
```

## Testing Your Domain Logic

Domain tests should be simple and focused:

### Testing Value Objects

```kotlin
class MoneyTest : DescribeSpec({
    describe("Money creation") {
        it("should create valid money") {
            val money = Money(BigDecimal("10.50"), Currency.getInstance("USD"))
            money.amount shouldBe BigDecimal("10.50")
            money.currency.currencyCode shouldBe "USD"
        }
        
        it("should reject negative amounts") {
            shouldThrow<IllegalArgumentException> {
                Money(BigDecimal("-10"), Currency.getInstance("USD"))
            }
        }
    }
    
    describe("Money operations") {
        it("should add money with same currency") {
            val money1 = Money(BigDecimal("10"), Currency.getInstance("USD"))
            val money2 = Money(BigDecimal("20"), Currency.getInstance("USD"))
            
            val result = money1 + money2
            
            result.amount shouldBe BigDecimal("30")
        }
        
        it("should reject adding different currencies") {
            val usd = Money(BigDecimal("10"), Currency.getInstance("USD"))
            val eur = Money(BigDecimal("20"), Currency.getInstance("EUR"))
            
            shouldThrow<IllegalArgumentException> {
                usd + eur
            }
        }
    }
})
```

### Testing Domain Services

```kotlin
class DiscountServiceTest : DescribeSpec({
    describe("Discount calculation") {
        it("should apply VIP discount") {
            val orderTotal = Money(BigDecimal("100"), Currency.getInstance("USD"))
            
            val result = DiscountPolicy.calculateDiscount(
                orderTotal, 
                CustomerType.VIP,
                null
            )
            
            result.shouldBeRight()
            result.value.percentage.value shouldBe 20
        }
        
        it("should reject orders under minimum") {
            val orderTotal = Money(BigDecimal("5"), Currency.getInstance("USD"))
            
            val result = DiscountPolicy.calculateDiscount(
                orderTotal,
                CustomerType.REGULAR,
                null
            )
            
            result.shouldBeLeft()
            val error = result.leftOrNull() as DomainError.BusinessRuleViolation
            error.rule shouldBe "MinimumOrderAmount"
        }
    }
})
```

## Common Mistakes to Avoid

### Mistake 1: External Dependencies

```kotlin
// DON'T: Domain depending on external library
import org.springframework.stereotype.Service

@Service // Framework annotation in domain!
class OrderService {
    fun createOrder() {
        logger.info("Creating order") // External logging!
        database.save(...) // Database access!
    }
}

// DO: Pure domain logic
class OrderService {
    fun createOrder(items: List<OrderItem>): Either<DomainError, Order> {
        // Pure business logic only
        return Order.create(items)
    }
}
```

### Mistake 2: Primitive Obsession

```kotlin
// DON'T: Using primitives everywhere
fun processPayment(
    amount: Double,          // What currency?
    cardNumber: String,      // Any validation?
    email: String,          // Valid format?
    customerId: Long        // Just a number
)

// DO: Rich domain types
fun processPayment(
    amount: Money,
    card: CreditCard,
    email: EmailAddress,
    customerId: CustomerId
)
```

### Mistake 3: Anemic Domain Models

```kotlin
// DON'T: Data-only classes
data class Product(
    val id: Long,
    val name: String,
    val price: Double
)

// Logic scattered in services
class ProductService {
    fun applyDiscount(product: Product, discount: Double): Product {
        // Logic here
    }
}

// DO: Rich models with behavior
class Product(
    val id: ProductId,
    val name: ProductName,
    private var price: Money
) {
    fun applyDiscount(discount: Percentage): Either<DomainError, Product> {
        if (discount.value > 50) {
            return DomainError.BusinessRuleViolation(
                "ExcessiveDiscount",
                "Cannot apply more than 50% discount"
            ).left()
        }
        
        price = price * (1 - discount.value / 100)
        return this.right()
    }
}
```

## Step-by-Step: Adding a New Domain Concept

Let's walk through adding a new feature: Customer Loyalty Points

### Step 1: Define the Value Object

```kotlin
@JvmInline
value class LoyaltyPoints private constructor(val value: Int) {
    companion object {
        fun create(value: Int): Either<DomainError.ValidationError, LoyaltyPoints> {
            return when {
                value < 0 -> DomainError.ValidationError(
                    "points", 
                    "Points cannot be negative"
                ).left()
                else -> LoyaltyPoints(value).right()
            }
        }
        
        fun zero() = LoyaltyPoints(0)
    }
    
    operator fun plus(other: LoyaltyPoints) = LoyaltyPoints(value + other.value)
    operator fun minus(other: LoyaltyPoints) = create(value - other.value)
    
    fun toMoney(rate: MoneyPerPoint): Money = Money(
        BigDecimal(value) * rate.value,
        rate.currency
    )
}
```

### Step 2: Define the Business Rules

```kotlin
object LoyaltyPointsPolicy {
    private const val POINTS_PER_DOLLAR = 10
    private const val MINIMUM_REDEMPTION = 1000
    
    fun calculatePointsEarned(purchase: Money): LoyaltyPoints {
        val dollars = purchase.amount.toInt()
        return LoyaltyPoints.create(dollars * POINTS_PER_DOLLAR)
            .getOrElse { LoyaltyPoints.zero() }
    }
    
    fun canRedeem(points: LoyaltyPoints): Boolean {
        return points.value >= MINIMUM_REDEMPTION
    }
    
    fun calculateRedemptionValue(points: LoyaltyPoints): Money {
        // 1000 points = $10
        val dollars = points.value / 100
        return Money(BigDecimal(dollars), Currency.getInstance("USD"))
    }
}
```

### Step 3: Add to Your Domain Model

```kotlin
data class Customer(
    val id: CustomerId,
    val name: PersonName,
    val email: EmailAddress,
    private val loyaltyPoints: LoyaltyPoints = LoyaltyPoints.zero()
) {
    fun earnPoints(purchase: Money): Customer {
        val earned = LoyaltyPointsPolicy.calculatePointsEarned(purchase)
        return copy(loyaltyPoints = loyaltyPoints + earned)
    }
    
    fun redeemPoints(points: LoyaltyPoints): Either<DomainError, Pair<Customer, Money>> {
        if (!LoyaltyPointsPolicy.canRedeem(points)) {
            return DomainError.BusinessRuleViolation(
                "InsufficientPoints",
                "Need at least 1000 points to redeem"
            ).left()
        }
        
        return (loyaltyPoints - points).map { remainingPoints ->
            val redemptionValue = LoyaltyPointsPolicy.calculateRedemptionValue(points)
            copy(loyaltyPoints = remainingPoints) to redemptionValue
        }
    }
}
```

### Step 4: Write Tests

```kotlin
class LoyaltyPointsTest : DescribeSpec({
    describe("earning points") {
        it("should earn 10 points per dollar") {
            val customer = Customer(/* ... */)
            val purchase = Money(BigDecimal("50"), Currency.getInstance("USD"))
            
            val updated = customer.earnPoints(purchase)
            
            updated.loyaltyPoints.value shouldBe 500
        }
    }
    
    describe("redeeming points") {
        it("should require minimum points") {
            val customer = Customer(/* ... */, loyaltyPoints = LoyaltyPoints.create(500).getOrThrow())
            
            val result = customer.redeemPoints(LoyaltyPoints.create(1000).getOrThrow())
            
            result.shouldBeLeft()
        }
    }
})
```

## Summary

The domain layer is where your business logic lives. Keep it:

- **Pure**: No external dependencies
- **Rich**: Behavior belongs with data
- **Valid**: Use factory methods and validation
- **Testable**: Simple tests, no mocks needed
- **Expressive**: Use domain language, not technical terms

Remember: If you can explain it to a business person without mentioning technology, it belongs in the domain layer!