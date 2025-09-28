/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.architecture

/**
 * Constants for architectural tests to ensure consistency across all test classes.
 */
object ArchitectureConstants {

    // Base package
    const val BASE_PACKAGE = "com.abitofhelp.hybrid"

    // Layer packages
    const val DOMAIN_PACKAGE = "$BASE_PACKAGE.domain"
    const val APPLICATION_PACKAGE = "$BASE_PACKAGE.application"
    const val INFRASTRUCTURE_PACKAGE = "$BASE_PACKAGE.infrastructure"
    const val PRESENTATION_PACKAGE = "$BASE_PACKAGE.presentation"
    const val BOOTSTRAP_PACKAGE = "$BASE_PACKAGE.bootstrap"

    // Sub-packages
    const val DOMAIN_VALUE_OBJECTS = "$DOMAIN_PACKAGE.value_object"
    const val DOMAIN_ENTITIES = "$DOMAIN_PACKAGE.entity"
    const val DOMAIN_AGGREGATES = "$DOMAIN_PACKAGE.aggregate"
    const val DOMAIN_SERVICES = "$DOMAIN_PACKAGE.service"
    const val DOMAIN_REPOSITORIES = "$DOMAIN_PACKAGE.repository"
    const val DOMAIN_ERRORS = "$DOMAIN_PACKAGE.error"
    const val DOMAIN_EVENTS = "$DOMAIN_PACKAGE.event"
    const val DOMAIN_POLICIES = "$DOMAIN_PACKAGE.policy"

    const val APPLICATION_USE_CASES = "$APPLICATION_PACKAGE.usecase"
    const val APPLICATION_PORTS = "$APPLICATION_PACKAGE.port"
    const val APPLICATION_INPUT_PORTS = "$APPLICATION_PACKAGE.port.input"
    const val APPLICATION_OUTPUT_PORTS = "$APPLICATION_PACKAGE.port.output"
    const val APPLICATION_SERVICES = "$APPLICATION_PACKAGE.service"
    const val APPLICATION_ERRORS = "$APPLICATION_PACKAGE.error"
    const val APPLICATION_DTOs = "$APPLICATION_PACKAGE.dto"

    const val INFRASTRUCTURE_ADAPTERS = "$INFRASTRUCTURE_PACKAGE.adapter"
    const val INFRASTRUCTURE_OUTPUT_ADAPTERS = "$INFRASTRUCTURE_PACKAGE.adapter.output"
    const val INFRASTRUCTURE_SERVICES = "$INFRASTRUCTURE_PACKAGE.service"

    const val PRESENTATION_CLI = "$PRESENTATION_PACKAGE.cli"
    const val PRESENTATION_COMMANDS = "$PRESENTATION_PACKAGE.cli.commands"

    // Test packages to exclude
    val TEST_PACKAGES = setOf(
        "..test..",
        "..tests..",
        "..testing..",
        "..mock..",
        "..fixture..",
    )

    // Generated code to exclude from architecture tests
    val GENERATED_PACKAGES = setOf(
        "..generated..",
        "..build..",
    )
}
