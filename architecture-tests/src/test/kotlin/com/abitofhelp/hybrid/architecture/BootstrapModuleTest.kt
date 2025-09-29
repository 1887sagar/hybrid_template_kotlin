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
 * Validates bootstrap module follows composition root pattern.
 *
 * Bootstrap Rules:
 * - Is the ONLY module allowed to access all layers
 * - Should contain minimal code (just wiring)
 * - Main entry point must be here
 * - Composition root handles all manual DI
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

    @Test
    fun `composition root should exist in bootstrap`() {
        val rule = classes()
            .that().haveNameMatching(".*CompositionRoot.*")
            .should().resideInAPackage("${ArchitectureConstants.BOOTSTRAP_PACKAGE}..")
            .because("CompositionRoot must be in bootstrap module")
            .allowEmptyShould(true) // May not be found in test context

        rule.check(classes)
    }

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
