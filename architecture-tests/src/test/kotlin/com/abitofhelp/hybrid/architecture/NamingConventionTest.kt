// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Test Suite
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.architecture

import com.tngtech.archunit.core.domain.JavaModifier
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.junit.jupiter.api.Test

/**
 * Validates that all classes follow consistent naming conventions across the architecture.
 *
 * ## Why Naming Conventions Matter
 *
 * Consistent naming conventions are crucial for maintainable code because they:
 * 1. **Make code self-documenting** - Names reveal intent and purpose
 * 2. **Reduce cognitive load** - Developers can predict class purposes from names
 * 3. **Enable tooling** - IDEs and static analysis tools can provide better support
 * 4. **Facilitate team collaboration** - Everyone follows the same patterns
 * 5. **Support architecture enforcement** - Names indicate which layer a class belongs to
 *
 * ## Our Naming Strategy
 *
 * We follow **domain-driven naming** with **architectural suffixes**:
 *
 * ### Domain Layer
 * - **Value Objects**: Descriptive names (PersonName, Money, Email)
 * - **Services**: End with "Service" (GreetingService, OrderService)
 * - **Policies**: End with "Policy" (DiscountPolicy, ValidationPolicy)
 * - **Repositories**: End with "Repository" (OrderRepository, UserRepository)
 * - **Errors**: End with "Error" and use sealed classes (OrderError, ValidationError)
 *
 * ### Application Layer
 * - **Use Cases**: End with "UseCase" (CreateOrderUseCase, ProcessPaymentUseCase)
 * - **Input Ports**: End with "InputPort" (CreateOrderInputPort, SearchProductsInputPort)
 * - **Output Ports**: End with "OutputPort" (SaveOrderOutputPort, SendEmailOutputPort)
 * - **DTOs**: Descriptive names (CreateOrderCommand, OrderResult)
 *
 * ### Infrastructure Layer
 * - **Adapters**: End with "Adapter" (DatabaseOrderAdapter, EmailAdapter)
 * - **Services**: End with implementation prefix (DefaultOrderService, PostgresUserRepository)
 *
 * ### Presentation Layer
 * - **Commands**: End with "Command" (CreateOrderCommand, ListUsersCommand)
 * - **Controllers**: End with "Controller" (OrderController, UserController)
 *
 * ## Why These Patterns Work
 *
 * 1. **Predictability**: If you see "UseCase", you know it's application logic
 * 2. **Searchability**: Easy to find all adapters with "*Adapter" search
 * 3. **Tooling Support**: IDEs can group related classes
 * 4. **Architecture Validation**: These tests can verify correct placement
 *
 * ## Common Anti-Patterns to Avoid
 *
 * ```kotlin
 * // ❌ Bad - Generic, non-descriptive names
 * class Manager { }
 * class Helper { }
 * class Utility { }
 * class Handler { }
 *
 * // ❌ Bad - Wrong suffixes in wrong layers
 * class OrderUseCase { } // In domain layer - should be in application!
 * class DatabaseService { } // In domain layer - should be in infrastructure!
 *
 * // ✅ Good - Clear, layer-appropriate names
 * class Order { } // Domain entity
 * class CreateOrderUseCase { } // Application use case
 * class DatabaseOrderAdapter { } // Infrastructure adapter
 * ```
 *
 * ## Testing Strategy
 *
 * These tests use **ArchUnit** to verify naming conventions by:
 * 1. **Scanning compiled bytecode** for class names and packages
 * 2. **Applying regex patterns** to validate naming
 * 3. **Checking package placement** to ensure classes are in correct layers
 * 4. **Failing fast** when conventions are violated
 *
 * This automated approach prevents naming debt from accumulating over time.
 */
class NamingConventionTest {

    private val classes = ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
        .withImportOption { location ->
            !location.contains("generated") &&
                !location.contains("/gen/") &&
                !location.contains("/build/")
        }
        .importPackages(ArchitectureConstants.BASE_PACKAGE)

