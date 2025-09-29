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
 * Validates that classes are organized in the correct package structure according to our architectural layers.
 *
 * ## Why Package Structure Matters
 *
 * A well-organized package structure is the **foundation of maintainable architecture**:
 * 1. **Discoverability**: Developers can quickly find related classes
 * 2. **Enforces Boundaries**: Package structure reflects architectural layers
 * 3. **Enables Tooling**: IDEs and build tools can leverage structure
 * 4. **Reduces Cognitive Load**: Predictable organization patterns
 * 5. **Supports Team Scalability**: Multiple developers can work without conflicts
 *
 * ## Our Package Organization Strategy
 *
 * We use **layer-first organization** with **functional sub-packages**:
 *
 * ```
 * com.abitofhelp.hybrid/
 * ├── domain/                    # Core business logic (no dependencies)
 * │   ├── value/                 # Value objects (PersonName, Money)
 * │   ├── entity/                # Domain entities (Order, Customer)
 * │   ├── service/               # Domain services (GreetingService)
 * │   ├── repository/            # Repository interfaces
 * │   ├── policy/                # Business policies
 * │   └── error/                 # Domain errors
 * ├── application/               # Application logic (orchestration)
 * │   ├── usecase/               # Use case implementations
 * │   ├── port/
 * │   │   ├── input/             # Input ports (driving)
 * │   │   └── output/            # Output ports (driven)
 * │   ├── dto/                   # Data transfer objects
 * │   └── error/                 # Application errors
 * ├── infrastructure/            # Technical implementation
 * │   ├── adapter/
 * │   │   └── output/            # Output port implementations
 * │   └── service/               # Infrastructure service implementations
 * ├── presentation/              # User interface layer
 * │   └── cli/                   # Command-line interface
 * └── bootstrap/                 # Application startup and wiring
 * ```
 *
 * ## Layer-First vs Feature-First
 *
 * We chose **layer-first** organization because:
 * 1. **Architectural Clarity**: Layer boundaries are immediately visible
 * 2. **Dependency Management**: Easier to enforce layer dependency rules
 * 3. **Shared Components**: Cross-feature utilities have natural homes
 * 4. **Team Organization**: Teams can own entire layers
 * 5. **Tool Support**: Build tools can work with layer-based modules
 *
 * ## Package Placement Rules
 *
 * ### Domain Layer (`com.abitofhelp.hybrid.domain`)
 * - **Value Objects**: `domain.value` or `domain.model` (relaxed rule)
 * - **Services**: `domain.service`
 * - **Repositories**: `domain.repository` (interfaces only!)
 * - **Policies**: `domain.policy`
 * - **Errors**: `domain.error`
 *
 * ### Application Layer (`com.abitofhelp.hybrid.application`)
 * - **Use Cases**: `application.usecase`
 * - **Input Ports**: `application.port.input`
 * - **Output Ports**: `application.port.output`
 * - **DTOs**: `application.dto`
 * - **Errors**: `application.error`
 *
 * ### Infrastructure Layer (`com.abitofhelp.hybrid.infrastructure`)
 * - **Adapters**: `infrastructure.adapter.output`
 * - **Services**: `infrastructure.service`
 *
 * ### Bootstrap Layer (`com.abitofhelp.hybrid.bootstrap`)
 * - **Composition Root**: Directly in bootstrap package
 * - **Configuration**: Bootstrap package
 * - **Entry Points**: Bootstrap package
 *
 * ## Common Package Anti-Patterns
 *
 * ```kotlin
 * // ❌ Bad - Wrong package placement
 * package com.example.domain.service
 * class DatabaseOrderService  // Infrastructure! Should be in infrastructure.service
 *
 * package com.example.presentation.usecase
 * class CreateOrderUseCase  // Application logic! Should be in application.usecase
 *
 * // ✅ Good - Correct placement
 * package com.example.domain.service
 * interface OrderService  // Domain interface
 *
 * package com.example.infrastructure.service
 * class DatabaseOrderService : OrderService  // Infrastructure implementation
 * ```
 *
 * ## Testing Benefits
 *
 * These package structure tests provide:
 * 1. **Early Detection**: Catch misplaced classes during development
 * 2. **Refactoring Safety**: Ensure moves don't break package rules
 * 3. **Team Alignment**: Automated enforcement of organization decisions
 * 4. **Documentation**: Tests serve as living documentation of structure
 */
