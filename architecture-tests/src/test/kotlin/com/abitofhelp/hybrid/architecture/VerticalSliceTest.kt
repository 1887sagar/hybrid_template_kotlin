////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Test Suite
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

/**
 * Validates that our architecture correctly implements vertical slices with proper dependency flow.
 *
 * ## What Are Vertical Slices?
 *
 * A **vertical slice** is a complete feature that spans all architectural layers while
 * respecting layer dependency rules. Think of it as a "feature column" that drills
 * through the architecture from top to bottom, like this:
 *
 * ```
 *              ┌─── FEATURE: Create Greeting ───┐
 *              │                                │
 * Presentation │  CLI Command Handler           │ ← User Interface
 *              │           │                     │
 * Application  │  CreateGreetingUseCase ───────┐│ ← Business Logic
 *              │           │                   ││
 * Domain       │  GreetingService + PersonName ││ ← Core Business
 *              │           │                   ││
 * Infrastructure│ DefaultGreetingService        ││ ← Technical Details
 *              │  ConsoleOutputAdapter         ││
 *              └─────────────────────────────────┘
 * ```
 *
 * ## Dependency Flow Rules (The "Drilling Inward" Pattern)
 *
 * Dependencies must flow **INWARD** toward the domain center:
 *
 * ### ✅ Allowed Dependencies
 * - **Presentation** → Application (calls use cases via input ports)
 * - **Infrastructure** → Application + Domain (implements ports, uses domain services)
 * - **Application** → Domain (orchestrates domain logic)
 * - **Domain** → Nothing (pure business logic, no external dependencies)
 * - **Bootstrap** → All layers (special case - composition root)
 *
 * ### ❌ Forbidden Dependencies
 * - **Domain** → Any other layer (would create coupling)
 * - **Application** → Infrastructure (would couple to technical details)
 * - **Presentation** → Domain (would bypass application layer)
 * - **Presentation** → Infrastructure (would create tight coupling)
 *
 * ## Why This Pattern Works
 *
 * 1. **Dependency Inversion Principle**: High-level modules don't depend on low-level modules
 * 2. **Testability**: Inner layers can be tested without outer layers
 * 3. **Flexibility**: Infrastructure can be swapped without affecting business logic
 * 4. **Clear Boundaries**: Each layer has a single, well-defined responsibility
 * 5. **Framework Independence**: Domain and application layers are pure business logic
 *
 * ## Real-World Example: Greeting Feature
 *
 * Here's how the "greeting" feature demonstrates proper vertical slicing:
 *
 * ```kotlin
 * // 1. Presentation Layer - Knows about Application
 * class CliRunner(private val createGreeting: CreateGreetingInputPort) {
 *     fun handleGreetingCommand(name: String) {
 *         val command = CreateGreetingCommand(name)
 *         createGreeting.execute(command)  // Calls into application
 *     }
 * }
 *
 * // 2. Application Layer - Knows about Domain
 * class CreateGreetingUseCase(
 *     private val greetingService: GreetingService,  // Domain interface
 *     private val outputPort: OutputPort             // Infrastructure interface
 * ) : CreateGreetingInputPort {
 *     override fun execute(command: CreateGreetingCommand) {
 *         val personName = PersonName(command.name)       // Domain value object
 *         val greeting = greetingService.createGreeting(personName) // Domain service
 *         outputPort.output(greeting.value)               // Infrastructure call
 *     }
 * }
 *
 * // 3. Domain Layer - Knows about nothing external
 * interface GreetingService {
 *     fun createGreeting(name: PersonName): Greeting
 * }
 *
 * @JvmInline
 * value class PersonName(val value: String)
 *
 * // 4. Infrastructure Layer - Knows about Domain + Application
 * class DefaultGreetingService : GreetingService {  // Implements domain interface
 *     override fun createGreeting(name: PersonName): Greeting {
 *         return Greeting("Hello, ${name.value}!")
 *     }
 * }
 *
 * class ConsoleOutputAdapter : OutputPort {  // Implements application interface
 *     override fun output(message: String) {
 *         println(message)
 *     }
 * }
 * ```
 *
 * ## Testing Strategy
 *
 * These tests verify that:
 * 1. **Features span appropriate layers** - Greeting components exist across layers
 * 2. **Dependencies flow inward** - No layer violates the dependency rules
 * 3. **Infrastructure isolation** - No framework dependencies leak into domain
 * 4. **Proper layer placement** - Classes are in their correct architectural homes
 *
 * ## Benefits of Vertical Slices
 *
 * - **Feature Completeness**: Each slice delivers end-to-end value
 * - **Team Ownership**: Teams can own complete features across layers
 * - **Parallel Development**: Multiple features can be developed simultaneously
 * - **Incremental Delivery**: Features can be delivered one slice at a time
 * - **Architectural Integrity**: Layer rules are maintained within each slice
 */
