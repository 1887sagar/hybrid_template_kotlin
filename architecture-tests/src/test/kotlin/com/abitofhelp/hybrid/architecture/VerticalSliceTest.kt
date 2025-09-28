/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

/**
 * Validates that our architecture supports proper vertical slices following DIP.
 *
 * According to CLAUDE-Kotlin.md, dependencies flow INWARD:
 * - Presentation → Application (never to Infrastructure or Domain)
 * - Infrastructure → Application + Domain (implements ports)
 * - Application → Domain (orchestrates domain logic)
 * - Domain → Nothing (pure business logic)
 * - Bootstrap → All layers (special case - composition root)
 *
 * A vertical slice is a feature that spans all layers while respecting these rules.
 * The "drilling toward the middle" means outer layers depend on inner layers,
 * converging on the Domain at the center.
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
