/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import org.junit.jupiter.api.Test

/**
 * Validates layer dependencies according to our hybrid DDD/Clean/Hexagonal architecture.
 *
 * ## Purpose of Architecture Tests
 *
 * Architecture tests are automated tests that verify architectural rules are followed.
 * They prevent architectural erosion over time by catching violations early in the
 * development process, ideally during CI/CD.
 *
 * ## Layer Dependency Rules
 *
 * Our hybrid architecture enforces strict layer boundaries:
 *
 * ### Domain Layer
 * - **NO dependencies** on any other layers
 * - Contains pure business logic
 * - Must remain framework-agnostic
 *
 * ### Application Layer
 * - Can depend on **Domain only**
 * - Orchestrates domain objects
 * - Defines ports (interfaces) for infrastructure
 *
 * ### Infrastructure Layer
 * - Can depend on **Domain and Application**
 * - Implements application ports
 * - Contains all technical/framework code
 *
 * ### Presentation Layer
 * - Can depend on **Application only**
 * - NOT allowed to access Domain or Infrastructure directly
 * - This ensures presentation changes don't affect business logic
 *
 * ### Bootstrap Layer
 * - **Special case**: Can depend on ALL layers
 * - Acts as the composition root
 * - Wires up all dependencies
 *
 * ## Why These Rules Matter
 *
 * 1. **Testability**: Domain logic can be tested without any infrastructure
 * 2. **Flexibility**: Infrastructure can be swapped without touching domain
 * 3. **Clarity**: Clear boundaries make the codebase easier to understand
 * 4. **Maintainability**: Changes are localized to appropriate layers
 *
 * ## How ArchUnit Works
 *
 * ArchUnit analyzes compiled bytecode to verify architectural rules:
 * 1. Imports all classes from the codebase
 * 2. Builds a model of dependencies between classes
 * 3. Checks this model against defined rules
 * 4. Fails the test if any violations are found
 *
 * ## Common Violations and Fixes
 *
 * ### Domain depending on Application
 * ```kotlin
 * // ❌ Bad - Domain using Application DTO
 * class Order(val items: List<OrderDto>)
 *
 * // ✅ Good - Domain using domain types
 * class Order(val items: List<OrderItem>)
 * ```
 *
 * ### Presentation accessing Domain
 * ```kotlin
 * // ❌ Bad - Controller using domain object
 * fun getOrder(): Order = orderService.findById(id)
 *
 * // ✅ Good - Controller using DTO
 * fun getOrder(): OrderDto = orderService.findById(id)
 * ```
 */
class LayerDependencyTest {

    private val classes = ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
        .withImportOption { location ->
            // Exclude ANTLR generated code and build directories
            !location.contains("generated") &&
                !location.contains("/gen/") &&
                !location.contains("/build/")
        }
        .importPackages(ArchitectureConstants.BASE_PACKAGE)

    @Test
    fun `layers should respect dependency rules`() {
        val rule = layeredArchitecture()
            .consideringAllDependencies()
            // Define layers
            .layer("Domain").definedBy("${ArchitectureConstants.DOMAIN_PACKAGE}..")
            .layer("Application").definedBy("${ArchitectureConstants.APPLICATION_PACKAGE}..")
            .layer("Infrastructure").definedBy("${ArchitectureConstants.INFRASTRUCTURE_PACKAGE}..")
            .layer("Presentation").definedBy("${ArchitectureConstants.PRESENTATION_PACKAGE}..")
            .layer("Bootstrap").definedBy("${ArchitectureConstants.BOOTSTRAP_PACKAGE}..")
            // Define allowed dependencies
            .whereLayer("Domain").mayNotAccessAnyLayer()
            .whereLayer("Application").mayOnlyAccessLayers("Domain")
            .whereLayer("Infrastructure").mayOnlyAccessLayers("Domain", "Application")
            .whereLayer("Presentation").mayOnlyAccessLayers("Application")
            // Bootstrap is special - it can access all layers (composition root)
            .whereLayer("Bootstrap").mayOnlyAccessLayers("Domain", "Application", "Infrastructure", "Presentation")
            .allowEmptyShould(true)

        rule.check(classes)
    }

    @Test
    fun `domain layer should have no external dependencies`() {
        val rule = noClasses()
            .that().resideInAPackage("${ArchitectureConstants.DOMAIN_PACKAGE}..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "${ArchitectureConstants.APPLICATION_PACKAGE}..",
                "${ArchitectureConstants.INFRASTRUCTURE_PACKAGE}..",
                "${ArchitectureConstants.PRESENTATION_PACKAGE}..",
                "${ArchitectureConstants.BOOTSTRAP_PACKAGE}..",
                // Also exclude common frameworks
                "org.springframework..",
                "jakarta..",
                "javax..",
                "org.hibernate..",
            )
            .because("Domain layer must be pure and have no dependencies on other layers or frameworks")
            .allowEmptyShould(true) // Allow if no domain classes exist yet

        rule.check(classes)
    }

    @Test
    fun `presentation should not access domain directly`() {
        val rule = noClasses()
            .that().resideInAPackage("${ArchitectureConstants.PRESENTATION_PACKAGE}..")
            .should().dependOnClassesThat()
            .resideInAPackage("${ArchitectureConstants.DOMAIN_PACKAGE}..")
            .because("Presentation layer should only interact with Application layer, not Domain directly")
            .allowEmptyShould(true) // Allow if no presentation classes exist yet

        rule.check(classes)
    }

    @Test
    fun `presentation should not access infrastructure`() {
        val rule = noClasses()
            .that().resideInAPackage("${ArchitectureConstants.PRESENTATION_PACKAGE}..")
            .should().dependOnClassesThat()
            .resideInAPackage("${ArchitectureConstants.INFRASTRUCTURE_PACKAGE}..")
            .because("Presentation layer should not depend on Infrastructure")
            .allowEmptyShould(true) // Allow if no presentation classes exist yet

        rule.check(classes)
    }

    @Test
    fun `application should not access infrastructure`() {
        val rule = noClasses()
            .that().resideInAPackage("${ArchitectureConstants.APPLICATION_PACKAGE}..")
            .should().dependOnClassesThat()
            .resideInAPackage("${ArchitectureConstants.INFRASTRUCTURE_PACKAGE}..")
            .because("Application layer defines ports but should not depend on their implementations")
            .allowEmptyShould(true) // Allow if no application classes exist yet

        rule.check(classes)
    }

    @Test
    fun `only bootstrap should wire concrete implementations`() {
        val rule = noClasses()
            .that().resideInAnyPackage(
                "${ArchitectureConstants.DOMAIN_PACKAGE}..",
                "${ArchitectureConstants.APPLICATION_PACKAGE}..",
                "${ArchitectureConstants.PRESENTATION_PACKAGE}..",
            )
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "${ArchitectureConstants.INFRASTRUCTURE_PACKAGE}.adapter..",
                "${ArchitectureConstants.INFRASTRUCTURE_PACKAGE}.service..",
            )
            .because("Only the Bootstrap module should know about concrete implementations")
            .allowEmptyShould(true)

        rule.check(classes)
    }
}
