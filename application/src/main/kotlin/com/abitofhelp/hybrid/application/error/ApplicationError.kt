// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.application.error

import com.abitofhelp.hybrid.domain.error.DomainError

/**
 * Application layer errors.
 *
 * ## Application vs Domain Errors
 * - **Domain Errors**: Business rule violations (e.g., "Name too long")
 * - **Application Errors**: Use case and orchestration failures (e.g., "Failed to save")
 *
 * ## Why Separate Error Types?
 * Each layer has its own concerns:
 * - Domain layer: Pure business logic errors
 * - Application layer: Orchestration, integration, and use case errors
 * - Infrastructure layer: Technical errors (handled and wrapped here)
 *
 * ## Error Handling Strategy
 * ```kotlin
 * // In a use case
 * class CreateUserUseCase(
 *     private val userRepo: UserRepository,
 *     private val emailService: EmailService
 * ) {
 *     suspend fun execute(command: CreateUserCommand): Either<ApplicationError, User> {
 *         // Validate domain rules
 *         val user = User.create(command.email).fold(
 *             { domainError ->
 *                 // Wrap domain error for application layer
 *                 return ApplicationError.DomainErrorWrapper(domainError).left()
 *             },
 *             { it }
 *         )
 *
 *         // Try to save (might fail)
 *         return userRepo.save(user).fold(
 *             {
 *                 ApplicationError.UseCaseError(
 *                     "CreateUser",
 *                     "Failed to save user"
 *                 ).left()
 *             },
 *             { savedUser ->
 *                 // Send email (non-critical, log if fails)
 *                 emailService.sendWelcome(savedUser.email)
 *                 savedUser.right()
 *             }
 *         )
 *     }
 * }
 * ```
 */
sealed class ApplicationError {
    /**
     * Errors specific to use case execution.
     *
     * Use this when:
     * - A use case cannot complete its operation
     * - Multiple steps fail in a transaction
     * - Business process cannot proceed
     *
     * ## Examples
     * ```kotlin
     * // Repository not available
     * UseCaseError(
     *     useCase = "CreateOrder",
     *     cause = "Product inventory service unavailable"
     * )
     *
     * // Complex validation failure
     * UseCaseError(
     *     useCase = "TransferFunds",
     *     cause = "Insufficient balance for transfer and fees"
     * )
     * ```
     *
     * @property useCase The name of the use case that failed
     * @property cause Human-readable explanation of the failure
     */
    data class UseCaseError(
        val useCase: String,
        val cause: String,
    ) : ApplicationError()

    /**
     * Errors from output ports (external communication failures).
     *
     * Common scenarios:
     * - File write failures (permissions, disk full)
     * - Network errors (API down, timeout)
     * - Message queue failures
     * - Database connection issues
     *
     * ## Example
     * ```kotlin
     * // In an output adapter
     * override suspend fun send(message: String): Either<ApplicationError, Unit> {
     *     return try {
     *         apiClient.post("/messages", message)
     *         Unit.right()
     *     } catch (e: IOException) {
     *         ApplicationError.OutputError(
     *             "Failed to send message: ${e.message}"
     *         ).left()
     *     }
     * }
     * ```
     *
     * @property message Description of what went wrong with the output operation
     */
    data class OutputError(
        val message: String,
    ) : ApplicationError()

    /**
     * Validation error for application-level input.
     *
     * Use this for DTO validation that doesn't belong in domain:
     * - Format validation (emails, URLs, phone numbers)
     * - Required field checks
     * - Size limits
     * - Structural validation
     *
     * @property field The field that failed validation
     * @property message Explanation of the validation failure
     */
    data class ValidationError(
        val field: String,
        val message: String,
    ) : ApplicationError()

    /**
     * Batch processing error when one item in a batch fails.
     *
     * Useful for batch operations where you need to know:
     * - Which specific item failed
     * - What the error was
     * - Whether to continue or stop
     *
     * @property item String representation of the failed item
     * @property error The specific error for this item
     */
    data class BatchValidationError(
        val item: String,
        val error: ApplicationError,
    ) : ApplicationError()

    /**
     * Wraps domain errors for the application layer.
     *
     * ## Why Wrap Domain Errors?
     * Domain errors are perfect within the domain, but at the application boundary:
     * - We might need to add context
     * - We might need to transform for presentation
     * - We maintain layer separation
     *
     * ## The userMessage Property
     * This computed property creates user-friendly messages from domain errors.
     * It's automatically generated based on the error type - no manual work needed!
     *
     * ## Example Usage
     * ```kotlin
     * // Domain operation returns domain error
     * val result = PersonName.create("")
     *
     * result.fold(
     *     { domainError ->
     *         // Wrap for application layer
     *         val appError = ApplicationError.DomainErrorWrapper(domainError)
     *
     *         // Can access original error
     *         logger.debug("Domain error: $domainError")
     *
     *         // Or user-friendly message
     *         console.println(appError.userMessage) // "name: Name cannot be blank"
     *
     *         appError.left()
     *     },
     *     { validName -> ... }
     * )
     * ```
     *
     * @property domainError The original domain error being wrapped
     */
    data class DomainErrorWrapper(
        val domainError: DomainError,
    ) : ApplicationError() {
        /**
         * Provides a user-friendly message based on the domain error type.
         *
         * This property automatically formats domain errors for display:
         * - ValidationError: "field: message" format
         * - BusinessRuleViolation: "rule - reason" format
         * - NotFound: "Entity with id 'X' not found" format
         *
         * The `when` expression is exhaustive - if new domain errors are added,
         * the compiler will force us to handle them here!
         */
        val userMessage: String = when (domainError) {
            is DomainError.ValidationError -> "${domainError.field}: ${domainError.message}"
            is DomainError.BusinessRuleViolation -> "${domainError.rule} - ${domainError.reason}"
            is DomainError.NotFound -> "${domainError.entity} with id '${domainError.id}' not found"
        }
    }
}
