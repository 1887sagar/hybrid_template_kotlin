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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking

/**
 * Factory function that creates a synchronous CLI program for legacy compatibility.
 *
 * ## What is a CLI Factory?
 * A factory function is a design pattern that encapsulates object creation.
 * This particular factory creates a CLI program wrapped in a Runnable interface,
 * allowing it to be executed by thread-based systems that expect the standard
 * Java Runnable contract.
 *
 * ## Why Use Factory Functions?
 * Factory functions provide several benefits:
 * - Encapsulate complex object creation logic
 * - Hide implementation details from callers
 * - Allow easy switching between implementations
 * - Provide a stable API even if internal structure changes
 *
 * ## Railway-Oriented Programming
 * This implementation uses Arrow's Either type for error handling:
 * ```kotlin
 * either {
 *     val result = riskyOperation().bind()  // Short-circuits on error
 *     processResult(result)
 * }.fold(
 *     { error -> handleError(error) },
 *     { success -> handleSuccess(success) }
 * )
 * ```
 *
 * The `.bind()` method automatically:
 * - Continues execution if the result is Right (success)
 * - Short-circuits and returns the error if the result is Left (failure)
 *
 * ## Error Mapping Strategy
 * The function maps technical application errors to user-friendly messages:
 * - OutputError: File/console writing failures
 * - DomainErrorWrapper: Business rule violations
 * - UseCaseError: Application processing failures
 * - ValidationError: Input format issues
 * - BatchValidationError: Problems with multiple items
 *
 * ## Blocking vs Non-Blocking
 * This function uses `runBlocking`, which blocks the current thread.
 * While not ideal for async applications, it provides compatibility
 * with systems that expect synchronous execution.
 *
 * ## Example Usage
 * ```kotlin
 * // In composition root
 * val config = PresentationConfig(
 *     verbose = true,
 *     outputPath = "/tmp/greetings.txt",
 *     name = "Alice"
 * )
 * 
 * val cliProgram = cli(config, greetingUseCase)
 * 
 * // Execute in a thread
 * val thread = Thread(cliProgram)
 * thread.start()
 * thread.join()
 * ```
 *
 * @param cfg The presentation configuration containing user preferences
 * @param createGreeting The use case port for creating greetings
 * @return A Runnable that executes the CLI program when run() is called
 * @deprecated Use asyncCli instead for better async support and non-blocking execution
 */
@Deprecated("Use asyncCli for better async support", ReplaceWith("asyncCli(cfg, createGreeting)"))
fun cli(cfg: PresentationConfig, createGreeting: CreateGreetingInputPort): Runnable =
    Runnable {
        try {
            runBlocking {
                either {
                    // Create command from config
                    val command = CreateGreetingCommand(
                        name = cfg.name,
                        silent = false,
                    )

                    // Show tip if no name provided
                    if (cfg.name == null) {
                        println("Tip: You can provide your name as a command-line argument")
                    }

                    // Execute use case
                    createGreeting.execute(command).bind()

                    // Log if verbose
                    if (cfg.verbose) {
                        println("[INFO] Greeting delivered successfully")
                    }
                }.fold(
                    { error ->
                        // Provide user-friendly error messages
                        val errorMessage = when (error) {
                            is ApplicationError.OutputError ->
                                "Failed to deliver greeting: ${error.message}"
                            is ApplicationError.DomainErrorWrapper ->
                                "Invalid input: ${error.userMessage}"
                            is ApplicationError.UseCaseError ->
                                "Processing error in ${error.useCase}: ${error.cause}"
                            is ApplicationError.ValidationError ->
                                "Validation error in ${error.field}: ${error.message}"
                            is ApplicationError.BatchValidationError ->
                                "Batch validation failed for '${error.item}': ${error.error}"
                        }
                        System.err.println("Error: $errorMessage")
                    },
                    { /* Success - output already handled */ },
                )
            }
        } catch (e: CancellationException) {
            // Coroutine was cancelled - let it propagate
            throw e
        } catch (e: Exception) {
            // Catch any unexpected errors to prevent crash
            System.err.println("Unexpected error: ${e.message ?: e.javaClass.simpleName}")
        }
    }
