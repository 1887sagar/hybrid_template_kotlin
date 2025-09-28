// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.infrastructure.adapter.output

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.abitofhelp.hybrid.application.error.ApplicationError
import com.abitofhelp.hybrid.application.port.output.OutputPort
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Composite output adapter that sends messages to multiple outputs concurrently.
 *
 * ## The Composite Pattern
 * This implements the Composite design pattern:
 * - **Component**: OutputPort interface
 * - **Leaf**: ConsoleOutputAdapter, FileOutputAdapter
 * - **Composite**: This class, which contains multiple OutputPorts
 *
 * The beauty? Code using OutputPort doesn't know if it's talking to one output or many!
 *
 * ## Use Cases
 * - **Development**: Console output for immediate feedback
 * - **Production**: File output for persistence, API for monitoring
 * - **Testing**: Multiple test spies to verify behavior
 * - **Redundancy**: Multiple outputs for fault tolerance
 *
 * ## Concurrent Execution
 * Messages are sent to all outputs simultaneously using coroutines:
 * ```kotlin
 * outputs.map { output ->
 *     async { output.send(message) }  // Launch concurrent coroutine
 * }.awaitAll()  // Wait for all to complete
 * ```
 *
 * This means if you have 3 outputs that each take 1 second,
 * total time is 1 second, not 3!
 *
 * ## Error Handling Philosophy
 * - **Attempts all outputs**: One failure doesn't stop others
 * - **Reports all failures**: You see everything that went wrong
 * - **Partial success = failure**: For consistency and safety
 *
 * ## Example Usage
 * ```kotlin
 * // Multiple outputs
 * val composite = CompositeOutputAdapter(
 *     listOf(
 *         ConsoleOutputAdapter(),
 *         FileOutputAdapter("/logs/app.log"),
 *         ApiOutputAdapter("https://logs.example.com")
 *     )
 * )
 *
 * // Use it like any single output
 * composite.send("Application started")
 * // Goes to console AND file AND API concurrently!
 * ```
 *
 * @property outputs The list of OutputPort implementations to send to
 */
class CompositeOutputAdapter(
    private val outputs: List<OutputPort>,
) : OutputPort {

    /**
     * Sends the message to all configured outputs concurrently.
     *
     * ## Execution Flow
     * 1. Launches a coroutine for each output
     * 2. All outputs run in parallel
     * 3. Waits for all to complete
     * 4. Collects and combines any errors
     *
     * ## The coroutineScope Function
     * `coroutineScope` creates a scope where:
     * - All child coroutines must complete before returning
     * - If any child fails with exception, all others are cancelled
     * - Structured concurrency ensures no leaked coroutines
     *
     * ## Error Strategy
     * Even if some outputs fail, all are attempted. This ensures:
     * - Critical logs aren't lost due to non-critical failures
     * - You get complete error information
     * - Debugging is easier with all failure details
     *
     * Returns an error if ANY output fails, but the message may have
     * been successfully sent to some outputs.
     *
     * @param message The message to send to all outputs
     * @return Right(Unit) if all succeed, Left(ApplicationError) if any fail
     */
    override suspend fun send(message: String): Either<ApplicationError, Unit> = coroutineScope {
        // Launch all outputs concurrently
        // Each 'async' creates a new coroutine that runs in parallel
        val results = outputs.map { output ->
            async {
                output.send(message)
            }
        }.awaitAll() // Wait for all coroutines to complete

        // Collect any errors from the results
        // mapNotNull: Maps each result, keeping only non-null values
        val errors = results.mapNotNull { result ->
            result.fold(
                { error -> error }, // If error, keep it
                { null }, // If success, filter out (null)
            )
        }

        // Determine overall success/failure
        if (errors.isEmpty()) {
            // All outputs succeeded!
            Unit.right()
        } else {
            // Combine multiple errors into one comprehensive error
            ApplicationError.OutputError(
                "Failed to send to ${errors.size} of ${outputs.size} outputs: " +
                    errors.joinToString("; ") { error ->
                        // Extract meaningful message based on error type
                        when (error) {
                            is ApplicationError.OutputError -> error.message
                            is ApplicationError.DomainErrorWrapper -> error.userMessage
                            is ApplicationError.UseCaseError -> "${error.useCase}: ${error.cause}"
                            is ApplicationError.ValidationError -> "${error.field}: ${error.message}"
                            is ApplicationError.BatchValidationError -> 
                                "Batch error for '${error.item}': ${error.error}"
                        }
                    },
            ).left()
        }
    }

    companion object {
        /**
         * Factory method to create a default output configuration.
         *
         * ## Companion Objects
         * In Kotlin, companion objects are like static members in Java.
         * They belong to the class, not instances, perfect for factory methods!
         *
         * ## Smart Creation Logic
         * - No file path → Just console output (no need for composite)
         * - File path provided → Both console AND file (composite needed)
         *
         * This optimization avoids unnecessary wrapping when there's only
         * one output, improving performance slightly.
         *
         * ## Usage Examples
         * ```kotlin
         * // Console only
         * val output1 = CompositeOutputAdapter.createDefault()
         *
         * // Console + File
         * val output2 = CompositeOutputAdapter.createDefault("/tmp/app.log")
         *
         * // In dependency injection
         * @Provides
         * fun provideOutputPort(@Named("logFile") filePath: String?): OutputPort =
         *     CompositeOutputAdapter.createDefault(filePath)
         * ```
         *
         * @param filePath Optional file path for file output
         * @return OutputPort configured based on parameters
         */
        fun createDefault(filePath: String? = null): OutputPort {
            // Always include console output
            val outputs = mutableListOf<OutputPort>(ConsoleOutputAdapter())

            // Add file output if path provided
            // The '?.let' pattern safely handles null
            filePath?.let { outputs.add(FileOutputAdapter(it)) }

            // Return appropriate implementation
            return if (outputs.size == 1) {
                // Single output - return directly, no composite needed
                outputs.first()
            } else {
                // Multiple outputs - wrap in composite
                CompositeOutputAdapter(outputs)
            }
        }
    }
}
