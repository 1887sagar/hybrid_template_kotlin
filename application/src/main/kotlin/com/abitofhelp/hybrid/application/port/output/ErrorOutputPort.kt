// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.application.port.output

import arrow.core.Either
import com.abitofhelp.hybrid.application.error.ApplicationError

/**
 * Port for sending error messages to external destinations.
 *
 * ## What is an Error Output Port?
 * In hexagonal architecture, this port defines how the application
 * sends error messages to the outside world. It's separate from the
 * regular OutputPort to follow the Single Responsibility Principle.
 *
 * ## Why Separate Error Output?
 * - **Different Destinations**: Errors often go to different places (stderr, log files, monitoring)
 * - **Different Formatting**: Error messages may need special formatting or metadata
 * - **Different Handling**: Errors might trigger alerts or special processing
 * - **Testing**: Easier to assert on errors separately from regular output
 *
 * ## Real-World Use Cases
 * ```kotlin
 * // Critical system errors might go to multiple places
 * class CriticalErrorHandler(
 *     private val errorPort: ErrorOutputPort
 * ) {
 *     suspend fun handleCriticalError(error: ApplicationError) {
 *         errorPort.sendError("CRITICAL: ${error.message}")
 *         // Implementation might also:
 *         // - Send to monitoring system (PagerDuty, DataDog)
 *         // - Write to error log file
 *         // - Send email to developers
 *         // - Update system status page
 *     }
 * }
 *
 * // User-facing errors might be formatted differently
 * class UserErrorHandler(
 *     private val errorPort: ErrorOutputPort
 * ) {
 *     suspend fun handleUserError(error: ApplicationError) {
 *         val userMessage = when (error) {
 *             is ApplicationError.ValidationError ->
 *                 "Please check your input: ${error.message}"
 *             is ApplicationError.OutputError ->
 *                 "Unable to save your changes. Please try again."
 *             else -> "Something went wrong. Please try again later."
 *         }
 *         errorPort.sendError(userMessage)
 *     }
 * }
 * ```
 *
 * ## Contract Requirements
 * Implementations must:
 * - Accept any non-null error message
 * - Handle their own errors gracefully (never throw)
 * - Return Either.Left only for infrastructure failures
 * - Be thread-safe if used concurrently
 *
 * ## Example Implementations
 * - **ConsoleErrorOutputAdapter**: Writes to stderr
 * - **FileErrorOutputAdapter**: Writes to error log files
 * - **MonitoringErrorOutputAdapter**: Sends to monitoring systems
 * - **CompositeErrorOutputAdapter**: Routes to multiple destinations
 *
 * ## Testing Example
 * ```kotlin
 * class TestErrorOutputAdapter : ErrorOutputPort {
 *     val errors = mutableListOf<String>()
 *
 *     override suspend fun sendError(message: String): Either<ApplicationError, Unit> {
 *         errors.add(message)
 *         return Unit.right()
 *     }
 * }
 *
 * // In your tests
 * @Test
 * fun `should log validation errors`() {
 *     val errorPort = TestErrorOutputAdapter()
 *     val handler = UserErrorHandler(errorPort)
 *
 *     runBlocking {
 *         handler.handleUserError(
 *             ApplicationError.ValidationError("email", "Invalid format")
 *         )
 *     }
 *
 *     assertThat(errorPort.errors).contains("Please check your input: Invalid format")
 * }
 * ```
 */
interface ErrorOutputPort {
    /**
     * Sends an error message to the configured destination.
     *
     * ## What Should This Method Do?
     * This method is responsible for delivering error messages to wherever they need to go.
     * The implementation might write to stderr, append to a log file, send to a monitoring
     * service, or any combination of these.
     *
     * ## Error Handling Philosophy
     * This method should never throw exceptions. If the error message can't be delivered
     * (e.g., disk full, network down), it should return an ApplicationError describing
     * what went wrong. The calling code can then decide how to handle this meta-error.
     *
     * ## Implementation Guidelines
     * ```kotlin
     * class FileErrorOutputAdapter(private val errorFile: File) : ErrorOutputPort {
     *     override suspend fun sendError(message: String): Either<ApplicationError, Unit> {
     *         return try {
     *             errorFile.appendText("${Instant.now()}: $message\n")
     *             Unit.right()
     *         } catch (e: IOException) {
     *             ApplicationError.OutputError(
     *                 "Failed to write error to file: ${e.message}"
     *             ).left()
     *         }
     *     }
     * }
     * ```
     *
     * @param message The error message to send. Should be human-readable and contain
     *                enough context to understand what went wrong.
     * @return Either.Right(Unit) if the message was successfully sent,
     *         Either.Left(ApplicationError) if sending failed
     */
    suspend fun sendError(message: String): Either<ApplicationError, Unit>
}
