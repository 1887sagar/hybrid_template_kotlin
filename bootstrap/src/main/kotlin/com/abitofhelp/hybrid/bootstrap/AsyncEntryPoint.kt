// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.bootstrap

import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

private const val CLEANUP_JOIN_TIMEOUT_MILLIS = 2000L
private const val SHUTDOWN_GRACE_PERIOD_MILLIS = 5000L
private const val SHUTDOWN_LATCH_COUNT = 1
// ---------------------------------------------------------------------------

/**
 * Async entry point with proper error handling and signal management.
 *
 * ## What is an Object?
 * In Kotlin, `object` creates a singleton - exactly one instance exists.
 * Perfect for application entry points and global state management:
 * ```kotlin
 * // Only one instance of AsyncApp exists in the entire JVM
 * val app1 = AsyncApp
 * val app2 = AsyncApp
 * println(app1 === app2) // true - same instance
 * ```
 *
 * ## Why Use AsyncApp?
 * Modern applications benefit from async/await patterns because:
 * - Better resource utilization (threads aren't blocked waiting)
 * - Improved scalability (handle more concurrent operations)
 * - Cleaner error handling with structured concurrency
 * - Proper lifecycle management for long-running operations
 *
 * ## Signal Handling
 * This app handles Unix signals for graceful shutdown through the SystemSignals
 * abstraction, avoiding direct dependencies on internal JVM APIs:
 * - **SIGTERM (15)**: Sent by system during shutdown (`kill <pid>`)
 * - **SIGINT (2)**: Sent when user presses Ctrl+C (`^C`)
 * - **SIGHUP (1)**: Terminal disconnected (Unix only)
 * - **SIGKILL (9)**: Cannot be caught! Forceful termination
 *
 * ## Graceful Shutdown Process
 * 1. Signal received → Signal handler triggered
 * 2. Initiates shutdown with 5-second grace period
 * 3. Allows running operations to complete naturally
 * 4. Forces shutdown if grace period expires
 * 5. Cleans up resources and exits with appropriate code
 *
 * ## Exit Codes
 * Uses centralized ExitCode enum following Unix conventions.
 * See ExitCode documentation for complete list.
 *
 * ## Coroutine Architecture
 * - **SupervisorJob**: Isolates failures (one coroutine failing doesn't kill others)
 * - **CoroutineExceptionHandler**: Catches unhandled exceptions as last resort
 * - **Structured Concurrency**: Ensures all coroutines are properly cleaned up
 * - **Dispatchers.Default**: Uses shared thread pool optimized for CPU-bound work
 *
 * ## Example Usage
 * ```kotlin
 * // Command line execution
 * suspend fun main(args: Array<String>) {
 *     val exitCode = AsyncApp.runAsync(args)
 *     exitProcess(exitCode)
 * }
 *
 * // Or in tests with no-op signals
 * @Test
 * fun testAsyncApp() = runTest {
 *     val result = AsyncApp.runAsync(arrayOf("--verbose", "TestUser"), NoOpSignals())
 *     assertEquals(0, result)
 * }
 * ```
 */
object AsyncApp {

    /**
     * CountDownLatch for coordinating shutdown across threads.
     * Starts at 1, counts down to 0 when shutdown is initiated.
     * Allows threads to wait for shutdown signal.
     */
    private val shutdownLatch = CountDownLatch(SHUTDOWN_LATCH_COUNT)

    /**
     * Atomic flag to ensure shutdown only happens once.
     * Multiple signals or errors might try to shutdown simultaneously.
     */
    private val isShuttingDown = AtomicBoolean(false)

    /**
     * Stores the final exit code to return to the OS.
     * Atomic to handle concurrent updates from different error paths.
     */
    private val exitCode = AtomicReference(ExitCode.SUCCESS)

    /**
     * Reference to the cleanup timeout job.
     * Allows cancellation if normal shutdown completes quickly.
     */
    private var cleanupJob: Job? = null

    /**
     * Main coroutine scope with supervisor job for fault isolation.
     *
     * ## Scope Components
     * - **Dispatchers.Default**: Uses shared thread pool for CPU-bound work
     * - **SupervisorJob()**: Children fail independently (fault isolation)
     * - **CoroutineExceptionHandler**: Last resort for uncaught exceptions
     *
     * This scope lives for the entire application lifetime.
     * Initialized with a no-op logger, but gets properly replaced in runAsync.
     */
    private var appScope = CoroutineScope(
        Dispatchers.Default +
            SupervisorJob() +
            CoroutineExceptionHandler { _, exception ->
                handleUncaughtException(exception, NoOpBootstrapLogger())
            },
    )

