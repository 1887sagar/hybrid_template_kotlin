// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.application.coroutines

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.abitofhelp.hybrid.application.error.ApplicationError
import kotlinx.coroutines.*

/**
 * Provides structured error handling for coroutines in the application layer.
 *
 * ## What is a CoroutineErrorHandler?
 * This object centralizes error handling for coroutines across the application. When a coroutine
 * fails with an unhandled exception, it needs special handling to prevent the entire application
 * from crashing. Think of it as a safety net that catches errors and converts them to our
 * application's error types.
 *
 * ## Why Do We Need This?
 * Coroutines can fail in many ways:
 * - Network timeouts when calling external services
 * - Invalid data causing exceptions
 * - Resource exhaustion (memory, threads)
 * - Unexpected runtime errors
 *
 * Without proper error handling, these failures could:
 * - Crash the entire application
 * - Leave resources uncleaned
 * - Provide poor user experience
 * - Make debugging difficult
 *
 * ## How It Works
 * This handler:
 * 1. Intercepts exceptions in coroutines
 * 2. Converts them to appropriate ApplicationError types
 * 3. Logs critical errors for monitoring
 * 4. Allows graceful error recovery
 *
 * ## Example Usage
 * ```kotlin
 * // Create a coroutine scope with error handling
 * val scope = CoroutineScope(
 *     Dispatchers.IO + CoroutineErrorHandler.create(
 *         context = "UserDataSync",
 *         onError = { error ->
 *             logger.error("Sync failed: $error")
 *             notifyUser("Sync failed, will retry later")
 *         }
 *     )
 * )
 *
 * // Launch coroutines in this scope - errors are handled automatically
 * scope.launch {
 *     userRepository.syncData() // If this throws, error handler catches it
 * }
 *
 * // Or use safeExecute for explicit error handling
 * val result = CoroutineErrorHandler.safeExecute("LoadUserProfile") {
 *     userService.loadProfile(userId)
 * }
 * result.fold(
 *     { error -> showError("Failed to load profile") },
 *     { profile -> showProfile(profile) }
 * )
 * ```
 */
object CoroutineErrorHandler {