class PackageStructureTest {

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
     * Validates that value objects are placed in appropriate domain sub-packages.
     *
     * ## Value Object Placement Rules
     *
     * Value objects can be placed in either:
     * - `domain.value` - Dedicated value object package (recommended)
     * - `domain.model` - General domain model package (legacy support)
     *
     * ## Why Package Placement Matters
     *
     * Consistent placement helps with:
     * 1. **Discoverability**: Developers know where to find value objects
     * 2. **Import Organization**: IDE can organize imports consistently
     * 3. **Build Tool Integration**: Tools can process value objects specially
     * 4. **Code Generation**: Tooling can generate based on package patterns
     *
     * ## Examples
     *
     * ```kotlin
     * // ✅ Good - In value object package
     * package com.example.domain.value
     * @JvmInline
     * value class PersonName(val value: String)
     *
     * // ✅ Also Good - In model package (legacy)
     * package com.example.domain.model
     * @JvmInline
     * value class Email(val value: String)
     *
     * // ❌ Bad - In wrong layer
     * package com.example.application.dto
     * @JvmInline
     * value class PersonName(val value: String)  // Domain concept!
     * ```
     */
    @Test
    fun `domain value objects should be in correct package`() {
        // For now, we'll allow value classes in domain.model package
        // This is a relaxed rule that accepts both locations
        val rule = classes()
            .that().areAnnotatedWith(JvmInline::class.java)
            .and().resideInAPackage("${ArchitectureConstants.DOMAIN_PACKAGE}..")
            .should().resideInAnyPackage(
                "${ArchitectureConstants.DOMAIN_VALUE_OBJECTS}..",
                "${ArchitectureConstants.DOMAIN_PACKAGE}..model..",
            )
            .because("Value objects should be in domain.value_object or domain.model package")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    @Test
    fun `use cases should be in correct package`() {
        val rule = classes()
            .that().haveNameMatching(".*UseCase")
            .and().resideInAPackage("${ArchitectureConstants.APPLICATION_PACKAGE}..")
            .should().resideInAPackage("${ArchitectureConstants.APPLICATION_USE_CASES}..")
            .because("Use cases should be in the application.usecase package")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    @Test
    fun `input port interfaces should be in correct package`() {
        val rule = classes()
            .that().areInterfaces()
            .and().haveNameMatching(".*InputPort")
            .and().resideInAPackage("${ArchitectureConstants.APPLICATION_PACKAGE}..")
            .should().resideInAPackage("${ArchitectureConstants.APPLICATION_INPUT_PORTS}..")
            .because("Input port interfaces should be in application.port.input package")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    @Test
    fun `output port interfaces should be in correct package`() {
        val rule = classes()
            .that().areInterfaces()
            .and().haveNameMatching(".*OutputPort")
            .and().resideInAPackage("${ArchitectureConstants.APPLICATION_PACKAGE}..")
            .should().resideInAPackage("${ArchitectureConstants.APPLICATION_OUTPUT_PORTS}..")
            .because("Output port interfaces should be in application.port.output package")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    @Test
    fun `adapters should be in infrastructure package`() {
        val rule = classes()
            .that().haveNameMatching(".*Adapter")
            .and().resideInAPackage("${ArchitectureConstants.INFRASTRUCTURE_PACKAGE}..")
            .should().resideInAPackage("${ArchitectureConstants.INFRASTRUCTURE_ADAPTERS}..")
            .because("Adapter implementations should be in infrastructure.adapter packages")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    @Test
    fun `domain services should be in correct package`() {
        val rule = classes()
            .that().haveNameMatching(".*Service")
            .and().resideInAPackage("${ArchitectureConstants.DOMAIN_PACKAGE}..")
            .should().resideInAPackage("${ArchitectureConstants.DOMAIN_SERVICES}..")
            .because("Domain services should be in domain.service package")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    @Test
    fun `repository interfaces should be in domain`() {
        val rule = classes()
            .that().areInterfaces()
            .and().haveNameMatching(".*Repository")
            .and().resideInAPackage("${ArchitectureConstants.DOMAIN_PACKAGE}..")
            .should().resideInAPackage("${ArchitectureConstants.DOMAIN_REPOSITORIES}..")
            .because("Repository interfaces belong in domain layer")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    @Test
    fun `domain policies should be in correct package`() {
        val rule = classes()
            .that().haveNameMatching(".*Policy")
            .and().resideInAPackage("${ArchitectureConstants.DOMAIN_PACKAGE}..")
            .should().resideInAPackage("${ArchitectureConstants.DOMAIN_POLICIES}..")
            .because("Domain policies should be in domain.policy package")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    @Test
    fun `error classes should follow package conventions`() {
        val rule = classes()
            .that().haveNameMatching(".*Error")
            .or().haveNameMatching(".*Exception")
            .and().resideInAPackage("${ArchitectureConstants.BASE_PACKAGE}..")
            .should().resideInAnyPackage(
                "${ArchitectureConstants.DOMAIN_ERRORS}..",
                "${ArchitectureConstants.APPLICATION_ERRORS}..",
                "${ArchitectureConstants.INFRASTRUCTURE_PACKAGE}..error..",
                "${ArchitectureConstants.PRESENTATION_PACKAGE}..error..",
                "${ArchitectureConstants.BOOTSTRAP_PACKAGE}..error..",
            )
            .because("Error classes should be in appropriate error packages")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    /**
     * Ensures bootstrap module contains only appropriate startup and configuration classes.
     *
     * ## Bootstrap Module Purity
     *
     * The bootstrap module should be the **thinnest possible layer** containing only:
     * - **CompositionRoot**: Dependency wiring
     * - **App**: Application startup logic
     * - **AppConfig**: Configuration classes
     * - **EntryPoint**: Main function containers
     * - **Config classes**: Settings and environment configuration
     *
     * ## Why This Restriction Matters
     *
     * 1. **Single Responsibility**: Bootstrap has one job - start the application
     * 2. **Testability**: Business logic should be testable without bootstrap
     * 3. **Clarity**: Clear separation between startup and business logic
     * 4. **Maintainability**: Changes to business logic don't affect startup
     *
     * ## Allowed Class Patterns
     *
     * Classes in bootstrap must match these naming patterns:
     * ```kotlin
     * // ✅ Good - Bootstrap classes
     * class CompositionRoot { /* dependency wiring */ }
     * class AppConfig { /* application configuration */ }
     * class App { /* application startup */ }
     * class EntryPoint { /* main function */ }
     * object DatabaseConfig { /* configuration */ }
     *
     * // Special handling for argument parsing
     * class SecureArgParser { /* contains 'parseArgs' in name */ }
     * ```
     *
     * ## Prohibited in Bootstrap
     *
     * ```kotlin
     * // ❌ Bad - Business logic
     * class OrderValidator { /* belongs in domain! */ }
     * class PaymentProcessor { /* belongs in application! */ }
     * class DatabaseRepository { /* belongs in infrastructure! */ }
     * class CliCommand { /* belongs in presentation! */ }
     * ```
     *
     * ## How to Fix Violations
     *
     * If business logic accidentally ends up in bootstrap:
     * 1. **Identify the correct layer** for the logic
     * 2. **Move the class** to appropriate package
     * 3. **Update the composition root** to wire the moved class
     * 4. **Update imports** in dependent classes
     */
    @Test
    fun `bootstrap module should only contain composition root`() {
        val rule = classes()
            .that().resideInAPackage("${ArchitectureConstants.BOOTSTRAP_PACKAGE}..")
            .should().haveNameMatching("(CompositionRoot|App|AppConfig|EntryPoint|.*Config)")
            .orShould().haveSimpleNameContaining("parseArgs")
            .because("Bootstrap module should only contain composition root and configuration")
            .allowEmptyShould(true)

        rule.check(classes)
    }
}