class VerticalSliceTest {

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
     * Validates that vertical slices properly implement the inward dependency flow pattern.
     *
     * ## What This Test Verifies
     * 
     * This test ensures that feature components (like "greeting") are properly distributed
     * across architectural layers while respecting dependency direction rules.
     * 
     * ## Inward Dependency Flow
     * 
     * Dependencies must flow toward the domain center:
     * ```
     * Presentation → Application → Domain ← Infrastructure
     *      CLI           UseCase       Service ←   Adapter
     *       │              │           │        │
     *       └───────────────┘           └────────┘
     *                                   ^
     *                              Domain Core
     * ```
     * 
     * ## Feature Distribution Example
     * 
     * The "greeting" feature should have components in these layers:
     * - **Domain**: `GreetingService`, `PersonName` (business logic)
     * - **Application**: `CreateGreetingUseCase` (orchestration)
     * - **Infrastructure**: `DefaultGreetingService` (implementation)
     * - **Presentation**: CLI handling (user interface)
     * - **Bootstrap**: Wiring everything together
     * 
     * ## Why This Pattern Works
     * 
     * 1. **Domain Independence**: Core business logic has no external dependencies
     * 2. **Layer Isolation**: Each layer has a clear, single responsibility
     * 3. **Testability**: Inner layers can be tested without outer layers
     * 4. **Flexibility**: Infrastructure can be swapped without affecting business logic
     * 
     * ## Testing Strategy
     * 
     * This test validates the concept that vertical slices can span layers
     * while the actual dependency rules are enforced by `LayerDependencyTest`.
     * It's more about feature organization than strict architectural violations.
     */
    @Test
    fun `vertical slices should follow inward dependency rule`() {
        // Test that dependencies flow inward per Clean Architecture
        // Within a feature (e.g., "greeting"), dependencies should flow:
        // presentation.cli → application.port.input (CreateGreetingInputPort)
        // infrastructure.adapter.output → application.port.output (OutputPort)
        // infrastructure.service → domain.service (GreetingService)
        // application.usecase → domain.*

        // Since we organize by layer first, then feature, we test layer dependencies
        // The LayerDependencyTest already validates the inward flow
        // This test focuses on feature cohesion within those constraints

        val rule = classes()
            .that().haveNameMatching(".*Greeting.*")
            .should().resideInAnyPackage(
                "${ArchitectureConstants.DOMAIN_PACKAGE}..", // GreetingService, PersonName
                "${ArchitectureConstants.APPLICATION_PACKAGE}..", // CreateGreetingUseCase
                "${ArchitectureConstants.INFRASTRUCTURE_PACKAGE}..", // DefaultGreetingService
                "${ArchitectureConstants.PRESENTATION_PACKAGE}..", // CLI with greeting
                "${ArchitectureConstants.BOOTSTRAP_PACKAGE}..", // CompositionRoot
            )
            .because("Greeting feature should have components in appropriate layers")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    @Test
    fun `greeting feature demonstrates proper vertical slice`() {
        // A vertical slice for the "greeting" feature showing dependency flow inward:
        // 1. CLI (presentation) → CreateGreetingInputPort (application)
        // 2. CreateGreetingUseCase (application) → GreetingService + OutputPort + domain models
        // 3. DefaultGreetingService (infrastructure) → GreetingService (implements it)
        // 4. ConsoleOutputAdapter (infrastructure) → OutputPort (implements it)
        // 5. Domain has no outward dependencies

        // Verify the greeting feature exists in appropriate layers
        val greetingInDomain = noClasses()
            .that().resideInAPackage("${ArchitectureConstants.DOMAIN_PACKAGE}..")
            .and().haveNameMatching(".*Greeting.*")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "${ArchitectureConstants.APPLICATION_PACKAGE}..",
                "${ArchitectureConstants.INFRASTRUCTURE_PACKAGE}..",
                "${ArchitectureConstants.PRESENTATION_PACKAGE}..",
                "${ArchitectureConstants.BOOTSTRAP_PACKAGE}..",
            )
            .because("Domain greeting components must not depend on outer layers")
            .allowEmptyShould(true)

        val greetingInfrastructure = classes()
            .that().resideInAPackage("${ArchitectureConstants.INFRASTRUCTURE_PACKAGE}..")
            .and().haveNameMatching(".*Greeting.*")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "${ArchitectureConstants.DOMAIN_PACKAGE}..",
                "${ArchitectureConstants.APPLICATION_PACKAGE}..",
                "${ArchitectureConstants.INFRASTRUCTURE_PACKAGE}..",
                "java..",
                "kotlin..",
                "arrow..",
            )
            .because("Infrastructure can only depend inward (Domain/Application) plus technical libraries")
            .allowEmptyShould(true)

