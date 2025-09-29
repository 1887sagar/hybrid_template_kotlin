////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.presentation.cli

import arrow.core.raise.either
import com.abitofhelp.hybrid.application.dto.CreateGreetingCommand
import com.abitofhelp.hybrid.application.error.ApplicationError
import com.abitofhelp.hybrid.application.port.input.CreateGreetingInputPort
import com.abitofhelp.hybrid.application.port.output.ErrorOutputPort
import com.abitofhelp.hybrid.application.port.output.OutputPort
import kotlinx.coroutines.*

// Exit codes for different error types
private const val EXIT_CODE_SUCCESS = 0
private const val EXIT_CODE_OUTPUT_ERROR = 1
private const val EXIT_CODE_DOMAIN_ERROR = 2
private const val EXIT_CODE_USECASE_ERROR = 3
private const val EXIT_CODE_VALIDATION_ERROR = 4
private const val EXIT_CODE_BATCH_VALIDATION_ERROR = 5

/**
 * Pure async CLI implementation that follows modern Kotlin coroutine best practices.
 *
 * ## What is Pure Async Programming?
 * "Pure async" means the code is written entirely with suspend functions and coroutines,
 * without any blocking operations like `runBlocking` or `Thread.sleep()`. This approach:
 * - Maximizes concurrency and resource efficiency
 * - Prevents thread pool exhaustion
 * - Scales better under load
 * - Integrates seamlessly with async frameworks
 *
 * ## Why Choose This Over Blocking Code?
 * Traditional blocking CLI programs tie up threads while waiting for I/O:
 * ```kotlin
 * // Blocking approach - wastes thread
 * fun blockingCli() {
 *     println("Starting...")        // Blocks thread for console I/O
 *     writeFile("data.txt")         // Blocks thread for file I/O
 *     println("Done!")              // Blocks thread again
 * }
 * ```
 *
 * Pure async approach frees threads during I/O:
 * ```kotlin
 * // Async approach - efficient thread usage
 * suspend fun asyncCli() {
 *     outputPort.send("Starting...")    // Suspends, doesn't block
 *     writeFileAsync("data.txt")        // Suspends, doesn't block  
 *     outputPort.send("Done!")          // Suspends, doesn't block
 * }
 * ```
 *
 * ## Hexagonal Architecture Compliance
 * This implementation strictly follows the ports and adapters pattern:
 * - **OutputPort**: All regular output (console, files, network)
 * - **ErrorOutputPort**: All error output (stderr, log files, alerts)
 * - **CreateGreetingInputPort**: Business logic entry point
 * - **No direct I/O**: Zero System.out, System.err, or File operations
 *
 * Benefits of this approach:
 * - **Testability**: Mock all I/O without complex test setup
 * - **Flexibility**: Switch output destinations without code changes
 * - **Isolation**: Presentation logic is independent of infrastructure
 * - **Composability**: Easy to combine with other async operations
 *
 * ## Structured Concurrency
 * Uses `coroutineScope` to ensure proper coroutine lifecycle management:
 * - All child coroutines complete before function returns
 * - If any coroutine fails, all others are cancelled
 * - Resources are automatically cleaned up
 * - No coroutine leaks possible
 *
 * ## Error Handling Strategy
 * Each error type maps to a specific exit code for shell script integration:
 * - 0: Success
 * - 1: Output/I/O error
 * - 2: Domain/business rule violation
 * - 3: Use case processing error
 * - 4: Input validation error
 * - 5: Batch validation error
 *
 * ## Example Usage
 * ```kotlin
 * // In a suspend context (like main or test)
 * suspend fun main() {
 *     val config = PresentationConfig(
 *         verbose = true,
 *         outputPath = null,  // Console only
 *         name = "Alice"
 *     )
 *     
 *     val cli = PureAsyncCli(
 *         config = config,
 *         createGreeting = greetingUseCase,
 *         outputPort = consoleOutputAdapter,
 *         errorOutputPort = stderrOutputAdapter
 *     )
 *     
 *     val exitCode = cli.execute()
 *     exitProcess(exitCode)
 * }
 * 
 * // For testing
 * @Test
 * fun `should handle user input correctly`() = runTest {
 *     val mockOutput = MockOutputPort()
 *     val mockErrorOutput = MockErrorOutputPort()
 *     
 *     val cli = PureAsyncCli(
 *         config = testConfig,
 *         createGreeting = mockUseCase,
 *         outputPort = mockOutput,
 *         errorOutputPort = mockErrorOutput
 *     )
 *     
 *     val exitCode = cli.execute()
 *     
 *     assertEquals(0, exitCode)
 *     assertTrue(mockOutput.messages.isNotEmpty())
 * }
 * ```
 *
 * @param config The presentation configuration containing user preferences and settings
 * @param createGreeting The application service for processing greeting requests
 * @param outputPort The port for sending regular output messages
 * @param errorOutputPort The port for sending error messages and diagnostics
 */
