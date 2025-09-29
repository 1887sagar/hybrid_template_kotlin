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
import org.junit.jupiter.api.Test

/**
 * Validates port pattern implementation according to Hexagonal Architecture.
 *
 * ## Hexagonal Architecture (Ports & Adapters)
 *
 * This pattern creates a clear boundary between business logic and external systems:
 *
 * ```
 *          ┌─────────────┐
 *          │   Driving   │
 *          │   Adapters  │
 *          └──────┬──────┘
 *                 │
 *          ┌──────▼──────┐
 *          │ Input Ports │ ◄── Interfaces defined by application
 *          └──────┬──────┘
 *                 │
 *          ┌──────▼──────┐
 *          │ Application │ ◄── Use cases implement input ports
 *          │    Core     │
 *          └──────┬──────┘
 *                 │
 *          ┌──────▼──────┐
 *          │Output Ports │ ◄── Interfaces defined by application
 *          └──────┬──────┘
 *                 │
 *          ┌──────▼──────┐
 *          │   Driven    │
 *          │  Adapters   │ ◄── Infrastructure implements output ports
 *          └─────────────┘
 * ```
 *
 * ## Port Categories
 *
 * ### Input Ports (Driving/Primary)
 * - Define what the application **can do**
 * - Implemented by use cases
 * - Called by presentation layer
 * - Examples: CreateOrderUseCase, ProcessPaymentUseCase
 *
 * ### Output Ports (Driven/Secondary)
 * - Define what the application **needs**
 * - Implemented by infrastructure
 * - Called by application layer
 * - Examples: OrderRepository, PaymentGateway, EmailService
 *
 * ## Why Ports Must Be Interfaces
 *
 * 1. **Dependency Inversion**: High-level modules define interfaces
 * 2. **Testability**: Easy to mock for unit tests
 * 3. **Flexibility**: Multiple implementations possible
 * 4. **Clear Contracts**: Explicit about capabilities and needs
 *
 * ## Common Violations
 *
 * ### Using Concrete Classes as Ports
 * ```kotlin
 * // ❌ Bad - concrete class as port
 * class CreateOrderUseCase {
 *     fun execute(command: CreateOrderCommand): Order
 * }
 *
 * // ✅ Good - interface as port
 * interface CreateOrderUseCase {
 *     fun execute(command: CreateOrderCommand): Either<Error, Order>
 * }
 * ```
 *
 * ### Mixing Port Types
 * ```kotlin
 * // ❌ Bad - output port in input port package
 * package application.port.input
 * interface OrderRepository // Should be in output!
 *
 * // ✅ Good - proper separation
 * package application.port.output
 * interface OrderRepository
 * ```
 *
 * ## Benefits of This Pattern
 *
 * 1. **Technology Independence**: Change database without touching business logic
 * 2. **Testability**: Test business logic without infrastructure
 * 3. **Clear Boundaries**: Obvious what belongs where
 * 4. **Parallel Development**: Teams can work on ports independently
 */
class PortPatternTest {

    private val classes = ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
        .withImportOption { location ->
            !location.contains("generated") &&
                !location.contains("/gen/") &&
                !location.contains("/build/")
        }
        .importPackages(ArchitectureConstants.BASE_PACKAGE)

    @Test
    fun `input ports should be interfaces`() {
        val rule = classes()
            .that().resideInAPackage("${ArchitectureConstants.APPLICATION_INPUT_PORTS}..")
            .should().beInterfaces()
            .because("Input ports must be interfaces to follow Hexagonal Architecture")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    @Test
    fun `output ports should be interfaces`() {
        val rule = classes()
            .that().resideInAPackage("${ArchitectureConstants.APPLICATION_OUTPUT_PORTS}..")
            .should().beInterfaces()
            .because("Output ports must be interfaces to follow Hexagonal Architecture")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    @Test
    fun `use cases should implement input ports`() {
        val rule = classes()
            .that().haveNameMatching(".*UseCase")
            .and().resideInAPackage("${ArchitectureConstants.APPLICATION_USE_CASES}..")
            .should().implement(".*InputPort")
            .because("Use cases should implement input port interfaces")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    @Test
    fun `infrastructure adapters should implement output ports`() {
        val rule = classes()
            .that().haveNameMatching(".*Adapter")
            .and().resideInAPackage("${ArchitectureConstants.INFRASTRUCTURE_OUTPUT_ADAPTERS}..")
            .should().implement(".*OutputPort")
            .because("Infrastructure adapters should implement output port interfaces")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    @Test
    fun `input ports should only depend on domain`() {
        val rule = classes()
            .that().areInterfaces()
            .and().haveNameMatching(".*InputPort")
            .and().resideInAPackage("${ArchitectureConstants.APPLICATION_INPUT_PORTS}..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "${ArchitectureConstants.DOMAIN_PACKAGE}..",
                "${ArchitectureConstants.APPLICATION_PACKAGE}..",
                "kotlin..",
                "java..",
                "arrow..",
            )
            .because("Input ports should only depend on domain and application concepts")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    @Test
    fun `output ports should only depend on domain`() {
        val rule = classes()
            .that().areInterfaces()
            .and().haveNameMatching(".*OutputPort")
            .and().resideInAPackage("${ArchitectureConstants.APPLICATION_OUTPUT_PORTS}..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "${ArchitectureConstants.DOMAIN_PACKAGE}..",
                "${ArchitectureConstants.APPLICATION_PACKAGE}..",
                "kotlin..",
                "java..",
                "arrow..",
            )
            .because("Output ports should only depend on domain and application concepts")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    @Test
    fun `presentation should use input ports not use cases directly`() {
        val rule = classes()
            .that().resideInAPackage("${ArchitectureConstants.PRESENTATION_PACKAGE}..")
            .should().onlyDependOnClassesThat()
            .resideOutsideOfPackage("${ArchitectureConstants.APPLICATION_USE_CASES}..")
            .orShould().dependOnClassesThat()
            .areInterfaces()
            .because("Presentation should depend on input port interfaces, not concrete use cases")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    @Test
    fun `port methods should return Either for error handling`() {
        // This is more of a guideline than a strict rule
        // In practice, we'd need more sophisticated analysis
        val rule = classes()
            .that().areInterfaces()
            .and().haveNameMatching(".*(Input|Output)Port")
            .should().dependOnClassesThat()
            .resideInAPackage("arrow.core..")
            .because("Ports should use Either for functional error handling")
            .allowEmptyShould(true)

        rule.check(classes)
    }
}
