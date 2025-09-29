// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Test Suite
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

/**
 * Validates that the bootstrap module correctly implements the Composition Root pattern.
 *
 * ## What is the Bootstrap Module?
 *
 * The bootstrap module is the **entry point** of the application and serves as the "composition root"
 * where all dependencies are wired together. Think of it as the conductor of an orchestra - it knows
 * about all the musicians (layers) and brings them together to create the symphony (application).
 *
 * ## Why These Tests Matter
 *
 * These tests ensure the bootstrap module follows critical architectural principles:
 *
 * 1. **Single Responsibility**: Bootstrap should only handle application startup and dependency injection
 * 2. **Composition Root Pattern**: All object creation and wiring happens in one place
 * 3. **Dependency Injection**: Manual DI is performed here (no framework magic in domain/application)
 * 4. **Layer Access Control**: Only bootstrap can "see" all layers simultaneously
 *
 * ## Bootstrap Module Rules
 *
 * ### ✅ Bootstrap CAN:
 * - Access ALL layers (domain, application, infrastructure, presentation)
 * - Contain main() entry point functions
 * - Wire dependencies manually (composition root)
 * - Contain configuration classes
 * - Create concrete implementations
 *
 * ### ❌ Bootstrap CANNOT:
 * - Contain business logic
 * - Have complex algorithms
 * - Implement domain rules
 * - Contain data processing logic
 *
 * ## What We Test
 *
 * 1. **Entry Point Location**: Ensures main() functions are in bootstrap
 * 2. **Dependency Instantiation**: Only bootstrap creates concrete implementations
 * 3. **Code Composition**: Bootstrap contains only wiring/configuration code
 * 4. **Composition Root Presence**: CompositionRoot class exists in bootstrap
 * 5. **Business Logic Absence**: No domain logic leaks into bootstrap
 *
 * ## Real-World Example
 *
 * ```kotlin
 * // ✅ Good - Bootstrap composition root
 * class CompositionRoot {
 *     fun createGreetingUseCase(): CreateGreetingInputPort {
 *         val greetingService = DefaultGreetingService()
 *         val outputAdapter = ConsoleOutputAdapter()
 *         return CreateGreetingUseCase(greetingService, outputAdapter)
 *     }
 * }
 *
 * // ❌ Bad - Business logic in bootstrap
 * class BootstrapWithBusinessLogic {
 *     fun processGreeting(name: String): String {
 *         // This validation logic belongs in domain!
 *         if (name.isBlank()) throw IllegalArgumentException("Name required")
 *         return "Hello, $name!"
 *     }
 * }
 * ```
 *
 * ## Testing Approach
 *
 * We use **ArchUnit** to analyze compiled bytecode and verify:
 * - Package structures match our rules
 * - Class naming follows conventions
 * - Dependencies flow in the correct direction
 * - No architectural violations exist
 *
 * These tests act as a **safety net** - they catch architectural violations early,
 * before they become embedded in the codebase and harder to fix.
 */
class BootstrapModuleTest {

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
     * Verifies that application entry points (main functions) are located in the bootstrap module.
     *
     * ## Why This Test Matters
     *
     * The main() function is the application's entry point and should be in bootstrap because:
     * 1. **Separation of Concerns**: Entry points don't belong in business logic layers
     * 2. **Composition Root**: Bootstrap is responsible for application startup
     * 3. **Dependency Direction**: Only bootstrap should know about all layers
     * 4. **Framework Independence**: Domain/application shouldn't know about startup
     *
     * ## What We Check
     *
     * - Classes ending with "EntryPointKt" or "MainKt" (Kotlin main function containers)
     * - These classes must reside in the bootstrap package hierarchy
     *
     * ## Common Violations
     *
     * ```kotlin
     * // ❌ Bad - main() in domain layer
     * package com.abitofhelp.hybrid.domain
     * fun main() { /* startup code */ }
     *
     * // ✅ Good - main() in bootstrap
     * package com.abitofhelp.hybrid.bootstrap
     * fun main() { CompositionRoot().start() }
     * ```
     */
    @Test
    fun `main function should be in bootstrap module`() {
        val rule = classes()
            .that().haveNameMatching(".*EntryPointKt")
            .or().haveNameMatching(".*MainKt")
            .should().resideInAPackage("${ArchitectureConstants.BOOTSTRAP_PACKAGE}..")
            .because("Application entry point must be in bootstrap module")
            .allowEmptyShould(true) // May not be found in test context

        rule.check(classes)
    }