    /**
     * Runs the application asynchronously with proper lifecycle management.
     *
     * ## Execution Flow
     * 1. Install signal handlers for graceful shutdown
     * 2. Parse and validate command-line arguments
     * 3. Launch main program in supervised coroutine
     * 4. Launch shutdown monitor coroutine
     * 5. Wait for either completion or shutdown
     * 6. Clean up resources
     *
     * ## The coroutineScope Builder
     * Creates a scope that:
     * - Waits for all children to complete
     * - Cancels all children if one fails (without handler)
     * - Ensures structured concurrency
     *
     * ## Error Handling Strategy
     * Uses ExitCode enum for standardized exit codes and error mapping.
     *
     * @param args Command-line arguments
     * @param signals Signal handler implementation (defaults to JvmSignals)
     * @param logger Optional logger for lifecycle messages (defaults to console)
     * @return Exit code for the OS
     */
    suspend fun runAsync(
        args: Array<String>,
        signals: SystemSignals = JvmSignals(),
        logger: BootstrapLogger = ConsoleBootstrapLogger(),
    ): Int = coroutineScope {
        // Re-initialize per-run state
        isShuttingDown.set(false)
        exitCode.set(ExitCode.SUCCESS)
        appScope = CoroutineScope(
            Dispatchers.Default +
                SupervisorJob() +
                CoroutineExceptionHandler { _, exception ->
                    handleUncaughtException(exception, logger)
                },
        )

        try {
            // Step 1: Install signal handlers for graceful shutdown
            signals.install { initiateShutdown(logger) }

            // Step 2: Parse configuration with security validation
            // This might throw IllegalArgumentException for invalid args
            val cfg = parseArgs(args)

            // Step 3: Launch the main program in a supervised coroutine
            val programJob = launch(appScope.coroutineContext) {
                try {
                    // Build dependency graph and run the application
                    val result = CompositionRoot.buildAndRunAsync(cfg)
                    exitCode.set(if (result == 0) ExitCode.SUCCESS else ExitCode.ERROR)
                } catch (e: CancellationException) {
                    // Normal cancellation during shutdown - log minimal info to avoid swallowing
                    logger.info("Program cancelled gracefully: ${e.message ?: "Clean shutdown"}")
                    exitCode.set(ExitCode.SIGINT) // Standard Unix exit code for SIGINT
                } catch (e: Exception) {
                    // Handle any other exception during execution
                    handleProgramException(e, logger)
                }
            }

            // Step 4: Launch shutdown monitor
            val shutdownJob = launch {
                awaitShutdown() // Suspends until shutdown signal
                programJob.cancelAndJoin() // Cancel program and wait
            }

            // Step 5: Race between normal completion and shutdown
            // The select expression completes when ANY branch completes
            select<Unit> {
                programJob.onJoin {
                    // Program completed normally
                    shutdownJob.cancel() // No need to monitor anymore
                }
                shutdownJob.onJoin {
                    // Shutdown was initiated
                    // programJob already cancelled by shutdownJob
                }
            }

            exitCode.get().code
        } catch (e: ShowHelpException) {
            // Help was displayed, exit successfully
            logger.info("Help displayed: ${e.message}")
            ExitCode.SUCCESS.code
        } catch (e: ShowVersionException) {
            // Version was displayed, exit successfully
            logger.info("Version displayed: ${e.message}")
            ExitCode.SUCCESS.code
        } catch (e: IllegalArgumentException) {
            logger.error("Configuration error: ${e.message}")
            ExitCode.EX_USAGE.code
        } catch (e: SecurityException) {
            logger.error("Security error: ${e.message}")
            ExitCode.EX_NOPERM.code
        } catch (e: Exception) {
            handleUncaughtException(e, logger)
            ExitCode.EX_SOFTWARE.code
        } finally {
            cleanup(logger)
        }
    }

