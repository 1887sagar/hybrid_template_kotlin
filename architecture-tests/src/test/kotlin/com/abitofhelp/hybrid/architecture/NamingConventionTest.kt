/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.architecture

import com.tngtech.archunit.core.domain.JavaModifier
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.junit.jupiter.api.Test

/**
 * Validates naming conventions according to CLAUDE-Kotlin.md standards.
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

    @Test
    fun `use cases should follow naming convention`() {
        val rule = classes()
            .that().resideInAPackage("${ArchitectureConstants.APPLICATION_USE_CASES}..")
            .should().haveNameMatching(".*UseCase")
            .because("Use cases should end with 'UseCase' suffix")
            .allowEmptyShould(true)

        rule.check(classes)
    }

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

    @Test
    fun `domain policies should follow naming`() {
        val rule = classes()
            .that().resideInAPackage("${ArchitectureConstants.DOMAIN_POLICIES}..")
            .should().haveNameMatching(".*Policy")
            .because("Domain policies should end with 'Policy' suffix")
            .allowEmptyShould(true)

        rule.check(classes)
    }

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

    @Test
    fun `commands should follow naming convention`() {
        val rule = classes()
            .that().resideInAPackage("${ArchitectureConstants.PRESENTATION_COMMANDS}..")
            .should().haveNameMatching(".*Command")
            .because("CLI commands should end with 'Command' suffix")
            .allowEmptyShould(true)

        rule.check(classes)
    }

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
