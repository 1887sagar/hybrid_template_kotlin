// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.domain.value

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.abitofhelp.hybrid.domain.error.DomainError

/**
 * A value object that represents a person's name with validation rules.
 *
 * ## What is a Value Object?
 * A value object is a small, immutable object that represents a concept from your
 * business domain. Unlike entities (which have identity), value objects are defined
 * only by their attributes. Two PersonName objects with the same value are considered equal.
 *
 * ## Why Use Value Objects?
 * - **Type Safety**: Instead of passing strings around, we have a specific type
 * - **Validation**: Business rules are enforced at creation time
 * - **Self-Documenting**: The code clearly shows what kind of data is expected
 *
 * ## Business Rules
 * This value object enforces these rules:
 * - Name cannot be blank (empty or only whitespace)
 * - Name must be between 1 and 100 characters long
 * - Name can only contain letters, spaces, hyphens, and apostrophes
 *
 * ## Example Usage
 * ```kotlin
 * // Creating a valid name
 * val nameResult = PersonName.create("John Doe")
 * nameResult.fold(
 *     { error -> println("Invalid: ${error.message}") },
 *     { name -> println("Hello, ${name.value}!") }
 * )
 *
 * // Creating an anonymous name
 * val anonymous = PersonName.anonymous()
 * println(anonymous.value) // "Anonymous"
 * ```
 *
 * @property value The validated name string
 * @constructor Private constructor to force use of factory methods
 */
@JvmInline
value class PersonName private constructor(val value: String) {
    companion object {
        /** Minimum allowed length for a name */
        private const val MIN_LENGTH = 1

        /** Maximum allowed length for a name */
        private const val MAX_LENGTH = 100

        /**
         * Regular expression pattern for valid names.
         * Allows: letters (a-z, A-Z), spaces, hyphens (-), and apostrophes (')
         */
        private val VALID_NAME_PATTERN = Regex("""^[a-zA-Z\s\-']+$""")

        /**
         * Creates a PersonName with validation.
         *
         * This is the main factory method for creating PersonName instances.
         * It validates the input against all business rules before creating the object.
         *
         * ## What is Either?
         * Either is a type that represents one of two possible values:
         * - Left: Contains an error (in this case, a ValidationError)
         * - Right: Contains a success value (in this case, a valid PersonName)
         *
         * ## Example
         * ```kotlin
         * when (val result = PersonName.create("John")) {
         *     is Either.Left -> println("Error: ${result.value.message}")
         *     is Either.Right -> println("Success: ${result.value.value}")
         * }
         * ```
         *
         * @param value The name string to validate and wrap
         * @return Either a ValidationError (Left) if validation fails, or a valid PersonName (Right)
         */
        fun create(value: String): Either<DomainError.ValidationError, PersonName> {
            val trimmed = value.trim()

            return when {
                trimmed.isBlank() ->
                    DomainError.ValidationError("name", "Name cannot be blank").left()

                trimmed.length < MIN_LENGTH || trimmed.length > MAX_LENGTH ->
                    DomainError.ValidationError(
                        "name",
                        "Name must be between $MIN_LENGTH and $MAX_LENGTH characters",
                    ).left()

                !trimmed.matches(VALID_NAME_PATTERN) ->
                    DomainError.ValidationError(
                        "name",
                        "Name can only contain letters, spaces, hyphens, and apostrophes",
                    ).left()

                else -> PersonName(trimmed).right()
            }
        }

        /**
         * Creates an anonymous PersonName for when no name is provided.
         *
         * This is useful when:
         * - Users choose not to provide their name
         * - Default values are needed for testing
         * - You want to ensure there's always a valid PersonName
         *
         * ## Example
         * ```kotlin
         * val user = PersonName.anonymous()
         * println("Welcome, ${user.value}!") // "Welcome, Anonymous!"
         * ```
         *
         * @return A PersonName with the value "Anonymous"
         */
        fun anonymous(): PersonName = PersonName("Anonymous")
    }
}