    /**
     * Verifies that all use case classes end with "UseCase" suffix.
     *
     * ## Why Use Cases Need Consistent Naming
     *
     * Use cases represent **application-specific business logic** and should be easily identifiable:
     * 1. **Clear Intent**: The suffix immediately tells you this is application logic
     * 2. **Searchability**: Easy to find all use cases with "*UseCase" search
     * 3. **Layer Validation**: Helps verify use cases are in the application layer
     * 4. **Team Communication**: Everyone knows what a "UseCase" is
     *
     * ## Good Examples
     * ```kotlin
     * class CreateOrderUseCase
     * class ProcessPaymentUseCase
     * class SearchProductsUseCase
     * class CancelOrderUseCase
     * ```
     *
     * ## Bad Examples
     * ```kotlin
     * class OrderCreator     // Unclear purpose
     * class PaymentHandler   // Generic naming
     * class ProductService   // Sounds like domain service
     * class OrderManager     // Vague responsibility
     * ```
     */
    @Test
    fun `use cases should follow naming convention`() {
        val rule = classes()
            .that().resideInAPackage("${ArchitectureConstants.APPLICATION_USE_CASES}..")
            .should().haveNameMatching(".*UseCase")
            .because("Use cases should end with 'UseCase' suffix")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    /**
     * Validates that value objects are properly implemented as immutable types.
     *
     * ## What Are Value Objects?
     *
     * Value objects are **immutable data containers** that represent domain concepts:
     * 1. **Identity by Value**: Two instances with same data are considered equal
     * 2. **Immutability**: Cannot be changed after creation
     * 3. **Side-Effect Free**: Operations don't modify state
     * 4. **Domain Concepts**: Represent business concepts (Money, PersonName, Email)
     *
     * ## Implementation Requirements
     *
     * Value objects must be either:
     * - **Value Classes**: `@JvmInline value class` for single-property wrappers
     * - **Final Data Classes**: `data class` marked as final (default in Kotlin)
     *
     * ## Examples
     *
     * ```kotlin
     * // ✅ Good - Value class for simple wrapper
     * @JvmInline
     * value class PersonName(val value: String) {
     *     init { require(value.isNotBlank()) }
     * }
     *
     * // ✅ Good - Final data class for complex values
     * data class Money(val amount: BigDecimal, val currency: Currency) {
     *     init { require(amount >= BigDecimal.ZERO) }
     * }
     *
     * // ❌ Bad - Mutable class
     * class PersonName {
     *     var value: String = ""  // Can be changed!
     * }
     * ```
     *
     * ## Why This Matters
     *
     * - **Thread Safety**: Immutable objects are inherently thread-safe
     * - **Predictability**: Values can't change unexpectedly
     * - **Domain Integrity**: Business rules are enforced at creation time
     */
    @Test
    fun `value objects should follow naming convention`() {
        val rule = classes()
            .that().resideInAPackage("${ArchitectureConstants.DOMAIN_VALUE_OBJECTS}..")
            .and().areNotInterfaces()
            .and().areNotEnums()
            .should().beAnnotatedWith(JvmInline::class.java)
            .orShould().haveModifier(JavaModifier.FINAL)
            .because("Value objects should be immutable (value classes or final data classes)")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    /**
     * Ensures all input port interfaces follow the "InputPort" naming convention.
     *
     * ## What Are Input Ports?
     *
     * Input ports define **what the application can do** - they are the public API
     * of your application layer:
     * 1. **Driving Ports**: Called BY external actors (presentation layer)
     * 2. **Use Case Interfaces**: Define capabilities of the application
     * 3. **Dependency Inversion**: High-level interface, low-level implementation
     * 4. **Testing Boundary**: Can be mocked for presentation layer tests
     *
     * ## Naming Rationale
     *
     * The "InputPort" suffix clearly indicates:
     * - This is a **port** (interface) not an implementation
     * - It's an **input** to the application (driving side)
     * - It follows **hexagonal architecture** patterns
     * - It's in the **application layer** boundary
     *
     * ## Examples
     *
     * ```kotlin
     * // ✅ Good - Clear input port interfaces
     * interface CreateOrderInputPort {
     *     fun execute(command: CreateOrderCommand): Either<OrderError, OrderResult>
     * }
     *
     * interface SearchProductsInputPort {
     *     fun search(criteria: SearchCriteria): Either<SearchError, List<Product>>
     * }
     *
     * // ❌ Bad - Unclear interface purpose
     * interface OrderService {  // Sounds like domain service
     *     fun createOrder(command: CreateOrderCommand)
     * }
     *
     * interface OrderCreator {  // Not clearly a port
     *     fun create(order: Order)
     * }
     * ```
     *
     * ## Implementation Pattern
     *
     * ```kotlin
     * // Interface (Input Port)
     * interface CreateOrderInputPort {
     *     fun execute(command: CreateOrderCommand): Either<OrderError, OrderResult>
     * }
     *
     * // Implementation (Use Case)
     * class CreateOrderUseCase : CreateOrderInputPort {
     *     override fun execute(command: CreateOrderCommand): Either<OrderError, OrderResult> {
     *         // Implementation here
     *     }
     * }
     * ```
     */
    @Test
    fun `input port interfaces should end with InputPort`() {
        val rule = classes()
            .that().areInterfaces()
            .and().resideInAPackage("${ArchitectureConstants.APPLICATION_INPUT_PORTS}..")
            .should().haveNameMatching(".*InputPort")
            .because("Input port interfaces should end with 'InputPort' suffix")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    /**
     * Validates that output port interfaces follow the "OutputPort" naming convention.
     *
     * ## What Are Output Ports?
     *
     * Output ports define **what the application needs** from external systems:
     * 1. **Driven Ports**: Called BY the application (to infrastructure)
     * 2. **Dependency Interfaces**: Abstract away infrastructure concerns
     * 3. **Testing Boundary**: Can be mocked for application layer tests
     * 4. **Plugin Points**: Infrastructure "plugs into" these ports
     *
     * ## Naming Philosophy
     *
     * The "OutputPort" suffix communicates:
     * - This is a **port** (interface) defining what we need
     * - It's an **output** from the application (driven side)
     * - It follows **hexagonal architecture** patterns
     * - Infrastructure will **implement** this interface
     *
     * ## Real-World Examples
     *
     * ```kotlin
     * // ✅ Good - Clear output port interfaces
     * interface SaveOrderOutputPort {
     *     fun save(order: Order): Either<DatabaseError, Unit>
     * }
     *
     * interface SendEmailOutputPort {
     *     fun sendOrderConfirmation(email: Email, order: Order): Either<EmailError, Unit>
     * }
     *
     * interface LoggingOutputPort {
     *     fun logOrderCreated(orderId: OrderId, timestamp: Instant)
     * }
     *
     * // ❌ Bad - Infrastructure-specific naming
     * interface DatabaseRepository {  // Reveals implementation details
     *     fun saveToPostgres(order: Order)
     * }
     *
     * interface EmailService {  // Too generic, could be domain service
     *     fun sendEmail(email: Email)
     * }
     * ```
     *
     * ## Architecture Flow
     *
     * ```
     * Application Layer:
     * CreateOrderUseCase → SaveOrderOutputPort (interface)
     *                          ↓ (implemented by)
     * Infrastructure Layer:
     *                   DatabaseOrderAdapter (concrete class)
     * ```
     *
     * ## Benefits
     *
     * - **Technology Independence**: Can swap databases without changing application
     * - **Testability**: Easy to mock for testing
     * - **Clear Contracts**: Explicit about what the application needs
     */
    @Test
    fun `output port interfaces should end with OutputPort`() {
        val rule = classes()
            .that().areInterfaces()
            .and().resideInAPackage("${ArchitectureConstants.APPLICATION_OUTPUT_PORTS}..")
            .should().haveNameMatching(".*OutputPort")
            .because("Output port interfaces should end with 'OutputPort' suffix")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    /**
     * Verifies that infrastructure adapters follow the "Adapter" naming convention.
     *
     * ## What Are Adapters?
     *
     * Adapters are **infrastructure implementations** of output ports that:
     * 1. **Implement Application Interfaces**: Fulfill output port contracts
     * 2. **Handle Technical Concerns**: Database, HTTP, filesystem, external APIs
     * 3. **Translate Between Layers**: Convert domain objects to external formats
     * 4. **Isolate Framework Dependencies**: Keep Spring, Hibernate, etc. out of application layer
     *
     * ## Why "Adapter" Suffix?
     *
     * The name comes from the **Adapter Design Pattern**:
     * - **Adapts** external systems to fit application needs
     * - **Translates** between incompatible interfaces
     * - **Wraps** technical complexity
     * - **Provides** clean interface to application layer
     *
     * ## Implementation Examples
     *
     * ```kotlin
     * // ✅ Good - Clear adapter implementations
     * class DatabaseOrderAdapter(
     *     private val jpaRepository: OrderJpaRepository
     * ) : SaveOrderOutputPort {
     *     override fun save(order: Order): Either<DatabaseError, Unit> {
     *         return try {
     *             val entity = order.toJpaEntity()
     *             jpaRepository.save(entity)
     *             Unit.right()
     *         } catch (e: Exception) {
     *             DatabaseError.SaveFailed(e.message).left()
     *         }
     *     }
     * }
     *
     * class EmailAdapter(
     *     private val mailSender: JavaMailSender
     * ) : SendEmailOutputPort {
     *     override fun sendOrderConfirmation(
     *         email: Email,
     *         order: Order
     *     ): Either<EmailError, Unit> {
     *         // Implementation with JavaMailSender
     *     }
     * }
     *
     * // ❌ Bad - Unclear naming
     * class OrderService : SaveOrderOutputPort {  // Sounds like domain service
     *     // Implementation
     * }
     *
     * class DatabaseHelper : SaveOrderOutputPort {  // Generic "helper" name
     *     // Implementation
     * }
     * ```
     *
     * ## Testing Strategy
     *
     * This test verifies that classes implementing OutputPort interfaces
     * follow the Adapter naming convention, ensuring clear separation
     * between application interfaces and infrastructure implementations.
     */
    @Test
    fun `adapter implementations should follow naming`() {
        val rule = classes()
            .that().implement(".*OutputPort")
            .and().resideInAPackage("${ArchitectureConstants.INFRASTRUCTURE_ADAPTERS}..")
            .should().haveNameMatching(".*Adapter")
            .because("OutputPort implementations should end with 'Adapter' suffix")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    /**
     * Ensures repository interfaces end with "Repository" suffix.
     *
     * ## What Are Repository Interfaces?
     *
     * Repositories are **domain-level abstractions** for data persistence:
     * 1. **Domain Contracts**: Define what data operations the domain needs
     * 2. **Persistence Abstraction**: Hide storage implementation details
     * 3. **Testing Boundary**: Can be mocked for domain/application tests
     * 4. **Technology Independence**: Domain doesn't know about databases
     *
     * ## Repository Pattern Benefits
     *
     * ```kotlin
     * // ✅ Good - Domain repository interface
     * interface OrderRepository {
     *     fun findById(id: OrderId): Order?
     *     fun save(order: Order)
     *     fun findByCustomerId(customerId: CustomerId): List<Order>
     * }
     *
     * // Domain service uses repository abstraction
     * class OrderService(private val orderRepository: OrderRepository) {
     *     fun createOrder(customerId: CustomerId): Order {
     *         val orders = orderRepository.findByCustomerId(customerId)
     *         // Business logic here...
     *         orderRepository.save(newOrder)
     *         return newOrder
     *     }
     * }
     *
     * // Infrastructure implements the interface
     * class JpaOrderRepository : OrderRepository {
     *     override fun findById(id: OrderId): Order? = // JPA implementation
     *     override fun save(order: Order) = // JPA implementation
     * }
     * ```
     *
     * ## Key Rules
     *
     * 1. **Interfaces in Domain**: Repository contracts belong in domain layer
     * 2. **Implementations in Infrastructure**: Concrete repos in infrastructure
     * 3. **Domain-Focused Methods**: Methods reflect business needs, not DB structure
     * 4. **No Framework Dependencies**: Interfaces don't know about JPA, MongoDB, etc.
     */
    @Test
    fun `repository interfaces should follow naming`() {
        val rule = classes()
            .that().areInterfaces()
            .and().resideInAPackage("${ArchitectureConstants.DOMAIN_REPOSITORIES}..")
            .should().haveNameMatching(".*Repository")
            .because("Repository interfaces should end with 'Repository' suffix")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    /**
     * Validates that domain service interfaces end with "Service" suffix.
     *
     * ## What Are Domain Services?
     *
     * Domain services contain **business logic that doesn't naturally belong to any entity**:
     * 1. **Stateless Operations**: Pure business logic without entity state
     * 2. **Cross-Entity Logic**: Operations involving multiple domain objects
     * 3. **Complex Business Rules**: Algorithms too complex for simple entity methods
     * 4. **Domain Language**: Methods use business terminology, not technical terms
     *
     * ## When to Use Domain Services
     *
     * Create a domain service when:
     * - Logic involves multiple entities
     * - Business rule is complex and reusable
     * - Operation doesn't naturally belong to one entity
     * - External expertise/calculation is needed
     *
     * ## Examples
     *
     * ```kotlin
     * // ✅ Good - Domain service interface
     * interface PricingService {
     *     fun calculateOrderTotal(
     *         items: List<OrderItem>,
     *         customer: Customer,
     *         promotions: List<Promotion>
     *     ): Money
     * }
     *
     * interface InventoryService {
     *     fun reserveItems(items: List<OrderItem>): ReservationResult
     *     fun releaseReservation(reservationId: ReservationId)
     * }
     *
     * // ❌ Bad - This should be in the Order entity
     * interface OrderService {
     *     fun addItem(order: Order, item: OrderItem)  // Entity method!
     *     fun removeItem(order: Order, itemId: ItemId)  // Entity method!
     * }
     * ```
     *
     * ## Domain vs Application Services
     *
     * - **Domain Services**: Pure business logic, no infrastructure concerns
     * - **Application Services**: Orchestrate use cases, may call infrastructure
     */
    @Test
    fun `domain services should follow naming`() {
        val rule = classes()
            .that().areInterfaces()
            .and().resideInAPackage("${ArchitectureConstants.DOMAIN_SERVICES}..")
            .should().haveNameMatching(".*Service")
            .because("Domain service interfaces should end with 'Service' suffix")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    /**
     * Ensures domain policy classes end with "Policy" suffix.
     *
     * ## What Are Domain Policies?
     *
     * Policies encapsulate **business rules and decisions** that can change over time:
     * 1. **Configurable Rules**: Business logic that varies by context
     * 2. **Decision Logic**: Determine outcomes based on business criteria
     * 3. **Extensible Behavior**: Can be swapped or configured without code changes
     * 4. **Business Language**: Named using domain expert terminology
     *
     * ## Policy Pattern Benefits
     *
     * - **Flexibility**: Business rules can change without code modification
     * - **Testability**: Isolated business logic is easier to test
     * - **Clarity**: Complex decisions are explicitly modeled
     * - **Reusability**: Policies can be shared across use cases
     *
     * ## Real-World Examples
     *
     * ```kotlin
     * // ✅ Good - Domain policies
     * class DiscountPolicy {
     *     fun calculateDiscount(
     *         customer: Customer,
     *         order: Order,
     *         season: Season
     *     ): Discount {
     *         return when {
     *             customer.isVip() && season == Season.HOLIDAY ->
     *                 Discount.percentage(20)
     *             order.total > Money(100, USD) ->
     *                 Discount.percentage(10)
     *             customer.isFirstTime() ->
     *                 Discount.fixed(Money(15, USD))
     *             else -> Discount.none()
     *         }
     *     }
     * }
     *
     * class ShippingPolicy {
     *     fun determineShippingCost(
     *         destination: Address,
     *         weight: Weight,
     *         priority: ShippingPriority
     *     ): Money {
     *         // Complex shipping calculation logic
     *     }
     * }
     *
     * // ❌ Bad - Hard-coded business rules
     * class OrderService {
     *     fun processOrder(order: Order) {
     *         // Business rules scattered in application logic
     *         if (order.customer.isVip() && currentSeason() == HOLIDAY) {
     *             order.applyDiscount(0.20)  // Rule buried in service!
     *         }
     *     }
     * }
     * ```
     *
     * ## Policy vs Service
     *
     * - **Policies**: Encapsulate variable business rules
     * - **Services**: Implement stable business operations
     */
    @Test
    fun `domain policies should follow naming`() {
        val rule = classes()
            .that().resideInAPackage("${ArchitectureConstants.DOMAIN_POLICIES}..")
            .should().haveNameMatching(".*Policy")
            .because("Domain policies should end with 'Policy' suffix")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    /**
     * Validates that error hierarchies use sealed classes for type safety.
     *
     * ## Why Sealed Classes for Errors?
     *
     * Sealed classes provide **exhaustive error handling** with compile-time safety:
     * 1. **Finite Error Types**: All possible errors are known at compile time
     * 2. **Exhaustive When**: Compiler ensures all error cases are handled
     * 3. **Type Safety**: No runtime surprises from unknown error types
     * 4. **Clear Domain Model**: Errors are part of the domain model
     *
     * ## Error Hierarchy Pattern
     *
     * ```kotlin
     * // ✅ Good - Sealed error hierarchy
     * sealed class OrderError {
     *     data class InvalidQuantity(val quantity: Int) : OrderError()
     *     data class ProductNotFound(val productId: ProductId) : OrderError()
     *     data class InsufficientStock(val available: Int, val requested: Int) : OrderError()
     *     object CustomerNotEligible : OrderError()
     * }
     *
     * // Usage with exhaustive handling
     * when (result) {
     *     is OrderError.InvalidQuantity -> // Handle invalid quantity
     *     is OrderError.ProductNotFound -> // Handle missing product
     *     is OrderError.InsufficientStock -> // Handle stock issues
     *     is OrderError.CustomerNotEligible -> // Handle eligibility
     *     // Compiler ensures all cases are covered!
     * }
     *
     * // ❌ Bad - Open class hierarchy
     * abstract class OrderError  // Can be extended anywhere
     * class InvalidQuantity : OrderError()  // Inheritance from any module
     * ```
     *
     * ## Benefits Over Exceptions
     *
     * 1. **Explicit Error Handling**: Errors are part of method signatures
     * 2. **No Hidden Control Flow**: No surprise exceptions thrown
     * 3. **Functional Style**: Works well with Either/Result types
     * 4. **Better Testing**: All error paths are visible and testable
     *
     * ## Testing Implementation
     *
     * This test checks that classes ending with "Error" in domain and application
     * packages are marked as `abstract` (which sealed classes are internally).
     * Top-level error classes should be sealed to ensure exhaustive handling.
     */
    @Test
    fun `error classes should be sealed`() {
        val rule = classes()
            .that().haveNameMatching(".*Error")
            .and().resideInAnyPackage(
                "${ArchitectureConstants.DOMAIN_ERRORS}..",
                "${ArchitectureConstants.APPLICATION_ERRORS}..",
            )
            .and().areTopLevelClasses()
            .should().haveModifier(JavaModifier.ABSTRACT) // sealed classes are abstract
            .because("Error hierarchies should use sealed classes as per CLAUDE-Kotlin.md")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    /**
     * Validates that CLI command classes end with "Command" suffix.
     *
     * ## What Are Commands?
     *
     * Commands represent **user intentions or actions** in the presentation layer:
     * 1. **User Input**: Capture what the user wants to do
     * 2. **Parameter Validation**: Basic input validation and parsing
     * 3. **Application Bridge**: Convert user input to application layer calls
     * 4. **CLI Structure**: Organize command-line interface functionality
     *
     * ## Command Pattern in CLI
     *
     * ```kotlin
     * // ✅ Good - Clear command structure
     * class CreateOrderCommand {
     *     @CommandLine.Option(names = ["-c", "--customer"])
     *     var customerId: String = ""
     *
     *     @CommandLine.Option(names = ["-p", "--products"])
     *     var productIds: List<String> = emptyList()
     *
     *     @CommandLine.Option(names = ["-q", "--quantities"])
     *     var quantities: List<Int> = emptyList()
     *
     *     fun execute() {
     *         // Validate input
     *         // Call application layer
     *         val command = CreateOrderCommand(customerId, items)
     *         createOrderUseCase.execute(command)
     *     }
     * }
     *
     * class ListOrdersCommand {
     *     @CommandLine.Option(names = ["-c", "--customer"])
     *     var customerId: String? = null
     *
     *     fun execute() {
     *         val query = ListOrdersQuery(customerId)
     *         listOrdersUseCase.execute(query)
     *     }
     * }
     *
     * // ❌ Bad - Generic naming
     * class OrderHandler {  // What does it handle?
     *     fun process() {    // What does it process?
     *     }
     * }
     * ```
     *
     * ## Command Responsibilities
     *
     * 1. **Parse CLI Arguments**: Extract user input from command line
     * 2. **Basic Validation**: Check required parameters are present
     * 3. **Transform Input**: Convert strings to appropriate types
     * 4. **Delegate to Application**: Call use cases with proper DTOs
     * 5. **Handle Results**: Present success/error feedback to user
     */
    @Test
    fun `commands should follow naming convention`() {
        val rule = classes()
            .that().resideInAPackage("${ArchitectureConstants.PRESENTATION_COMMANDS}..")
            .should().haveNameMatching(".*Command")
            .because("CLI commands should end with 'Command' suffix")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    /**
     * Ensures test classes are not accidentally placed in production code packages.
     *
     * ## Why This Rule Matters
     *
     * Test classes in production packages create several problems:
     * 1. **Build Artifacts**: Tests get included in production JARs
     * 2. **Security Risk**: Test code might expose internal APIs
     * 3. **Performance Impact**: Unnecessary classes loaded at runtime
     * 4. **Confusion**: Unclear what's production vs test code
     * 5. **Deployment Issues**: Test dependencies might be missing in production
     *
     * ## Proper Test Organization
     *
     * ```
     * src/
     * ├── main/kotlin/           # Production code
     * │   └── com/example/
     * │       ├── domain/
     * │       ├── application/
     * │       └── infrastructure/
     * └── test/kotlin/           # Test code
     *     └── com/example/
     *         ├── domain/
     *         ├── application/
     *         └── infrastructure/
     * ```
     *
     * ## What We Check
     *
     * Classes with these naming patterns should NOT be in production packages:
     * - `*Test` - Unit test classes
     * - `*Tests` - Test suites
     * - `*Spec` - Specification tests (BDD style)
     *
     * ## Common Violations
     *
     * ```kotlin
     * // ❌ Bad - Test in production package
     * package com.example.domain.service  // Production package!
     * class OrderServiceTest {             // Test class!
     *     @Test
     *     fun shouldCreateOrder() { }
     * }
     *
     * // ✅ Good - Test in test package
     * package com.example.domain.service  // Test source root
     * class OrderServiceTest {
     *     @Test
     *     fun shouldCreateOrder() { }
     * }
     * ```
     *
     * ## How to Fix Violations
     *
     * If this test fails:
     * 1. **Move test classes** to `src/test/kotlin` directory
     * 2. **Keep same package structure** as production code
     * 3. **Update imports** if necessary
     * 4. **Verify build configuration** separates test and main source sets
     *
     * ## Testing Strategy
     *
     * This test uses ArchUnit to scan compiled classes and ensures
     * no test-named classes exist in production package hierarchies.
     */
    @Test
    fun `test classes should not be in production packages`() {
        val rule = classes()
            .that().haveNameMatching(".*Test")
            .or().haveNameMatching(".*Tests")
            .or().haveNameMatching(".*Spec")
            .should().resideOutsideOfPackages(
                "${ArchitectureConstants.DOMAIN_PACKAGE}..",
                "${ArchitectureConstants.APPLICATION_PACKAGE}..",
                "${ArchitectureConstants.INFRASTRUCTURE_PACKAGE}..",
                "${ArchitectureConstants.PRESENTATION_PACKAGE}..",
                "${ArchitectureConstants.BOOTSTRAP_PACKAGE}..",
            )
            .because("Test classes should be in test packages, not production code")
            .allowEmptyShould(true)

        rule.check(classes)
    }
}
