// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.presentation.cli

import arrow.core.raise.either
import com.abitofhelp.hybrid.application.dto.CreateGreetingCommand
import com.abitofhelp.hybrid.application.error.ApplicationError
import com.abitofhelp.hybrid.application.port.input.CreateGreetingInputPort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking

/**
 * Factory function the composition root calls to obtain a Runnable CLI program.
 * Presentation still depends only on application ports.
 *
 * @deprecated Use asyncCli instead for better async support
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
