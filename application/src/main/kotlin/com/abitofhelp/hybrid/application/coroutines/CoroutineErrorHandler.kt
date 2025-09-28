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
 */
object CoroutineErrorHandler {

    /**
     * Creates a coroutine exception handler that converts exceptions to application errors.
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
     */
    private fun convertExceptionToApplicationError(exception: Throwable, context: String): ApplicationError =
        when (exception) {
            is CancellationException -> throw exception // Let cancellation propagate
            is IllegalArgumentException -> createValidationError(context, "Invalid argument: ${exception.message}")
            is IllegalStateException -> createValidationError(context, "Invalid state: ${exception.message}")
            is SecurityException -> createSecurityError(exception)
            is OutOfMemoryError -> createCriticalResourceError(context, "Out of memory")
            is StackOverflowError -> createCriticalResourceError(context, "Stack overflow")
            is java.io.IOException -> createIOError(exception)
            is TimeoutCancellationException -> createTimeoutError(context)
            else -> createUnexpectedError(context, exception)
        }

    private fun createValidationError(context: String, message: String) =
        ApplicationError.UseCaseError(useCase = context, cause = message)

    private fun createSecurityError(exception: SecurityException) =
        ApplicationError.OutputError("Security error: ${exception.message ?: "Access denied"}")

    private fun createCriticalResourceError(context: String, errorType: String): ApplicationError {
        System.err.println("CRITICAL: $errorType in $context")
        return ApplicationError.UseCaseError(useCase = context, cause = errorType)
    }

    private fun createIOError(exception: java.io.IOException) =
        ApplicationError.OutputError("I/O error: ${exception.message ?: "Failed to read/write"}")

    private fun createTimeoutError(context: String) =
        ApplicationError.UseCaseError(useCase = context, cause = "Operation timed out")

    private fun createUnexpectedError(context: String, exception: Throwable) =
        ApplicationError.UseCaseError(
            useCase = context,
            cause = "Unexpected error: ${exception.message ?: exception.javaClass.simpleName}",
        )

    /**
     * Logs exceptions based on their severity.
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
     */
    fun supervisedScope(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): CoroutineScope = CoroutineScope(
        SupervisorJob() + dispatcher + create(),
    )
}

/**
 * Extension function to add retry logic with exponential backoff.
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
