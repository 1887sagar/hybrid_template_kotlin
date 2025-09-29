////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.domain.error

/**
 * Base type for all domain-layer errors.
 *
 * ## What is a Sealed Class?
 * A sealed class is like an enum, but more flexible. It defines a closed set of subclasses
 * that must be defined in the same file. This allows the compiler to know all possible
 * subtypes at compile time.
 *
 * ## Why Use Sealed Classes for Errors?
 * - **Type Safety**: The compiler ensures you handle all error cases
 * - **Exhaustive Checking**: When using `when` expressions, the compiler warns if you miss a case
 * - **Rich Data**: Unlike enums, each error type can carry its own data
 *
 * ## Example Usage
 * ```kotlin
 * fun handleError(error: DomainError) {
 *     when (error) {
 *         is DomainError.ValidationError ->
 *             println("Invalid ${error.field}: ${error.message}")
 *         is DomainError.BusinessRuleViolation ->
 *             println("Rule '${error.rule}' violated: ${error.reason}")
 *         is DomainError.NotFound ->
 *             println("${error.entity} with id ${error.id} not found")
 *     }
 *     // No 'else' needed - compiler knows all cases are covered!
 * }
 * ```
 */
sealed class DomainError {
    /**
     * Represents validation failures for domain objects.
     *
     * Use this error when:
     * - Input data doesn't meet format requirements (e.g., invalid email format)
     * - Values are outside acceptable ranges (e.g., negative age)
     * - Required fields are missing or blank
     *
     * ## Example
     * ```kotlin
     * // Creating a validation error
     * val error = DomainError.ValidationError(
     *     field = "email",
     *     message = "Email must contain @ symbol"
     * )
     * ```
     *
     * @property field The name of the field that failed validation
     * @property message A human-readable description of what went wrong
     */
    data class ValidationError(
        val field: String,
        val message: String,
    ) : DomainError()

    /**
     * Represents violations of business rules that go beyond simple validation.
     *
     * ## Validation vs Business Rules
     * - **Validation**: "Is this email format correct?" (technical constraint)
     * - **Business Rule**: "Can this user place an order?" (business logic)
     *
     * Use this error when:
     * - A user tries an action they're not authorized for
     * - Business logic prevents an operation (e.g., "Cannot withdraw more than balance")
     * - Complex rules involving multiple entities fail
     *
     * ## Example
     * ```kotlin
     * // User tries to order but hasn't verified their email
     * val error = DomainError.BusinessRuleViolation(
     *     rule = "email-verification-required",
     *     reason = "Users must verify email before placing orders"
     * )
     * ```
     *
     * @property rule A code or identifier for the violated rule
     * @property reason A human-readable explanation of why the rule was violated
     */
    data class BusinessRuleViolation(
        val rule: String,
        val reason: String,
    ) : DomainError()

    /**
     * Represents attempts to access entities that don't exist.
     *
     * ## What is an Entity?
     * An entity is a domain object with a unique identity that persists over time.
     * Examples: User, Order, Product (things with IDs).
     *
     * Use this error when:
     * - Looking up a record by ID that doesn't exist
     * - Trying to update/delete something already removed
     * - Following a reference to a missing related entity
     *
     * ## Example
     * ```kotlin
     * // Looking for a user that doesn't exist
     * val error = DomainError.NotFound(
     *     entity = "User",
     *     id = "user-123"
     * )
     *
     * // In your code:
     * userRepository.findById(userId).fold(
     *     { error -> println("Could not find ${error.entity} with id ${error.id}") },
     *     { user -> println("Found user: ${user.name}") }
     * )
     * ```
     *
     * @property entity The type of entity that wasn't found (e.g., "User", "Order")
     * @property id The identifier that was searched for
     */
    data class NotFound(
        val entity: String,
        val id: String,
    ) : DomainError()
}
