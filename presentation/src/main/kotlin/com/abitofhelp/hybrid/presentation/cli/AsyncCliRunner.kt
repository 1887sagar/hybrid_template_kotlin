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
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * Async CLI runner that properly manages coroutine lifecycle.
 *
 * ## Coroutines and CoroutineScope
 * This class implements `CoroutineScope`, which means it:
 * - Can launch coroutines using `launch` and `async`
 * - Manages the lifecycle of all its coroutines
 * - Cancels all child coroutines when shut down
 *
 * ## Key Components
 *
 * ### SupervisorJob
 * ```kotlin
 * private val job = SupervisorJob()
 * ```
 * Unlike regular Job, SupervisorJob doesn't cancel other children when one fails.
 * This means if one operation fails, others can continue.
 *
 * ### CoroutineExceptionHandler
 * Catches unhandled exceptions in coroutines. Without this, exceptions would:
 * - Crash the application
 * - Be silently ignored
 * - Be hard to debug
 *
 * ### Dispatcher
 * Controls which threads coroutines run on:
 * - `Dispatchers.Default`: CPU-intensive work
 * - `Dispatchers.IO`: I/O operations
 * - `Dispatchers.Main`: UI updates (not used in CLI)
 *
 * ## Structured Concurrency
 * All coroutines launched in this scope are children of the main job.
 * When we call `shutdown()`, all children are cancelled automatically.
 *
 * ## Example Usage
 * ```kotlin
 * val runner = AsyncCliRunner(greetingUseCase)
 *
 * // Launch async operation
 * val exitCode = runner.runAsync(config).await()
 *
 * // Always clean up
 * runner.shutdown()
 * ```
 *
 * @param createGreeting The use case for creating greetings
 * @param dispatcher The coroutine dispatcher to use (default: CPU-bound work)
 */
class AsyncCliRunner(
    private val createGreeting: CreateGreetingInputPort,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : CoroutineScope {

    /**
     * SupervisorJob ensures that if one coroutine fails, others continue.
     * This is important for resilience - one failed operation shouldn't
     * cancel everything else.
     */
    private val job = SupervisorJob()

    /**
     * Catches exceptions that escape from coroutines.
     * The lambda receives the coroutine context and the exception.
     * We ignore the context here as we handle all exceptions the same way.
     */
    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        handleCoroutineException(exception)
    }

    /**
     * Combines all coroutine context elements:
     * - Dispatcher: Where to run (thread pool)
     * - Job: Lifecycle management
     * - Exception Handler: Error handling
     *
     * The + operator combines contexts - Kotlin's way of composing behaviors.
     */
    override val coroutineContext: CoroutineContext = dispatcher + job + errorHandler

    /**
     * Handles exceptions that escape from coroutines.
     *
     * ## Exception Types
     *
     * ### CancellationException
     * - Normal part of coroutine lifecycle
     * - Thrown when coroutines are cancelled
     * - Must be rethrown to maintain cancellation
     *
     * ### OutOfMemoryError
     * - Critical system error
     * - Minimal logging to avoid more allocation
     * - System is likely unstable at this point
     *
     * ### SecurityException
     * - Security manager blocked an operation
     * - Important for audit trails
     *
     * ### Everything Else
     * - Unexpected errors that escaped normal handling
     * - Full stack trace for debugging
     *
     * ## Why System.err?
     * Using System.err ensures error messages:
     * - Appear even if stdout is redirected
     * - Can be captured separately in logs
     * - Are immediately flushed (unbuffered)
     *
     * @param exception The unhandled exception from a coroutine
     */
    private fun handleCoroutineException(exception: Throwable) {
        when (exception) {
            is CancellationException -> {
                // Normal cancellation, don't log
                // MUST rethrow to preserve cancellation semantics
                throw exception
            }
            is OutOfMemoryError -> {
                System.err.println("CRITICAL: Out of memory in coroutine")
                // Don't try to allocate more memory for detailed logging
            }
            is SecurityException -> {
                System.err.println("SECURITY: Coroutine security violation: ${exception.message}")
            }
            else -> {
                System.err.println("ERROR: Unhandled coroutine exception: ${exception.javaClass.simpleName}")
                System.err.println("Message: ${exception.message}")
                exception.printStackTrace()
            }
        }
    }

    /**
     * Runs the CLI program asynchronously.
     *
     * ## Deferred<T>
     * `Deferred` is like a `Future` or `Promise` in other languages:
     * - Represents a value that will be available later
     * - Can be awaited with `.await()`
     * - Can be cancelled
     *
     * ## The async Builder
     * `async { ... }` launches a coroutine that:
     * - Runs concurrently
     * - Returns a value
     * - Can be awaited
     *
     * Compare with `launch { ... }` which returns Job (no value).
     *
     * ## Exit Codes
     * Following Unix convention:
     * - 0 = Success
     * - 1 = General error
     * - Other codes could indicate specific errors
     *
     * @param config Configuration for the presentation layer
     * @return Deferred<Int> that will contain the exit code
     */
    fun runAsync(config: PresentationConfig): Deferred<Int> = async {
        // Arrow's 'either' builder for railway-oriented programming
        either {
            // Step 1: Transform presentation config to application command
            val command = CreateGreetingCommand(
                name = config.name,
                silent = false, // CLI always outputs
            )

            // Step 2: Show helpful tip for new users (non-blocking)
            if (config.name == null) {
                // Launch on IO dispatcher for console output
                launch(Dispatchers.IO) {
                    println("Tip: You can provide your name as a command-line argument")
                }.join() // Wait for tip to display before greeting
            }

            // Step 3: Execute the use case
            // The .bind() automatically unwraps Either:
            // - If Left (error): Short-circuits and returns the error
            // - If Right (success): Continues with the value
            createGreeting.execute(command).bind()

            // Step 4: Verbose logging for debugging
            if (config.verbose) {
                println("[INFO] Greeting delivered successfully")
            }
        }.fold(
            { error ->
                // Transform technical errors to user-friendly messages
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
                1 // Error exit code
            },
            {
                // Success case - greeting was created and delivered
                0 // Success exit code
            },
        )
    }

    /**
     * Runs the CLI program synchronously (for testing only).
     *
     * ## Why Deprecated?
     * `runBlocking` blocks the current thread, which:
     * - Defeats the purpose of async programming
     * - Can cause deadlocks in some scenarios
     * - Is only appropriate for tests and main functions
     *
     * Use `runAsync()` and properly handle the Deferred result instead.
     *
     * @deprecated Use runAsync for proper async support
     * @param config The presentation configuration
     * @return Exit code (0 for success, non-zero for error)
     */
    @Deprecated("Use runAsync for proper async support")
    fun runBlocking(config: PresentationConfig): Int = runBlocking {
        runAsync(config).await()
    }

    /**
     * Cancels all running coroutines and cleans up resources.
     *
     * ## Structured Concurrency
     * When we cancel the parent job:
     * - All child coroutines are cancelled
     * - Running operations are interrupted
     * - Resources are cleaned up
     *
     * This is essential for:
     * - Graceful shutdown
     * - Test cleanup
     * - Preventing resource leaks
     *
     * ## Cancellation is Cooperative
     * Coroutines check for cancellation at suspension points.
     * Long-running computations should check `isActive` periodically.
     */
    fun shutdown() {
        job.cancel()
    }
}

