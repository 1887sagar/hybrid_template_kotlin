////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.application.dto

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.abitofhelp.hybrid.application.error.ApplicationError
import com.abitofhelp.hybrid.domain.value.PersonName

/**
 * Command for creating multiple greetings in a batch.
 *
 * ## Self-Validating DTO Pattern
 * This DTO demonstrates comprehensive validation:
 * - Structural validation (size limits, required fields)
 * - Format validation (no duplicates)
 * - Transformation methods for domain objects
 *
 * ## Benefits of DTO Validation
 * 1. **Single Responsibility**: DTOs know their own rules
 * 2. **Reusability**: Validation logic in one place
 * 3. **Testability**: Easy to test validation in isolation
 * 4. **Clear API**: Methods clearly indicate what they do
 *
 * ## Example Usage
 * ```kotlin
 * val command = BatchGreetingCommand(
 *     names = listOf("Alice", "Bob", "Charlie"),
 *     greetingTemplate = "Welcome, {name}!",
 *     maxConcurrent = 5,
 *     stopOnError = false
 * )
 *
 * // Validate the entire command
 * command.validate().fold(
 *     { error -> logger.error("Invalid batch: ${error.message}") },
 *     { logger.info("Valid batch of ${command.names.size} names") }
 * )
 *
 * // Transform to domain objects
 * val personNames = command.toPersonNames()
 * ```
 *
 * @property names List of names to create greetings for
 * @property greetingTemplate Optional custom template (use {name} as placeholder)
 * @property maxConcurrent Maximum concurrent operations (for rate limiting)
 * @property stopOnError Whether to stop batch on first error
 */
data class BatchGreetingCommand(
    val names: List<String>,
    val greetingTemplate: String? = null,
    val maxConcurrent: Int = 10,
    val stopOnError: Boolean = true,
) {

    companion object {
        /**
         * Maximum allowed batch size to prevent resource exhaustion.
         */
        const val MAX_BATCH_SIZE = 1000

        /**
         * Minimum concurrent operations (must be at least 1).
         */
        const val MIN_CONCURRENT = 1

        /**
         * Maximum concurrent operations to prevent system overload.
         */
        const val MAX_CONCURRENT = 100

        /**
         * Default template placeholder for name substitution.
         */
        const val NAME_PLACEHOLDER = "{name}"

        /**
         * Threshold for considering a batch as "large" for special handling.
         */
        const val LARGE_BATCH_THRESHOLD = 100
    }

    /**
     * Validates this batch command comprehensively.
     *
     * ## Validation Rules
     * 1. **Batch Size**: Must have at least 1 name, at most MAX_BATCH_SIZE
     * 2. **Duplicates**: No duplicate names allowed
     * 3. **Concurrency**: Must be between MIN and MAX limits
     * 4. **Template**: If provided, must contain {name} placeholder
     *
     * ## Design Decision
     * We validate structure here but delegate name validation
     * to the domain layer. This maintains proper boundaries.
     *
     * @return Right(Unit) if valid, Left(ApplicationError) with details
     */
    fun validate(): Either<ApplicationError, Unit> {
        val error = validateBatchSize()
            ?: validateNoDuplicates()
            ?: validateConcurrencyLimits()
            ?: validateTemplate()

        return error?.left() ?: Unit.right()
    }

    private fun validateBatchSize(): ApplicationError? = when {
        names.isEmpty() -> ApplicationError.ValidationError(
            field = "names",
            message = "Batch must contain at least one name",
        )
        names.size > MAX_BATCH_SIZE -> ApplicationError.ValidationError(
            field = "names",
            message = "Batch size ${names.size} exceeds maximum of $MAX_BATCH_SIZE",
        )
        else -> null
    }

    private fun validateNoDuplicates(): ApplicationError? {
        val duplicates = names.groupingBy { it }
            .eachCount()
            .filter { it.value > 1 }
            .keys

        return if (duplicates.isNotEmpty()) {
            ApplicationError.ValidationError(
                field = "names",
                message = "Duplicate names found: ${duplicates.joinToString()}",
            )
        } else {
            null
        }
    }

    private fun validateConcurrencyLimits(): ApplicationError? = when {
        maxConcurrent < MIN_CONCURRENT -> ApplicationError.ValidationError(
            field = "maxConcurrent",
            message = "Concurrent limit must be at least $MIN_CONCURRENT",
        )
        maxConcurrent > MAX_CONCURRENT -> ApplicationError.ValidationError(
            field = "maxConcurrent",
            message = "Concurrent limit cannot exceed $MAX_CONCURRENT",
        )
        else -> null
    }

    private fun validateTemplate(): ApplicationError? =
        if (greetingTemplate != null && !greetingTemplate.contains(NAME_PLACEHOLDER)) {
            ApplicationError.ValidationError(
                field = "greetingTemplate",
                message = "Template must contain $NAME_PLACEHOLDER placeholder",
            )
        } else {
            null
        }

    /**
     * Converts all names to PersonName domain objects.
     *
     * ## Batch Transformation
     * Uses Arrow's `traverse` to:
     * - Transform each name to PersonName
     * - Collect all successes or stop on first error
     * - Maintain order of names
     *
     * ## Error Handling
     * - If any name fails validation, returns that error
     * - Error includes which name failed
     *
     * @return Either list of PersonNames or first error encountered
     */
    fun toPersonNames(): Either<ApplicationError, List<PersonName>> {
        val results = mutableListOf<PersonName>()
        for (name in names) {
            when (val result = PersonName.create(name)) {
                is Either.Left -> return ApplicationError.BatchValidationError(
                    item = name,
                    error = ApplicationError.DomainErrorWrapper(result.value),
                ).left()
                is Either.Right -> results.add(result.value)
            }
        }
        return results.right()
    }

    /**
     * Applies the template to a name.
     *
     * ## Template Processing
     * - Uses custom template if provided
     * - Falls back to default greeting format
     * - Safely handles template substitution
     *
     * @param name The name to insert into template
     * @param defaultGreeting Function to generate default greeting
     * @return The formatted greeting
     */
    fun applyTemplate(name: String, defaultGreeting: (String) -> String): String =
        greetingTemplate?.replace(NAME_PLACEHOLDER, name)
            ?: defaultGreeting(name)

    /**
     * Checks if this is a large batch that might need special handling.
     *
     * ## Performance Consideration
     * Large batches might need:
     * - Progress reporting
     * - Chunked processing
     * - Different timeout values
     *
     * @return true if batch has more than 100 items
     */
    fun isLargeBatch(): Boolean = names.size > LARGE_BATCH_THRESHOLD

    /**
     * Splits the batch into chunks for processing.
     *
     * ## Chunking Strategy
     * Useful for:
     * - Rate limiting
     * - Memory management
     * - Progress reporting
     *
     * @param chunkSize Size of each chunk
     * @return List of BatchGreetingCommand objects, each with chunk of names
     */
    fun chunked(chunkSize: Int = maxConcurrent): List<BatchGreetingCommand> =
        names.chunked(chunkSize).map { chunk ->
            copy(names = chunk)
        }
}
