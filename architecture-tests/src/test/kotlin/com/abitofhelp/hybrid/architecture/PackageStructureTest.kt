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
import org.junit.jupiter.api.Test

/**
 * Validates package structure conventions for our architecture.
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
