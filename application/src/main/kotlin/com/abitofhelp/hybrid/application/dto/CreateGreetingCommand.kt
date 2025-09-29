// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.application.dto

import arrow.core.Either
import arrow.core.right
import com.abitofhelp.hybrid.application.error.ApplicationError
import com.abitofhelp.hybrid.domain.value.PersonName

/**
 * Command for creating a greeting.
 *
 * ## What is a Command?
 * A command is a Data Transfer Object (DTO) that represents a user's intention
 * to perform an action. It carries all the data needed for that action.
 *
 * ## Command vs Domain Object
 * - **Command**: Raw data from user input (might be invalid)
 * - **Domain Object**: Validated, business-rule-compliant data
 *
 * The application layer validates commands and converts them to domain objects.
 *
 * ## Self-Validating DTOs
 * This DTO includes validation methods to:
 * - Encapsulate validation logic
 * - Provide consistent error messages
 * - Make use cases cleaner
 * - Enable reuse across different use cases
 *
 * ## What is a Data Class?
 * `data class` in Kotlin automatically provides:
 * - `equals()` and `hashCode()` based on properties
 * - `toString()` that shows all properties
 * - `copy()` function to create modified copies
 * - Destructuring support
 *
 * Perfect for DTOs that just carry data!
 *
 * ## Example Usage
 * ```kotlin
 * // Creating a command from user input
 * val command = CreateGreetingCommand(
 *     name = userInput.trim().ifEmpty { null },
 *     silent = args.contains("--silent")
 * )
 *
 * // Validate the command
 * val validation = command.validate()
 * validation.fold(
 *     { error -> println("Invalid: ${error.message}") },
 *     { println("Valid command") }
 * )
 *
 * // Or validate and transform to domain object
 * val personName = command.toPersonName()
 * ```
 *
 * @property name The name to greet. Nullable because users might not provide one.
 * @property silent Whether to suppress output (useful for testing or batch operations)
 */
data class CreateGreetingCommand(
    /**
     * The name to create a greeting for.
     * - `null` means no name was provided (will use "Anonymous")
     * - Empty string will be treated as invalid
     * - Whitespace will be trimmed during validation
     */
    val name: String? = null,

    /**
     * Controls whether the greeting should be sent to output.
     * - `true`: Create greeting but don't send it (useful for testing)
     * - `false`: Create and send greeting normally (default)
     */
    val silent: Boolean = false,
) {

    /**
     * Validates this command for basic structural correctness.
     *
     * ## Validation Rules
     * - No rules for `silent` (any boolean is valid)
     * - Name validation is delegated to domain layer
     *
     * ## Why Minimal Validation Here?
     * DTOs should only validate:
     * - Structural correctness (required fields present)
     * - Basic type constraints
     * - Format validation (emails, URLs, etc.)
     *
     * Business rules belong in the domain layer!
     *
     * @return Right(Unit) if valid, Left(ApplicationError) if invalid
     */
    fun validate(): Either<ApplicationError, Unit> {
        // Currently, all CreateGreetingCommand instances are structurally valid
        // The actual name validation happens in the domain layer
        return Unit.right()
    }

    /**
     * Converts the name to a PersonName domain object.
     *
     * ## Transformation Logic
     * - Null/blank names → PersonName.anonymous()
     * - Non-blank names → PersonName.create() with validation
     *
     * ## Error Handling
     * Domain validation errors are wrapped in ApplicationError
     * to maintain layer boundaries.
     *
     * @return Either PersonName or ApplicationError
     */
    fun toPersonName(): Either<ApplicationError, PersonName> =
        if (name.isNullOrBlank()) {
            // No name provided - use anonymous
            PersonName.anonymous().right()
        } else {
            // Try to create valid PersonName
            PersonName.create(name)
                .mapLeft { ApplicationError.DomainErrorWrapper(it) }
        }

    /**
     * Checks if output should be suppressed.
     *
     * ## Convenience Method
     * Makes use case code more readable:
     * ```kotlin
     * if (command.shouldSendOutput()) {
     *     outputPort.send(greeting)
     * }
     * ```
     *
     * @return true if output should be sent, false if silent
     */
    fun shouldSendOutput(): Boolean = !silent
}