class PureAsyncCli(
    private val config: PresentationConfig,
    private val createGreeting: CreateGreetingInputPort,
    private val outputPort: OutputPort,
    private val errorOutputPort: ErrorOutputPort,
) {
    /**
     * Executes the CLI program asynchronously and returns an exit code.
     *
     * ## What This Function Does
     * This is the main entry point for the CLI program execution. It:
     * 1. Transforms presentation config into application commands
     * 2. Optionally shows user tips for better experience
     * 3. Executes the core business logic via use cases
     * 4. Handles all possible error scenarios gracefully
     * 5. Returns appropriate exit codes for shell integration
     *
     * ## Why Suspend?
     * This function is marked `suspend` because it may perform I/O operations
     * that should not block threads. The `coroutineScope` ensures that:
     * - All child coroutines complete before returning
     * - If this coroutine is cancelled, all children are cancelled
     * - Resources are properly cleaned up
     *
     * ## Error vs Exception Handling
     * This function uses Arrow's Either type for expected errors (business logic failures)
     * but relies on Kotlin's exception system for unexpected errors (programming bugs).
     * 
     * Expected errors (handled via Either):
     * - Invalid user input
     * - File permission issues
     * - Network connectivity problems
     *
     * Unexpected errors (propagated as exceptions):
     * - Programming bugs
     * - System resource exhaustion
     * - Corrupted data structures
     *
     * ## Example Call Flow
     * ```kotlin
     * // User runs: ./app --verbose Alice
     * val config = PresentationConfig(
     *     verbose = true,
     *     outputPath = null,
     *     name = "Alice"
     * )
     * 
     * val exitCode = cli.execute()  // This function
     * // -> Creates CreateGreetingCommand(name="Alice", silent=false)
     * // -> Calls createGreeting.execute(command)
     * // -> Outputs "Hello, Alice!" via outputPort
     * // -> Logs "[INFO] Greeting delivered successfully"
     * // -> Returns 0 (success)
     * ```
     *
     * @return Exit code following Unix conventions (0 = success, non-zero = error)
     */
    suspend fun execute(): Int = coroutineScope {
        either {
            // Create command from config
            val command = CreateGreetingCommand(
                name = config.name,
                silent = false,
            )

            // Show tip if no name provided (async)
            if (config.name == null) {
                launch(Dispatchers.IO) {
                    outputPort.send("Tip: You can provide your name as a command-line argument")
                }
            }

            // Execute use case
            createGreeting.execute(command).bind()

            // Log if verbose
            if (config.verbose) {
                outputPort.send("[INFO] Greeting delivered successfully")
            }
        }.fold(
            { error -> handleError(error) },
            { EXIT_CODE_SUCCESS }, // Success
        )
    }

    /**
     * Maps application errors to user-friendly messages and appropriate exit codes.
     *
     * ## Why Separate Error Handling?
     * Centralizing error handling in a dedicated function provides several benefits:
     * - **Consistency**: All errors follow the same format and style
     * - **Maintainability**: Easy to update error messages in one place
     * - **Testability**: Can test error scenarios independently
     * - **Separation of Concerns**: Main execution logic isn't cluttered with error details
     *
     * ## Exit Code Strategy
     * Different error types return different exit codes, allowing shell scripts
     * to react appropriately:
     * ```bash
     * ./app --name ""  # Returns 4 (validation error)
     * if [ $? -eq 4 ]; then
     *     echo "Please provide a valid name"
     *     exit 1
     * fi
     * ```
     *
     * ## Error Message Design
     * Error messages follow a pattern:
     * - Start with the error category ("Failed to...", "Invalid...", etc.)
     * - Include the specific problem from the error object
     * - Use language that non-technical users can understand
     * - Avoid exposing internal implementation details
     *
     * ## Output Port vs Direct Logging
     * Instead of writing directly to System.err, this function uses ErrorOutputPort:
     * - **Testable**: Can capture and verify error messages in tests
     * - **Flexible**: Errors can go to files, remote logging systems, etc.
     * - **Architecture Compliant**: Follows hexagonal architecture principles
     * - **Composable**: Multiple error outputs can be combined
     *
     * @param error The application error to be handled and reported
     * @return The appropriate exit code for the error type
     */
    private suspend fun handleError(error: ApplicationError): Int {
        val (message, exitCode) = when (error) {
            is ApplicationError.OutputError ->
                "Failed to deliver greeting: ${error.message}" to EXIT_CODE_OUTPUT_ERROR

            is ApplicationError.DomainErrorWrapper ->
                "Invalid input: ${error.userMessage}" to EXIT_CODE_DOMAIN_ERROR

            is ApplicationError.UseCaseError ->
                "Processing error in ${error.useCase}: ${error.cause}" to EXIT_CODE_USECASE_ERROR

            is ApplicationError.ValidationError ->
                "Validation error in ${error.field}: ${error.message}" to EXIT_CODE_VALIDATION_ERROR

            is ApplicationError.BatchValidationError ->
                "Batch validation failed for '${error.item}': ${error.error}" to EXIT_CODE_BATCH_VALIDATION_ERROR
        }

        // Send error through the error port (ignore failure - we're already handling an error)
        errorOutputPort.sendError("Error: $message")
        return exitCode
    }
}