    /**
     * Creates a coroutine exception handler that converts exceptions to application errors.
     *
     * ## What Does This Method Do?
     * This factory method creates a CoroutineExceptionHandler that will be invoked when
     * a coroutine fails with an uncaught exception. It's like setting up a try-catch
     * block for all coroutines in a scope.
     *
     * ## Parameters Explained
     *
     * @param context A descriptive name for where this handler is used (e.g., "UserSync", "DataLoad").
     *                This helps with debugging by identifying which part of the app had the error.
     *                Default is "Unknown" if not specified.
     *
     * @param onError A callback function that receives the ApplicationError when something goes wrong.
     *                You can use this to log errors, show user messages, or trigger recovery actions.
     *                Default behavior prints to stderr.
     *
     * ## Example Usage
     * ```kotlin
     * // Basic usage with default error printing
     * val handler = CoroutineErrorHandler.create("FileUpload")
     *
     * // Custom error handling with user notification
     * val handler = CoroutineErrorHandler.create(
     *     context = "ImageProcessing",
     *     onError = { error ->
     *         when (error) {
     *             is ApplicationError.OutputError -> {
     *                 logger.warn("Could not save image: ${error.message}")
     *                 showToast("Image save failed, saved to drafts")
     *             }
     *             is ApplicationError.UseCaseError -> {
     *                 logger.error("Image processing failed: $error")
     *                 showError("Unable to process image")
     *             }
     *         }
     *     }
     * )
     *
     * // Use in a coroutine scope
     * val scope = CoroutineScope(Dispatchers.IO + handler)
     * ```
     *
     * @return A CoroutineExceptionHandler ready to be added to a CoroutineContext
     */
    fun create(
        context: String = "Unknown",
        onError: (ApplicationError) -> Unit = { error ->
            System.err.println("Coroutine error: $error")
        },
    ): CoroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, exception ->
        val applicationError = convertExceptionToApplicationError(exception, context)
        onError(applicationError)
        logExceptionIfNeeded(exception, coroutineContext)
    }

    /**
     * Converts various exception types to application errors.
     *
     * ## What is Exception Mapping?
     * Different types of exceptions need different handling. This method acts as a translator,
     * converting low-level JVM exceptions into our high-level ApplicationError types that
     * the rest of the application understands.
     *
     * ## Why Map Exceptions?
     * - **Consistency**: All errors follow the same ApplicationError structure
     * - **Context**: We can add meaningful context about where/why the error occurred
     * - **Layer Separation**: Infrastructure exceptions don't leak into business logic
     * - **User Experience**: We can provide appropriate user messages for each error type
     *
     * ## Exception Categories
     * - **CancellationException**: Special case - always rethrown to respect coroutine cancellation
     * - **Validation Errors**: IllegalArgumentException, IllegalStateException
     * - **Security Errors**: SecurityException for permission/access issues
     * - **Critical Errors**: OutOfMemoryError, StackOverflowError need immediate attention
     * - **I/O Errors**: Network, file system, database connection issues
     * - **Timeout Errors**: Operations that took too long
     * - **Unexpected**: Anything else we haven't specifically handled
     *
     * @param exception The exception that was thrown
     * @param context Where the exception occurred (helps with debugging)
     * @return An appropriate ApplicationError for the exception type
     */
    private fun convertExceptionToApplicationError(exception: Throwable, context: String): ApplicationError =
        when (exception) {
            is CancellationException -> throw exception // Let cancellation propagate
            is IllegalArgumentException ->
                ErrorFactory.validationError(context, "Invalid argument: ${exception.message}")
            is IllegalStateException -> ErrorFactory.validationError(context, "Invalid state: ${exception.message}")
            is SecurityException -> ErrorFactory.securityError(exception)
            is OutOfMemoryError -> ErrorFactory.criticalResourceError(context, "Out of memory")
            is StackOverflowError -> ErrorFactory.criticalResourceError(context, "Stack overflow")
            is java.io.IOException -> ErrorFactory.ioError(exception)
            is TimeoutCancellationException -> ErrorFactory.timeoutError(context)
            else -> ErrorFactory.unexpectedError(context, exception)
        }

    /**
     * Factory for creating different types of application errors.
     *
     * ## What is the Factory Pattern?
     * A factory is a design pattern that centralizes object creation. Instead of creating
     * ApplicationError instances throughout the code, we have dedicated methods that know
     * exactly how to construct each type of error with the right format and information.
     *
     * ## Why Use a Factory Here?
     * - **Consistency**: All errors of the same type follow the same format
     * - **Maintainability**: Change error format in one place
     * - **Clarity**: Method names clearly indicate what error is being created
     * - **Reusability**: Same error creation logic used throughout the handler
     *
     * ## Design Decision
     * This is a private nested object because:
     * - It's only used within CoroutineErrorHandler
     * - It groups related error creation methods
     * - It keeps the main class focused on error handling flow
     */
    private object ErrorFactory {
        fun validationError(context: String, message: String) =
            ApplicationError.UseCaseError(useCase = context, cause = message)

        fun securityError(exception: SecurityException) =
            ApplicationError.OutputError("Security error: ${exception.message ?: "Access denied"}")

        fun criticalResourceError(context: String, errorType: String): ApplicationError {
            System.err.println("CRITICAL: $errorType in $context")
            return ApplicationError.UseCaseError(useCase = context, cause = errorType)
        }

        fun ioError(exception: java.io.IOException) =
            ApplicationError.OutputError("I/O error: ${exception.message ?: "Failed to read/write"}")

        fun timeoutError(context: String) =
            ApplicationError.UseCaseError(useCase = context, cause = "Operation timed out")

        fun unexpectedError(context: String, exception: Throwable) =
            ApplicationError.UseCaseError(
                useCase = context,
                cause = "Unexpected error: ${exception.message ?: exception.javaClass.simpleName}",
            )
    }

    /**
     * Logs exceptions based on their severity.
     *
     * ## What Does This Do?
     * This method decides whether and how to log exceptions. Not all exceptions need
     * the same treatment - some are critical and need immediate attention, others
     * are expected and just need recording.
     *
     * ## Logging Strategy
     * - **Critical Errors** (OutOfMemoryError, StackOverflowError): Log with context name
     * - **Cancellation**: Don't log (normal coroutine lifecycle)
     * - **Everything Else**: Print full stack trace for debugging
     *
     * ## Why Different Logging Levels?
     * - Critical errors might trigger alerts or immediate investigation
     * - Stack traces help developers debug unexpected issues
     * - Cancellation is normal operation, not an error
     *
     * @param exception The exception that occurred
     * @param coroutineContext Context containing coroutine name and other metadata
     */
    private fun logExceptionIfNeeded(exception: Throwable, coroutineContext: kotlin.coroutines.CoroutineContext) {
        when (exception) {
            is OutOfMemoryError, is StackOverflowError -> {
                System.err.println("Critical error in coroutine: ${coroutineContext[CoroutineName]?.name ?: "unnamed"}")
            }
            !is CancellationException -> {
                exception.printStackTrace()
            }
        }
    }

    /**
     * Executes a suspending block with proper error handling.
     * Converts exceptions to Either<ApplicationError, T>.
     *
     * ## What is safeExecute?
     * This method wraps any suspending operation in a try-catch block and converts
     * exceptions to Either types. It's like a safety wrapper that ensures your code
     * always returns either a successful result or a proper error.
     *
     * ## Why Use This?
     * Instead of try-catch blocks everywhere:
     * ```kotlin
     * // Without safeExecute (verbose and error-prone)
     * val result = try {
     *     api.fetchUser(id).right()
     * } catch (e: CancellationException) {
     *     throw e  // Must rethrow!
     * } catch (e: Exception) {
     *     ApplicationError.UseCaseError("FetchUser", e.message ?: "Unknown").left()
     * }
     *
     * // With safeExecute (clean and consistent)
     * val result = safeExecute("FetchUser") {
     *     api.fetchUser(id)
     * }
     * ```
     *
     * ## Type Parameter <T>
     * The <T> means this method works with any return type. If your operation returns
     * a User, T becomes User. If it returns a List<String>, T becomes List<String>.
     *
     * ## Example Usage
     * ```kotlin
     * // Wrap any suspending call
     * val userResult = safeExecute("LoadUserProfile") {
     *     userRepository.findById(userId)
     * }
     *
     * // Handle the result
     * userResult.fold(
     *     { error ->
     *         logger.error("Failed to load user: $error")
     *         showErrorMessage(error.userMessage)
     *     },
     *     { user ->
     *         updateUI(user)
     *     }
     * )
     *
     * // Chain multiple operations
     * val result = safeExecute("ProcessOrder") {
     *     val order = orderService.create(items)
     *     paymentService.charge(order.total)
     *     emailService.sendConfirmation(order)
     *     order
     * }
     * ```
     *
     * @param useCase Descriptive name for the operation (used in error messages)
     * @param block The suspending operation to execute safely
     * @return Either.Right with the result on success, Either.Left with ApplicationError on failure
     */
    suspend fun <T> safeExecute(
        useCase: String,
        block: suspend () -> T,
    ): Either<ApplicationError, T> = try {
        block().right()
    } catch (e: CancellationException) {
        throw e // Let cancellation propagate
    } catch (e: Exception) {
        ApplicationError.UseCaseError(
            useCase = useCase,
            cause = e.message ?: "Unknown error: ${e.javaClass.simpleName}",
        ).left()
    }

    /**
     * Creates a supervised scope that isolates failures.
     * Child coroutine failures won't cancel the parent.
     *
     * ## What is a Supervised Scope?
     * In normal coroutine scopes, if one child coroutine fails, it cancels all its siblings
     * and the parent. A supervised scope changes this behavior - each child coroutine is
     * independent, so one failure doesn't affect the others.
     *
     * ## Why Use Supervision?
     * Perfect for scenarios where operations are independent:
     * - Processing multiple user requests
     * - Uploading multiple files
     * - Syncing data from multiple sources
     * - Running periodic background tasks
     *
     * ## Example: Without vs With Supervision
     * ```kotlin
     * // Without supervision - one failure cancels everything
     * scope.launch {
     *     launch { processUser1() }  // If this fails...
     *     launch { processUser2() }  // This gets cancelled!
     *     launch { processUser3() }  // This too!
     * }
     *
     * // With supervision - failures are isolated
     * val supervised = CoroutineErrorHandler.supervisedScope()
     * supervised.launch { processUser1() }  // If this fails...
     * supervised.launch { processUser2() }  // This continues running
     * supervised.launch { processUser3() }  // This too!
     * ```
     *
     * ## Complete Example
     * ```kotlin
     * class FileUploadService {
     *     private val uploadScope = CoroutineErrorHandler.supervisedScope(
     *         dispatcher = Dispatchers.IO
     *     )
     *
     *     fun uploadFiles(files: List<File>) {
     *         files.forEach { file ->
     *             uploadScope.launch {
     *                 try {
     *                     api.uploadFile(file)
     *                     markAsUploaded(file)
     *                 } catch (e: Exception) {
     *                     // This file failed, but others continue
     *                     markAsFailed(file, e)
     *                 }
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * @param dispatcher The coroutine dispatcher to use (Default, IO, Main, etc.)
     * @return A CoroutineScope with supervision and error handling
     */
    fun supervisedScope(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): CoroutineScope = CoroutineScope(
        SupervisorJob() + dispatcher + create(),
    )
}

