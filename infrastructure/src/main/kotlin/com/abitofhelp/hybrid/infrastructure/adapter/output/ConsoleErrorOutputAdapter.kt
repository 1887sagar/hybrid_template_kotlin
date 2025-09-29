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
import com.abitofhelp.hybrid.application.port.output.ErrorOutputPort
import java.io.PrintStream

/**
 * Console error implementation of the ErrorOutputPort.
 *
 * ## What is an Error Adapter?
 * This adapter writes error messages to the console's error stream (stderr).
 * It follows the same patterns as ConsoleOutputAdapter but is dedicated
 * to error output.
 *
 * ## Why Use stderr?
 * - **Standard Practice**: Unix convention - stdout for data, stderr for errors
 * - **Stream Separation**: Allows piping/redirecting errors separately
 * - **Monitoring**: Many tools specifically watch stderr
 * - **Visibility**: Errors often displayed in red in terminals
 *
 * ## Design Decisions
 * - **PrintStream Injection**: Allows testing without touching System.err
 * - **No Blank Messages**: Enforces that errors are meaningful
 * - **Thread-Safe**: PrintStream.println is thread-safe
 * - **No Formatting**: Keep it simple - let higher layers format
 *
 * ## Example Usage
 * ```kotlin
 * // Production
 * val errorPort = ConsoleErrorOutputAdapter(System.err)
 *
 * // Testing
 * val errorStream = ByteArrayOutputStream()
 * val errorPort = ConsoleErrorOutputAdapter(PrintStream(errorStream))
 * ```
 *
 * ## Error Handling
 * Catches all exceptions to prevent crashes. Returns descriptive
 * error messages for debugging.
 */
class ConsoleErrorOutputAdapter(
    private val err: PrintStream = System.err,
) : ErrorOutputPort {

    /**
     * Sends an error message to the console error stream (stderr).
     *
     * ## Implementation Details
     * - Validates message is not blank
     * - Writes to the injected PrintStream
     * - Catches all exceptions to prevent crashes
     *
     * ## Thread Safety
     * PrintStream.println is synchronized, making this thread-safe.
     *
     * @param message The error message to print to stderr
     * @return Right(Unit) on success, Left(ApplicationError) on failure
     */
    override suspend fun sendError(message: String): Either<ApplicationError, Unit> {
        return try {
            // Validate message before sending
            require(message.isNotBlank()) { "Error message cannot be blank" }

            // Print to error stream - the actual I/O operation
            err.println(message)

            // Return success
            Unit.right()
        } catch (e: IllegalArgumentException) {
            // Handle validation errors from require()
            ApplicationError.OutputError(
                "Invalid error message: ${e.message ?: "Unknown validation error"}",
            ).left()
        } catch (e: SecurityException) {
            // Handle security exceptions (e.g., System.err redirected or restricted)
            ApplicationError.OutputError(
                "Security error writing to error console: ${e.message ?: "Permission denied"}",
            ).left()
        } catch (e: Exception) {
            // Catch all other exceptions to prevent crashes
            ApplicationError.OutputError(
                "Unexpected error writing to error console: ${e.javaClass.simpleName} - " +
                    "${e.message ?: "Unknown error"}",
            ).left()
        }
    }
}