/**
 * Creates a suspend function for the CLI program.
 *
 * ## When to Use This
 * This function is perfect for:
 * - Integration with other suspend functions
 * - Testing with `runTest`
 * - Composing with other async operations
 *
 * ## Resource Management
 * Uses try-finally to ensure cleanup even if:
 * - The operation fails
 * - The coroutine is cancelled
 * - An exception is thrown
 *
 * ## Example
 * ```kotlin
 * // In a suspend context
 * suspend fun main() {
 *     val config = PresentationConfig(name = "Alice")
 *     val exitCode = suspendCli(config, greetingUseCase)
 *     exitProcess(exitCode)
 * }
 * ```
 *
 * @param cfg Configuration for the CLI
 * @param createGreeting The greeting use case
 * @return Exit code (0 for success)
 */
suspend fun suspendCli(
    cfg: PresentationConfig,
    createGreeting: CreateGreetingInputPort,
): Int {
    val runner = AsyncCliRunner(createGreeting)
    return try {
        // Launch async operation and wait for result
        runner.runAsync(cfg).await()
    } finally {
        // ALWAYS clean up, even if cancelled or failed
        runner.shutdown()
    }
}

/**
 * Factory function for backward compatibility with Runnable interface.
 *
 * ## Why This Exists
 * Some frameworks expect a `Runnable` interface.
 * This adapter bridges between:
 * - Old style: Thread-based Runnable
 * - New style: Coroutine-based async
 *
 * ## The Dispatchers.IO Choice
 * Using `Dispatchers.IO` in runBlocking:
 * - Prevents blocking the default dispatcher
 * - Better for I/O-bound operations
 * - Reduces risk of thread starvation
 *
 * ## Important Note
 * This still blocks a thread! It's a compatibility shim,
 * not the recommended approach. Prefer `suspendCli` when possible.
 *
 * @param cfg Configuration for the CLI
 * @param createGreeting The greeting use case
 * @return Runnable that can be executed by thread-based systems
 */
fun asyncCli(cfg: PresentationConfig, createGreeting: CreateGreetingInputPort): Runnable =
    Runnable {
        val runner = AsyncCliRunner(createGreeting)
        // Use IO dispatcher to avoid blocking default thread pool
        runBlocking(Dispatchers.IO) {
            try {
                runner.runAsync(cfg).await()
            } finally {
                runner.shutdown()
            }
        }
        // Exit code is handled by the bootstrap layer
        // Runnable doesn't return a value, so we don't use the result
    }