    /**
     * Blocking run method for compatibility.
     *
     * ## When This Is Used
     * - Legacy integration that expects blocking behavior
     * - Testing frameworks that don't support suspend functions
     * - When HYBRID_ASYNC_MODE environment variable is false
     *
     * ## Why Not Always Async?
     * Some environments (old frameworks, certain containers) expect
     * the main thread to block until completion.
     *
     * @param args Command-line arguments
     * @param signals Signal handler implementation (defaults to JvmSignals)
     * @param logger Optional logger for lifecycle messages (defaults to console)
     * @return Exit code for the OS
     */
    fun run(
        args: Array<String>,
        signals: SystemSignals = JvmSignals(),
        logger: BootstrapLogger = ConsoleBootstrapLogger(),
    ): Int = runBlocking {
        runAsync(args, signals, logger)
    }

    /**
     * Initiates graceful shutdown.
     *
     * ## Shutdown Process
     * 1. Check if already shutting down (atomic operation)
     * 2. Start grace period timer
     * 3. Signal all waiting threads via CountDownLatch
     * 4. If grace period expires, force exit
     *
     * ## Atomic Operations
     * `compareAndSet(false, true)` atomically:
     * - Checks if current value is false
     * - If yes, sets to true and returns true
     * - If no, returns false
     *
     * This ensures only ONE thread initiates shutdown,
     * even if multiple signals arrive simultaneously.
     *
     * ## Grace Period
     * 5-second delay allows:
     * - Current operations to complete
     * - Database transactions to commit
     * - Network requests to finish
     * - Resources to be released
     *
     * @param logger Logger for shutdown messages
     */
    private fun initiateShutdown(logger: BootstrapLogger) {
        if (isShuttingDown.compareAndSet(false, true)) {
            logger.info("Initiating graceful shutdown...")

            // Launch grace period timer
            cleanupJob = appScope.launch {
                try {
                    // Give the application time to finish current operations
                    delay(SHUTDOWN_GRACE_PERIOD_MILLIS) // 5 second grace period

                    // If we're still here, grace period expired
                    if (shutdownLatch.count > 0) {
                        logger.error("Forceful shutdown after timeout")
                        exitCode.set(ExitCode.fromThrowable(InterruptedException("Shutdown timeout")))
                        shutdownLatch.countDown()
                    }
                } catch (e: CancellationException) {
                    // Normal: cleanup was cancelled because shutdown completed - log to avoid swallowing
                    logger.info("Cleanup cancelled: ${e.message ?: "Normal shutdown completion"}")
                }
            }

            // Signal all threads waiting for shutdown
            shutdownLatch.countDown()
        }
    }

    /**
     * Waits for shutdown signal.
     *
     * ## Suspending Coroutine Magic
     * `suspendCancellableCoroutine` allows us to:
     * - Suspend the coroutine (doesn't block thread!)
     * - Resume when something happens (shutdown signal)
     * - Handle cancellation properly
     *
     * ## Thread Management
     * Uses Dispatchers.IO because CountDownLatch.await() blocks.
     * This ensures we don't block the Default dispatcher's threads.
     *
     * ## The Flow
     * 1. Coroutine suspends here
     * 2. Thread is freed for other work
     * 3. When latch counts down, continuation resumes
     * 4. Coroutine continues execution
     */
    private suspend fun awaitShutdown() = suspendCancellableCoroutine<Unit> { cont ->
        appScope.launch {
            withContext(Dispatchers.IO) {
                shutdownLatch.await()
            }
            cont.resumeWith(Result.success(Unit))
        }
    }

    /**
     * Handles uncaught exceptions in coroutines.
     *
     * Uses ExitCode.fromThrowable to map exceptions to appropriate exit codes
     * following Unix conventions. Always initiates shutdown after logging.
     *
     * @param exception The uncaught exception
     * @param logger Logger for error messages
     */
    private fun handleUncaughtException(exception: Throwable, logger: BootstrapLogger) {
        val exitCodeToUse = ExitCode.fromThrowable(exception)

        when (exception) {
            is OutOfMemoryError -> {
                logger.error("CRITICAL: Out of memory")
            }
            is StackOverflowError -> {
                logger.error("CRITICAL: Stack overflow")
            }
            is SecurityException -> {
                logger.error("SECURITY: ${exception.message}")
            }
            else -> {
                logger.error("FATAL: Unhandled exception: ${exception.javaClass.simpleName}", exception)
            }
        }

        exitCode.set(exitCodeToUse)
        initiateShutdown(logger)
    }