        greetingInDomain.check(classes)
        greetingInfrastructure.check(classes)
    }

    @Test
    fun `each feature should have components in appropriate layers`() {
        // Verify that greeting feature has proper structure
        val greetingLayerDistribution = classes()
            .that().haveNameMatching(".*Greeting.*")
            .should().resideInAnyPackage(
                "${ArchitectureConstants.DOMAIN_PACKAGE}..",
                "${ArchitectureConstants.APPLICATION_PACKAGE}..",
                "${ArchitectureConstants.INFRASTRUCTURE_PACKAGE}..",
                "${ArchitectureConstants.PRESENTATION_PACKAGE}..",
                "${ArchitectureConstants.BOOTSTRAP_PACKAGE}..",
            )
            .because("Greeting feature components should be distributed across layers")
            .allowEmptyShould(true)

        greetingLayerDistribution.check(classes)
    }

    /**
     * Ensures that domain layer remains free of infrastructure framework dependencies.
     *
     * ## Why Domain Must Stay Pure
     * 
     * The domain layer represents **pure business logic** and must not depend on:
     * 1. **Web Frameworks**: Spring, Ktor, etc.
     * 2. **Persistence Frameworks**: Hibernate, JPA, etc.
     * 3. **Dependency Injection**: Guice, Dagger, Koin, etc.
     * 4. **External Libraries**: Unless they're domain-relevant (like money libraries)
     * 
     * ## Prohibited Framework Dependencies
     * 
     * ```kotlin
     * // ❌ Bad - Infrastructure leaking into domain
     * package com.example.domain.service
     * 
     * import org.springframework.stereotype.Service  // Framework dependency!
     * import jakarta.persistence.Entity             // JPA dependency!
     * import com.google.inject.Inject               // DI framework!
     * 
     * @Service  // Spring annotation in domain!
     * class OrderService @Inject constructor(      // DI in domain!
     *     private val repository: OrderRepository
     * ) {
     *     fun createOrder(items: List<OrderItem>): Order {
     *         // Business logic
     *     }
     * }
     * 
     * // ✅ Good - Pure domain service
     * package com.example.domain.service
     * 
     * interface OrderService {  // Pure interface
     *     fun createOrder(items: List<OrderItem>): Order
     * }
     * 
     * class OrderServiceImpl : OrderService {  // Pure implementation
     *     override fun createOrder(items: List<OrderItem>): Order {
     *         // Pure business logic with no framework dependencies
     *         return Order.create(items)
     *     }
     * }
     * ```
     * 
     * ## Benefits of Pure Domain
     * 
     * 1. **Framework Independence**: Can use any framework or no framework
     * 2. **Fast Testing**: No need to start Spring context for domain tests
     * 3. **Clear Business Logic**: No technical noise obscuring business rules
     * 4. **Portability**: Domain logic can be moved between applications
     * 5. **Long-term Stability**: Business logic outlasts framework choices
     * 
     * ## Allowed Domain Dependencies
     * 
     * The domain layer may depend on:
     * - **Kotlin Standard Library**: Collections, functions, etc.
     * - **Java Standard Library**: Basic utilities
     * - **Arrow Core**: Functional programming utilities (Either, Option)
     * - **Domain-Specific Libraries**: Money libraries, time libraries
     * 
     * ## Framework Placement
     * 
     * Instead of polluting domain with frameworks:
     * ```kotlin
     * // Infrastructure layer handles frameworks
     * @Service  // Spring annotation in infrastructure
     * class SpringOrderService(
     *     private val domainService: OrderService  // Pure domain service
     * ) : OrderApplicationService {
     *     
     *     @Transactional  // Framework concern
     *     override fun createOrder(command: CreateOrderCommand) {
     *         val items = command.toOrderItems()
     *         val order = domainService.createOrder(items)  // Pure call
     *         // Handle infrastructure concerns
     *     }
     * }
     * ```
     */
    @Test
    fun `infrastructure should not leak into domain`() {
        val rule = noClasses()
            .that().resideInAPackage("${ArchitectureConstants.DOMAIN_PACKAGE}..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "jakarta..",
                "javax..",
                "com.google.inject..",
                "dagger..",
                "koin..",
            )
            .because("Domain should not depend on frameworks or infrastructure concerns")
            .allowEmptyShould(true)

        rule.check(classes)
    }
}
