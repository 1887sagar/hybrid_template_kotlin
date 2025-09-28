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
import kotlinx.coroutines.*

/**
 * Pure async CLI implementation without any blocking code.
 * This is the modern, preferred way to implement CLI programs in Kotlin.
 */
class PureAsyncCli(
    private val config: PresentationConfig,
    private val createGreeting: CreateGreetingInputPort,
    private val scope: CoroutineScope = CoroutineScope(
        Dispatchers.Default +
            SupervisorJob() +
            CoroutineName("PureAsyncCli"),
    ),
) {
    /**
     * Executes the CLI program asynchronously.
     * Returns the exit code.
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
                    println("Tip: You can provide your name as a command-line argument")
                }
            }

            // Execute use case
            createGreeting.execute(command).bind()

            // Log if verbose
            if (config.verbose) {
                println("[INFO] Greeting delivered successfully")
            }
        }.fold(
            { error -> handleError(error) },
            { 0 }, // Success
        )
    }

    private fun handleError(error: ApplicationError): Int {
        val (message, exitCode) = when (error) {
            is ApplicationError.OutputError ->
                "Failed to deliver greeting: ${error.message}" to 1

            is ApplicationError.DomainErrorWrapper ->
                "Invalid input: ${error.userMessage}" to 2

            is ApplicationError.UseCaseError ->
                "Processing error in ${error.useCase}: ${error.cause}" to 3

            is ApplicationError.ValidationError ->
                "Validation error in ${error.field}: ${error.message}" to 4

            is ApplicationError.BatchValidationError ->
                "Batch validation failed for '${error.item}': ${error.error}" to 5
        }

        System.err.println("Error: $message")
        return exitCode
    }
}

/**
 * Factory function to create and execute a pure async CLI.
 */
suspend fun createAndRunCli(
    config: PresentationConfig,
    createGreeting: CreateGreetingInputPort,
): Int {
    val cli = PureAsyncCli(config, createGreeting)
    return cli.execute()
}
