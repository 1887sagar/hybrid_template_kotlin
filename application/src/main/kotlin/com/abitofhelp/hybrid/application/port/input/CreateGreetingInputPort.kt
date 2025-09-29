////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.application.port.input

import arrow.core.Either
import com.abitofhelp.hybrid.application.dto.CreateGreetingCommand
import com.abitofhelp.hybrid.application.dto.GreetingResult
import com.abitofhelp.hybrid.application.error.ApplicationError

/**
 * Input port for the create greeting use case.
 *
 * ## What is a Port?
 * In hexagonal architecture (ports and adapters), a port is an interface that defines
 * how different layers communicate. Think of it like a USB port - it defines the shape
 * of the connection, but not what's plugged into it.
 *
 * ## Input Port vs Output Port
 * - **Input Port**: How the outside world talks TO your application (like this interface)
 * - **Output Port**: How your application talks TO the outside world (like database interfaces)
 *
 * ## Why Use Input Ports?
 * - **Decoupling**: The presentation layer doesn't know HOW greetings are created
 * - **Testability**: Easy to mock this interface in tests
 * - **Flexibility**: Can change the implementation without affecting the presentation layer
 *
 * ## Example Usage
 * ```kotlin
 * // In your presentation layer (e.g., CLI, REST controller)
 * class GreetingController(
 *     private val createGreeting: CreateGreetingInputPort
 * ) {
 *     suspend fun handleRequest(name: String) {
 *         val command = CreateGreetingCommand(name)
 *
 *         createGreeting.execute(command).fold(
 *             { error -> println("Error: ${error.message}") },
 *             { result -> println(result.greeting) }
 *         )
 *     }
 * }
 * ```
 *
 * This interface is the "front door" to your application's greeting functionality.
 */
interface CreateGreetingInputPort {
    /**
     * Executes the create greeting use case.
     *
     * ## What is a Command?
     * A command is a simple data object that carries the input needed for an operation.
     * It's like filling out a form - the command holds all the form data.
     *
     * ## The Flow
     * 1. Presentation layer creates a `CreateGreetingCommand` with user input
     * 2. Calls this `execute` method
     * 3. Application layer validates and processes the command
     * 4. Returns either an error or a successful result
     *
     * ## Error Handling
     * The method returns `Either<ApplicationError, GreetingResult>`:
     * - **Left** (ApplicationError): Something went wrong
     *   - ValidationError: Invalid input data
     *   - ProcessingError: Business logic failed
     *   - DependencyError: External service failed
     * - **Right** (GreetingResult): Success! Contains the greeting
     *
     * ## Example
     * ```kotlin
     * val command = CreateGreetingCommand(name = "Alice")
     *
     * when (val result = createGreeting.execute(command)) {
     *     is Either.Left -> {
     *         val error = result.value
     *         logger.error("Failed to create greeting: ${error.message}")
     *     }
     *     is Either.Right -> {
     *         val greeting = result.value
     *         println(greeting.greeting) // "Hey there, Alice! Welcome!"
     *     }
     * }
     * ```
     *
     * @param command The command containing the name to create a greeting for
     * @return Either an ApplicationError (Left) or the GreetingResult (Right)
     */
    suspend fun execute(command: CreateGreetingCommand): Either<ApplicationError, GreetingResult>
}