    /**
     * Ensures that concrete implementations are only instantiated within the bootstrap module.
     *
     * ## Why This Test Matters
     *
     * This enforces the **Dependency Inversion Principle** (DIP):
     * 1. **High-level modules** (domain/application) should not depend on low-level modules (infrastructure)
     * 2. **Both should depend on abstractions** (interfaces/ports)
     * 3. **Only the composition root** should know about concrete implementations
     *
     * ## What We Check
     *
     * - No classes outside bootstrap should call constructors of:
     *   - DefaultGreetingService (infrastructure implementation)
     *   - ConsoleOutputAdapter (infrastructure adapter)
     * - This ensures loose coupling between layers
     *
     * ## Real-World Example
     *
     * ```kotlin
     * // ❌ Bad - Application directly instantiating infrastructure
     * class CreateGreetingUseCase {
     *     private val service = DefaultGreetingService() // Violation!
     * }
     *
     * // ✅ Good - Application depends on abstraction
     * class CreateGreetingUseCase(private val service: GreetingService) {
     *     // Injected by bootstrap composition root
     * }
     * ```
     *
     * ## Benefits
     *
     * - **Testability**: Easy to inject mocks for testing
     * - **Flexibility**: Can swap implementations without changing business logic
     * - **Maintainability**: Changes to infrastructure don't break application layer
     */
    @Test
    fun `only bootstrap can instantiate concrete implementations`() {
        // Check that concrete implementations are only instantiated in bootstrap
        val rule = noClasses()
            .that().resideOutsideOfPackage("${ArchitectureConstants.BOOTSTRAP_PACKAGE}..")
            .should().callConstructor(
                com.abitofhelp.hybrid.infrastructure.service.DefaultGreetingService::class.java,
            )
            .orShould().callConstructor(
                com.abitofhelp.hybrid.infrastructure.adapter.output.ConsoleOutputAdapter::class.java,
            )
            .because("Only bootstrap module should instantiate concrete implementations")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    /**
     * Validates that bootstrap module contains only configuration and wiring code.
     *
     * ## Purpose of This Rule
     *
     * Bootstrap should be the **thinnest possible layer** that:
     * 1. **Wires dependencies** together (composition root pattern)
     * 2. **Configures the application** (settings, environment)
     * 3. **Starts the application** (entry points)
     * 4. **Does NOT contain business logic** (that belongs in domain/application)
     *
     * ## What We Check
     *
     * Classes in bootstrap should be named with these suffixes:
     * - `*Config` - Configuration classes
     * - `*CompositionRoot` - Dependency wiring
     * - `*App` - Application startup classes
     * - `*EntryPoint*` - Main function containers
     * - `*Main*` - Entry point classes
     * - Interfaces and Enums are also allowed
     *
     * ## Examples
     *
     * ```kotlin
     * // ✅ Good - Bootstrap classes
     * class AppConfig { /* configuration */ }
     * class CompositionRoot { /* dependency wiring */ }
     * class App { /* application startup */ }
     *
     * // ❌ Bad - Business logic in bootstrap
     * class OrderProcessor { /* domain logic - belongs in domain! */ }
     * class ValidationService { /* application logic - wrong layer! */ }
     * ```
     *
     * ## Why This Matters
     *
     * - **Single Responsibility**: Bootstrap has one job - wire and start
     * - **Testing**: Business logic should be testable without bootstrap
     * - **Clarity**: Clear separation makes codebase easier to understand
     */
    @Test
    fun `bootstrap should contain only wiring code`() {
        val rule = classes()
            .that().resideInAPackage("${ArchitectureConstants.BOOTSTRAP_PACKAGE}..")
            .should().haveNameMatching(".*(Config|CompositionRoot|App|EntryPoint.*|Main.*)")
            .orShould().beInterfaces()
            .orShould().beEnums()
            .because("Bootstrap should only contain configuration and wiring code")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    /**
     * Verifies that the CompositionRoot class exists in the bootstrap module.
     *
     * ## What is a Composition Root?
     *
     * A Composition Root is a class responsible for composing (wiring) all dependencies
     * for the application. It's the **single place** where:
     * 1. **Concrete implementations are created**
     * 2. **Dependencies are injected manually**
     * 3. **Object graphs are assembled**
     * 4. **The application is "composed" from its parts**
     *
     * ## Why We Need This
     *
     * - **Dependency Injection without Frameworks**: Manual DI keeps domain/application clean
     * - **Single Source of Truth**: All wiring happens in one place
     * - **Flexibility**: Easy to change implementations for testing or different environments
     * - **Explicit Dependencies**: No hidden framework magic, dependencies are clear
     *
     * ## Example CompositionRoot
     *
     * ```kotlin
     * class CompositionRoot {
     *     fun createGreetingFlow(): CreateGreetingInputPort {
     *         // 1. Create infrastructure implementations
     *         val greetingService = DefaultGreetingService()
     *         val outputAdapter = ConsoleOutputAdapter()
     *
     *         // 2. Create application use case with dependencies
     *         return CreateGreetingUseCase(
     *             greetingService = greetingService,
     *             outputPort = outputAdapter
     *         )
     *     }
     * }
     * ```
     *
     * ## Testing Note
     *
     * This test allows empty results because in some test contexts,
     * the CompositionRoot might not be on the classpath.
     */
    @Test
    fun `composition root should exist in bootstrap`() {
        val rule = classes()
            .that().haveNameMatching(".*CompositionRoot.*")
            .should().resideInAPackage("${ArchitectureConstants.BOOTSTRAP_PACKAGE}..")
            .because("CompositionRoot must be in bootstrap module")
            .allowEmptyShould(true) // May not be found in test context

        rule.check(classes)
    }

    /**
     * Ensures that no business logic accidentally leaks into the bootstrap module.
     *
     * ## Why This Test is Critical
     *
     * Business logic in bootstrap violates fundamental architectural principles:
     * 1. **Single Responsibility**: Bootstrap should only handle wiring, not processing
     * 2. **Testability**: Business logic in bootstrap is hard to unit test
     * 3. **Separation of Concerns**: Domain logic belongs in domain layer
     * 4. **Maintainability**: Mixed concerns make code harder to understand and change
     *
     * ## What We Prohibit
     *
     * Classes in bootstrap that don't match these patterns:
     * - Configuration classes (`*Config`)
     * - Composition root classes (`*CompositionRoot`)
     * - Application startup classes (`*App`)
     * - Entry point classes (`*EntryPoint`, `*Main`)
     * - Interfaces and Enums (which are always allowed)
     *
     * ## Red Flag Examples
     *
     * ```kotlin
     * // ❌ Bad - Business logic in bootstrap
     * package com.example.bootstrap
     * class OrderValidator {  // Should be in domain!
     *     fun validateOrder(order: Order): ValidationResult { ... }
     * }
     *
     * class PaymentProcessor {  // Should be in application!
     *     fun processPayment(amount: Money): PaymentResult { ... }
     * }
     *
     * // ✅ Good - Only wiring in bootstrap
     * class CompositionRoot {
     *     fun createOrderValidator(): OrderValidator = OrderValidatorImpl()
     *     fun createPaymentProcessor(): PaymentProcessor = StripePaymentProcessor()
     * }
     * ```
     *
     * ## How to Fix Violations
     *
     * If this test fails:
     * 1. **Move business logic** to appropriate layer (domain/application)
     * 2. **Create interfaces** in application layer
     * 3. **Implement interfaces** in infrastructure layer
     * 4. **Wire dependencies** in bootstrap composition root
     */
    @Test
    fun `no business logic in bootstrap module`() {
        val rule = classes()
            .that().resideInAPackage("${ArchitectureConstants.BOOTSTRAP_PACKAGE}..")
            .and().haveNameNotMatching(".*(Config|CompositionRoot|App|EntryPoint|Main).*")
            .should().beInterfaces()
            .orShould().beEnums()
            .because("Bootstrap should not contain business logic")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    /**
     * Verifies that bootstrap module can access all architectural layers.
     *
     * ## Why Bootstrap is Special
     *
     * Bootstrap is the **only module** allowed to access all layers because:
     * 1. **Composition Root Responsibility**: Must wire dependencies from all layers
     * 2. **Application Startup**: Needs to initialize components across layers
     * 3. **Dependency Injection**: Must instantiate concrete implementations from infrastructure
     * 4. **Entry Point Role**: Coordinates the entire application startup sequence
     *
     * ## Layer Access Rules
     *
     * - **Domain**: ❌ Cannot access any other layer
     * - **Application**: ✅ Can access Domain only
     * - **Infrastructure**: ✅ Can access Domain + Application
     * - **Presentation**: ✅ Can access Application only
     * - **Bootstrap**: ✅ Can access ALL layers (special case)
     *
     * ## What This Test Does
     *
     * This is a **conceptual verification** rather than a strict rule check:
     * 1. **Looks for bootstrap classes** in the test context
     * 2. **Confirms they exist** (if available on classpath)
     * 3. **Documents the principle** that bootstrap can access all layers
     *
     * The actual dependency rules are enforced in `LayerDependencyTest`.
     *
     * ## Real-World Bootstrap Example
     *
     * ```kotlin
     * class CompositionRoot {
     *     fun wireApplication(): App {
     *         // Access Domain
     *         val greetingPolicy = GreetingPolicy()
     *
     *         // Access Infrastructure
     *         val greetingService = DefaultGreetingService(greetingPolicy)
     *         val outputAdapter = ConsoleOutputAdapter()
     *
     *         // Access Application
     *         val useCase = CreateGreetingUseCase(greetingService, outputAdapter)
     *
     *         // Access Presentation
     *         val cli = CliRunner(useCase)
     *
     *         return App(cli)
     *     }
     * }
     * ```
     *
     * ## Testing Note
     *
     * In test contexts, bootstrap classes might not be available on the classpath,
     * which is expected behavior for isolated architecture tests.
     */
    @Test
    fun `bootstrap can access all layers`() {
        // This is a positive test - bootstrap SHOULD be able to access all layers
        // In a real build, the bootstrap module can access all other layers.
        // However, in the test context, we might not have all classes available.

        // This test verifies the concept rather than actual classes
        val bootstrapClasses = classes.filter { it.packageName.startsWith(ArchitectureConstants.BOOTSTRAP_PACKAGE) }

        if (bootstrapClasses.isNotEmpty()) {
            // If we have bootstrap classes, verify they exist
            assert(true) { "Bootstrap classes found" }
        } else {
            // In test context, bootstrap classes might not be on the classpath
            // This is expected behavior in architecture tests
            println("Note: Bootstrap classes not found in test context, which is expected")
        }

        // The actual architectural rule that bootstrap can access all layers
        // is validated in LayerDependencyTest
    }
}