/**
 * Extension function to add retry logic with exponential backoff.
 *
 * ## What is Retry with Backoff?
 * When an operation fails, sometimes it's temporary (network glitch, server busy).
 * This function automatically retries the operation with increasing delays between
 * attempts. Each retry waits longer than the last - this is "exponential backoff".
 *
 * ## Why Exponential Backoff?
 * - **Reduces Load**: Gives the system time to recover
 * - **Avoids Thundering Herd**: Prevents many clients retrying simultaneously
 * - **Better Success Rate**: Later attempts more likely to succeed
 * - **Resource Friendly**: Doesn't hammer a struggling service
 *
 * ## How the Math Works
 * With default values:
 * - Attempt 1: Fails → Wait 100ms
 * - Attempt 2: Fails → Wait 200ms (100 * 2)
 * - Attempt 3: Fails → Wait 400ms (200 * 2)
 * - Attempt 4: Final attempt, no wait after
 *
 * ## Example Usage
 * ```kotlin
 * // Basic retry for API call
 * val user = retryWithBackoff {
 *     api.getUser(userId)  // Might fail due to network issues
 * }
 *
 * // Custom configuration for slower operations
 * val data = retryWithBackoff(
 *     times = 5,           // Try up to 5 times
 *     initialDelayMs = 500, // Start with 500ms delay
 *     factor = 1.5         // Increase by 50% each time
 * ) { attempt ->
 *     logger.debug("Upload attempt ${attempt + 1}")
 *     uploadLargeFile(file)
 * }
 *
 * // With error handling
 * try {
 *     val result = retryWithBackoff {
 *         riskyOperation()
 *     }
 * } catch (e: Exception) {
 *     // All retries failed
 *     logger.error("Operation failed after all retries", e)
 * }
 * ```
 *
 * @param times Maximum number of attempts (default: 3)
 * @param initialDelayMs Delay before first retry in milliseconds (default: 100ms)
 * @param factor Multiplication factor for delay increase (default: 2.0 = double each time)
 * @param block The operation to retry. Receives current attempt number (0-based)
 * @return The successful result from block
 * @throws Exception The last exception if all retries fail
 */
