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
 * Output port for delivering messages to external systems.
 *
 * ## Output Ports Explained
 * An output port is an interface that your application uses to communicate with
 * the outside world. It's like defining "I need to send messages somewhere"
 * without caring whether it's to a console, file, database, or API.
 *
 * ## Dependency Inversion Principle
 * This interface lives in the application layer but is implemented in the
 * infrastructure layer. This inverts the dependency:
 * - Application layer: Defines what it needs (this interface)
 * - Infrastructure layer: Provides the implementation
 *
 * This means your business logic doesn't depend on infrastructure details!
 *
 * ## Common Implementations
 * ```kotlin
 * // Console output
 * class ConsoleOutputPort : OutputPort {
 *     override suspend fun send(message: String) =
 *         println(message).right()
 * }
 *
 * // File output
 * class FileOutputPort(private val file: File) : OutputPort {
 *     override suspend fun send(message: String) =
 *         try {
 *             file.appendText("$message\n")
 *             Unit.right()
 *         } catch (e: Exception) {
 *             ApplicationError.OutputError(e.message ?: "Write failed").left()
 *         }
 * }
 *
 * // API output
 * class ApiOutputPort(private val client: HttpClient) : OutputPort {
 *     override suspend fun send(message: String) =
 *         client.post("/messages", message).mapToEither()
 * }
 * ```
 *
 * The application doesn't know or care which one is used!
 */
interface OutputPort {
    /**
     * Sends a message to the configured output destination.
     *
     * ## Return Type Explanation
     * `Either<ApplicationError, Unit>` means:
     * - **Success (Right)**: Returns `Unit` (Kotlin's "void") - nothing to return, just "it worked"
     * - **Failure (Left)**: Returns an ApplicationError explaining what went wrong
     *
     * ## Common Errors
     * - **OutputError**: Failed to write (permissions, disk full, network down)
     * - **ValidationError**: Message was invalid (empty, too long)
     *
     * ## Usage Example
     * ```kotlin
     * class GreetingUseCase(private val output: OutputPort) {
     *     suspend fun greetUser(name: String) {
     *         val greeting = "Hello, $name!"
     *
     *         output.send(greeting).fold(
     *             { error ->
     *                 // Handle error - maybe try alternative output
     *                 logger.error("Failed to send: ${error.message}")
     *             },
     *             {
     *                 // Success! Message was sent
     *                 logger.debug("Greeting sent successfully")
     *             }
     *         )
     *     }
     * }
     * ```
     *
     * ## Implementation Guidelines
     * - Validate the message (not empty, within size limits)
     * - Handle errors gracefully (don't throw exceptions)
     * - Make it truly async - don't block threads
     * - Return specific error messages for debugging
     *
     * @param message The message to send to the output destination
     * @return Either an ApplicationError (Left) if sending failed, or Unit (Right) if successful
     */
    suspend fun send(message: String): Either<ApplicationError, Unit>
}
