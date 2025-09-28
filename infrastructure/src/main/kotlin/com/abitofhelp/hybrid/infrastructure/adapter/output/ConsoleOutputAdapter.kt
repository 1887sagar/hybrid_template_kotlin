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

/**
 * Console implementation of the OutputPort.
 *
 * ## What is an Adapter?
 * In hexagonal architecture, an adapter is a concrete implementation of a port.
 * It "adapts" between your application's needs and the external world:
 * - Port (interface): What the application needs
 * - Adapter (implementation): How it's actually done
 *
 * ## Infrastructure Layer Responsibilities
 * The infrastructure layer handles all technical concerns:
 * - I/O operations (files, console, network)
 * - Database access
 * - External API calls
 * - Framework integrations
 *
 * ## This Adapter
 * This simple adapter writes messages to the console (stdout).
 * Despite being simple, it still:
 * - Validates input
 * - Handles errors gracefully
 * - Returns proper Either types
 *
 * ## Example Usage
 * ```kotlin
 * // In dependency injection / composition root
 * val outputPort: OutputPort = ConsoleOutputAdapter()
 * val useCase = CreateGreetingUseCase(greetingService, outputPort)
 *
 * // The use case doesn't know it's using console output!
 * useCase.execute(command)
 * ```
 *
 * ## Testing Tip
 * For tests, you could create a TestOutputAdapter that collects messages:
 * ```kotlin
 * class TestOutputAdapter : OutputPort {
 *     val messages = mutableListOf<String>()
 *
 *     override suspend fun send(message: String): Either<ApplicationError, Unit> {
 *         messages.add(message)
 *         return Unit.right()
 *     }
 * }
 * ```
 */
class ConsoleOutputAdapter : OutputPort {

    /**
     * Sends a message to the console (stdout).
     *
     * ## Implementation Details
     *
     * ### Validation
     * Uses Kotlin's `require` function which throws `IllegalArgumentException`
     * if the condition is false. This ensures we never print empty lines.
     *
     * ### Error Handling Strategy
     * Multiple catch blocks handle different scenarios:
     * 1. **IllegalArgumentException**: Our validation failed
     * 2. **SecurityException**: Rare, but possible if security manager restricts stdout
     * 3. **Exception**: Catch-all for unexpected issues
     *
     * ### Why Not Let It Crash?
     * We catch all exceptions because:
     * - Infrastructure failures shouldn't crash the app
     * - We can retry with alternative outputs
     * - Better error messages help debugging
     *
     * ## Possible Errors
     * - Message validation failure (blank/empty)
     * - Security restrictions on System.out
     * - Out of memory (for very large messages)
     * - Console redirection issues
     *
     * ## Thread Safety
     * `println` is thread-safe in Kotlin/JVM, so this adapter is safe
     * for concurrent use.
     *
     * @param message The message to print to console
     * @return Right(Unit) on success, Left(ApplicationError) on failure
     */
    override suspend fun send(message: String): Either<ApplicationError, Unit> {
        return try {
            // Validate message before sending
            require(message.isNotBlank()) { "Message cannot be blank" }

            // Print to console - the actual I/O operation
            println(message)

            // Return success
            Unit.right()
        } catch (e: IllegalArgumentException) {
            // Handle validation errors from require()
            ApplicationError.OutputError(
                "Invalid message: ${e.message ?: "Unknown validation error"}",
            ).left()
        } catch (e: SecurityException) {
            // Handle security exceptions (e.g., System.out redirected or restricted)
            ApplicationError.OutputError(
                "Security error writing to console: ${e.message ?: "Permission denied"}",
            ).left()
        } catch (e: Exception) {
            // Catch all other exceptions to prevent crashes
            // Include exception type in message for debugging
            ApplicationError.OutputError(
                "Unexpected error writing to console: ${e.javaClass.simpleName} - ${e.message ?: "Unknown error"}",
            ).left()
        }
    }
}