suspend fun <T> retryWithBackoff(
    times: Int = 3,
    initialDelayMs: Long = 100,
    factor: Double = 2.0,
    block: suspend (attempt: Int) -> T,
): T {
    var currentDelay = initialDelayMs
    repeat(times - 1) { attempt ->
        try {
            return block(attempt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Log retry attempt (avoid swallowing exception)
            System.err.println("Retry attempt ${attempt + 1} failed: ${e.message}")
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong()
        }
    }

    // Last attempt
    return block(times - 1)
}

/**
 * Extension function to add timeout with proper error handling.
 *
 * ## What is Timeout Handling?
 * This function wraps an operation with a time limit. If the operation doesn't
 * complete within the specified time, it's cancelled and an error is returned.
 * This prevents operations from hanging forever.
 *
 * ## Why Use Timeouts?
 * - **User Experience**: Don't make users wait indefinitely
 * - **Resource Management**: Free up resources from stuck operations
 * - **System Health**: Detect and handle slow/unresponsive services
 * - **Cascading Failures**: Prevent one slow service from blocking everything
 *
 * ## How It Differs from withTimeout
 * Kotlin's `withTimeout` throws an exception on timeout. This function:
 * - Returns Either instead of throwing
 * - Provides consistent error messages
 * - Logs timeout information
 * - Integrates with our error handling system
 *
 * ## Example Usage
 * ```kotlin
 * // Basic timeout for API call
 * val result = withTimeoutOrError(5000, "FetchUserData") {
 *     api.getUserProfile(userId)
 * }
 *
 * result.fold(
 *     { error ->
 *         when (error) {
 *             is ApplicationError.UseCaseError -> {
 *                 if (error.cause.contains("timed out")) {
 *                     showMessage("Server is slow, please try again")
 *                 }
 *             }
 *         }
 *     },
 *     { profile -> displayProfile(profile) }
 * )
 *
 * // Different timeouts for different operations
 * suspend fun syncData() {
 *     // Quick operation - strict timeout
 *     withTimeoutOrError(1000, "CheckConnection") {
 *         api.ping()
 *     }
 *
 *     // Slow operation - generous timeout
 *     withTimeoutOrError(30000, "DownloadData") {
 *         api.downloadLargeDataset()
 *     }
 * }
 *
 * // Combine with retry for robustness
 * suspend fun reliableFetch(id: String) = retryWithBackoff {
 *     withTimeoutOrError(3000, "FetchItem") {
 *         api.getItem(id)
 *     }.getOrElse { throw Exception(it.toString()) }
 * }
 * ```
 *
 * @param timeMillis Maximum time in milliseconds to wait for completion
 * @param useCase Descriptive name for the operation (used in error messages)
 * @param block The suspending operation to run with timeout
 * @return Either.Right with result on success, Either.Left with timeout error if time exceeded
 */
suspend fun <T> withTimeoutOrError(
    timeMillis: Long,
    useCase: String,
    block: suspend CoroutineScope.() -> T,
): Either<ApplicationError, T> = try {
    withTimeout(timeMillis) {
        block().right()
    }
} catch (e: TimeoutCancellationException) {
    // Log timeout exception to avoid swallowing it
    System.err.println("Timeout in $useCase after ${timeMillis}ms: ${e.message}")
    ApplicationError.UseCaseError(
        useCase = useCase,
        cause = "Operation timed out after ${timeMillis}ms",
    ).left()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    // Log exception to avoid swallowing it
    System.err.println("Exception in $useCase: ${e.message}")
    ApplicationError.UseCaseError(
        useCase = useCase,
        cause = e.message ?: "Unknown error",
    ).left()
}