/**
 * Factory function that creates and immediately executes a pure async CLI program.
 *
 * ## What is a Factory Function?
 * A factory function is a simple function (not a class constructor) that creates
 * and returns objects. This particular factory:
 * - Creates a PureAsyncCli instance
 * - Immediately executes it
 * - Returns the result
 *
 * This pattern is useful when you want a "create and run" operation in one step.
 *
 * ## Why Use This Instead of Constructor + Execute?
 * This factory function provides convenience and ensures proper usage:
 * ```kotlin
 * // Without factory - multiple steps, easy to forget execute()
 * val cli = PureAsyncCli(config, useCase, output, errorOutput)
 * val exitCode = cli.execute()  // Easy to forget this step
 * 
 * // With factory - one step, impossible to forget
 * val exitCode = createAndRunCli(config, useCase, output, errorOutput)
 * ```
 *
 * ## When to Use This Function
 * Use this factory function when:
 * - You want immediate execution (most CLI scenarios)
 * - You don't need to reuse the CLI instance
 * - You want simplified calling code
 * - You're in a suspend context already
 *
 * Use the constructor directly when:
 * - You need to configure the CLI before running
 * - You want to run the same CLI multiple times
 * - You need access to the CLI instance for testing
 *
 * ## Example Usage
 * ```kotlin
 * suspend fun main() {
 *     val config = parseCommandLineArgs(args)
 *     val dependencies = setupDependencies()
 *     
 *     val exitCode = createAndRunCli(
 *         config = config,
 *         createGreeting = dependencies.greetingUseCase,
 *         outputPort = dependencies.consoleOutput,
 *         errorOutputPort = dependencies.errorOutput
 *     )
 *     
 *     exitProcess(exitCode)
 * }
 * ```
 *
 * @param config The presentation configuration containing user preferences
 * @param createGreeting The application service for processing greeting requests
 * @param outputPort The port for sending regular output messages
 * @param errorOutputPort The port for sending error messages and diagnostics
 * @return Exit code following Unix conventions (0 = success, non-zero = error)
 */
suspend fun createAndRunCli(
    config: PresentationConfig,
    createGreeting: CreateGreetingInputPort,
    outputPort: OutputPort,
    errorOutputPort: ErrorOutputPort,
): Int {
    val cli = PureAsyncCli(config, createGreeting, outputPort, errorOutputPort)
    return cli.execute()
}
