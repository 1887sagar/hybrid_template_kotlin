// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Test Suite
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.architecture

/**
 * Centralized constants for architectural tests to ensure consistency and maintainability.
 *
 * ## Purpose of This Constants File
 *
 * This object serves as the **single source of truth** for package structure definitions
 * used throughout all architecture tests. It ensures:
 *
 * 1. **Consistency**: All tests use the same package definitions
 * 2. **Maintainability**: Changes to package structure only need updates in one place
 * 3. **Readability**: Clear, documented constants instead of magic strings
 * 4. **Refactoring Safety**: IDE can find all usages when packages change
 * 5. **Team Alignment**: Shared understanding of architectural organization
 *
 * ## How to Use These Constants
 *
 * When writing architecture tests, always reference these constants instead of
 * hardcoding package names:
 *
 * ```kotlin
 * // ✅ Good - Using constants
 * classes()
 *     .that().resideInAPackage("${ArchitectureConstants.DOMAIN_PACKAGE}..")
 *     .should().onlyDependOnClassesThat()
 *     .resideInAPackage("${ArchitectureConstants.DOMAIN_PACKAGE}..")
 *
 * // ❌ Bad - Hardcoded strings
 * classes()
 *     .that().resideInAPackage("com.abitofhelp.hybrid.domain..")
 *     .should().onlyDependOnClassesThat()
 *     .resideInAPackage("com.abitofhelp.hybrid.domain..")
 * ```
 *
 * ## Package Naming Strategy
 *
 * Our package names follow these principles:
 * - **Hierarchical**: Clear parent-child relationships
 * - **Descriptive**: Names indicate purpose and content
 * - **Consistent**: Same patterns across all layers
 * - **Scalable**: Structure supports growth
 *
 * ## When to Update These Constants
 *
 * Update these constants when:
 * - Adding new architectural layers
 * - Restructuring package organization
 * - Adding new sub-packages within layers
 * - Renaming packages for clarity
 *
 * All architecture tests will automatically use the new structure.
 */
object ArchitectureConstants {

    // Base package - Root of all application code
    const val BASE_PACKAGE = "com.abitofhelp.hybrid"

    // Layer packages - Primary architectural boundaries
    const val DOMAIN_PACKAGE = "$BASE_PACKAGE.domain" // Core business logic (no dependencies)
    const val APPLICATION_PACKAGE = "$BASE_PACKAGE.application" // Use cases and application services
    const val INFRASTRUCTURE_PACKAGE = "$BASE_PACKAGE.infrastructure" // Technical implementations and adapters
    const val PRESENTATION_PACKAGE = "$BASE_PACKAGE.presentation" // User interface and external API layers
    const val BOOTSTRAP_PACKAGE = "$BASE_PACKAGE.bootstrap" // Application startup and dependency wiring

    // Domain layer sub-packages - Core business concepts
    const val DOMAIN_VALUE_OBJECTS = "$DOMAIN_PACKAGE.value_object" // Immutable value types (PersonName, Money)
    const val DOMAIN_ENTITIES = "$DOMAIN_PACKAGE.entity" // Business entities with identity (Order, Customer)
    const val DOMAIN_AGGREGATES = "$DOMAIN_PACKAGE.aggregate" // Consistency boundaries (OrderAggregate)
    const val DOMAIN_SERVICES = "$DOMAIN_PACKAGE.service" // Domain logic that doesn't fit in entities
    const val DOMAIN_REPOSITORIES = "$DOMAIN_PACKAGE.repository" // Data access interfaces (defined by domain)
    const val DOMAIN_ERRORS = "$DOMAIN_PACKAGE.error" // Domain-specific error types
    const val DOMAIN_EVENTS = "$DOMAIN_PACKAGE.event" // Domain events for communication
    const val DOMAIN_POLICIES = "$DOMAIN_PACKAGE.policy" // Business rules and decision logic

    // Application layer sub-packages - Use cases and orchestration
    const val APPLICATION_USE_CASES = "$APPLICATION_PACKAGE.usecase" // Use case implementations (business workflows)
    const val APPLICATION_PORTS = "$APPLICATION_PACKAGE.port" // All port interfaces (input + output)
    const val APPLICATION_INPUT_PORTS = "$APPLICATION_PACKAGE.port.input" // Driving ports (what app can do)
    const val APPLICATION_OUTPUT_PORTS = "$APPLICATION_PACKAGE.port.output" // Driven ports (what app needs)
    const val APPLICATION_SERVICES = "$APPLICATION_PACKAGE.service" // Application-specific services
    const val APPLICATION_ERRORS = "$APPLICATION_PACKAGE.error" // Application-layer error types
    const val APPLICATION_DTOs = "$APPLICATION_PACKAGE.dto" // Data transfer objects for external communication

    // Infrastructure layer sub-packages - Technical implementations
    const val INFRASTRUCTURE_ADAPTERS = "$INFRASTRUCTURE_PACKAGE.adapter" // All adapter implementations
    const val INFRASTRUCTURE_OUTPUT_ADAPTERS = "$INFRASTRUCTURE_PACKAGE.adapter.output" // Output port implementations
    const val INFRASTRUCTURE_SERVICES = "$INFRASTRUCTURE_PACKAGE.service" // Infrastructure service implementations

    // Presentation layer sub-packages - User interfaces
    const val PRESENTATION_CLI = "$PRESENTATION_PACKAGE.cli" // Command-line interface components
    const val PRESENTATION_COMMANDS = "$PRESENTATION_PACKAGE.cli.commands" // CLI command implementations

    // Test packages to exclude from architecture analysis
    val TEST_PACKAGES = setOf(
        "..test..", // Standard test packages
        "..tests..", // Alternative test naming
        "..testing..", // Testing utility packages
        "..mock..", // Mock object packages
        "..fixture..", // Test fixture packages
    )

    // Generated code to exclude from architecture analysis
    val GENERATED_PACKAGES = setOf(
        "..generated..", // Code generated by annotation processors
        "..build..", // Build tool generated classes
    )
}