    /**
     * Handles exceptions during program execution.
     *
     * Uses ExitCode.fromThrowable to map exceptions to appropriate exit codes
     * following Unix conventions.
     *
     * @param exception The exception caught during execution
     * @param logger Logger for error messages
     */
    private fun handleProgramException(exception: Exception, logger: BootstrapLogger) {
        val exitCodeToUse = ExitCode.fromThrowable(exception)
        val message = when (exception) {
            is IllegalStateException -> "Invalid state: ${exception.message}"
            is IllegalArgumentException -> "Invalid argument: ${exception.message}"
            is SecurityException -> "Security violation: ${exception.message}"
            is InterruptedException -> "Interrupted: ${exception.message}"
            else -> "Error: ${exception.message ?: exception.javaClass.simpleName}"
        }

        logger.error(message)
        exitCode.set(exitCodeToUse)
    }

    /**
     * Performs cleanup operations.
     *
     * ## Cleanup Steps
     * 1. Cancel grace period timer (if still running)
     * 2. Cancel application scope (signals all coroutines)
     * 3. Wait up to 2 seconds for coroutines to finish
     * 4. Log completion
     *
     * ## Why runBlocking Here?
     * We're already shutting down and need to wait for
     * coroutines to complete. This is one of the few
     * appropriate uses of runBlocking.
     *
     * ## withTimeoutOrNull
     * - Waits up to 2 seconds for coroutines to finish
     * - Returns null if timeout expires
     * - Prevents hanging forever on stuck coroutines
     *
     * ## Error Handling
     * Cleanup errors are logged but don't prevent shutdown.
     * The app is terminating anyway.
     *
     * @param logger Logger for cleanup messages
     */
    private fun cleanup(logger: BootstrapLogger) {
        try {
            cleanupJob?.cancel()

            // Cancel the application scope (signals all coroutines to stop)
            appScope.cancel()

            // Wait for coroutines to finish (with timeout)
            runBlocking {
                withTimeoutOrNull(CLEANUP_JOIN_TIMEOUT_MILLIS) {
                    // Join all child jobs - waits for them to complete
                    appScope.coroutineContext.job.children.forEach { it.join() }
                }
            }

            logger.info("Cleanup completed")
        } catch (e: Exception) {
            logger.error("Error during cleanup: ${e.message}")
        }
    }
}

/**
 * Async main function that properly handles coroutines and signals.
 *
 * ## What is a Suspend Main Function?
 * Kotlin allows main to be a suspend function, enabling modern async patterns:
 * ```kotlin
 * // Traditional blocking main
 * fun main(args: Array<String>) {
 *     runBlocking { // Required wrapper
 *         someAsyncWork()
 *     }
 * }
 *
 * // Modern suspend main
 * suspend fun main(args: Array<String>) {
 *     someAsyncWork() // Direct async calls
 * }
 * ```
 *
 * ## Why Suspend Main?
 * Benefits of suspend main over traditional main:
 * - No need for runBlocking wrapper in main
 * - Proper coroutine context from the start
 * - Better integration with coroutine debuggers
 * - Cleaner stack traces in async code
 * - Natural async/await patterns throughout
 *
 * ## Exit Process
 * `exitProcess(code)` is the proper way to exit with a code:
 * - Ensures all shutdown hooks run properly
 * - Returns meaningful exit code to the OS
 * - Works consistently across platforms (Unix, Windows, etc.)
 * - Forces immediate termination (vs return from main)
 *
 * ## Process Exit Codes in Practice
 * Operating systems and scripts use exit codes to determine success:
 * ```bash
 * # Shell scripting example
 * if myapp "Alice"; then
 *     echo "App succeeded"
 * else
 *     echo "App failed with code $?"
 * fi
 * ```
 *
 * ## Usage Examples
 * ```bash
 * # Basic usage
 * java -jar hybrid-app.jar Alice
 *
 * # With options
 * java -jar hybrid-app.jar --verbose --out=results.txt Bob
 *
 * # Check exit code (Unix/Linux/Mac)
 * echo $?
 *
 * # Check exit code (Windows)
 * echo %ERRORLEVEL%
 *
 * # Use in scripts
 * java -jar hybrid-app.jar "Charlie" && echo "Success!" || echo "Failed!"
 * ```
 *
 * ## Integration with CI/CD
 * Exit codes are crucial for automated builds:
 * ```yaml
 * # GitHub Actions example
 * - name: Run Application
 *   run: java -jar app.jar TestUser
 *   # Action fails if exit code != 0
 * ```
 *
 * @param args Command-line arguments passed to the program
 */
suspend fun main(args: Array<String>) {
    val exitCode = AsyncApp.runAsync(args)
    exitProcess(exitCode)
}
