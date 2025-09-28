// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.application.usecase

import arrow.core.Either
import com.abitofhelp.hybrid.application.dto.CreateGreetingCommand
import com.abitofhelp.hybrid.application.dto.GreetingResult
import com.abitofhelp.hybrid.application.error.ApplicationError
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

/**
 * Use case for creating greetings in batch with concurrent processing.
 * Demonstrates advanced async patterns with Flow and channels.
 */
class CreateBatchGreetingsUseCase(
    private val createGreetingUseCase: CreateGreetingUseCase,
    private val concurrencyLimit: Int = 10,
) {

    /**
     * Process multiple greetings concurrently using Flow.
     * Returns a flow of results that can be collected asynchronously.
     */
    fun executeBatch(commands: List<CreateGreetingCommand>): Flow<Either<ApplicationError, GreetingResult>> =
        commands.asFlow()
            .map { command ->
                // Process each greeting asynchronously
                createGreetingUseCase.execute(command)
            }
            .buffer(concurrencyLimit) // Limit concurrent processing

    /**
     * Process greetings with a channel for real-time streaming.
     * Useful for scenarios where greetings come from an async source.
     */
    suspend fun executeStream(
        commandChannel: Channel<CreateGreetingCommand>,
    ): Flow<Either<ApplicationError, GreetingResult>> = channelFlow {
        coroutineScope {
            // Create a worker pool
            repeat(concurrencyLimit) { workerId ->
                launch {
                    for (command in commandChannel) {
                        val result = createGreetingUseCase.execute(command)
                        send(result)
                    }
                }
            }
        }
    }

    /**
     * Process greetings with aggregated results.
     * Waits for all greetings to complete and returns aggregated statistics.
     */
    suspend fun executeBatchWithStats(
        commands: List<CreateGreetingCommand>,
    ): BatchGreetingResult = coroutineScope {
        val startTime = System.currentTimeMillis()

        // Process all greetings concurrently
        val results = commands.map { command ->
            async {
                createGreetingUseCase.execute(command)
            }
        }.awaitAll()

        val endTime = System.currentTimeMillis()

        // Calculate statistics
        val successful = results.count { it.isRight() }
        val failed = results.count { it.isLeft() }
        val processingTimeMs = endTime - startTime

        BatchGreetingResult(
            totalProcessed = commands.size,
            successful = successful,
            failed = failed,
            processingTimeMs = processingTimeMs,
            results = results,
        )
    }

    /**
     * Process greetings with retry logic for failures.
     */
    suspend fun executeWithRetry(
        command: CreateGreetingCommand,
        maxRetries: Int = 3,
        delayMs: Long = 100,
    ): Either<ApplicationError, GreetingResult> {
        repeat(maxRetries) { attempt ->
            val result = createGreetingUseCase.execute(command)
            if (result.isRight()) {
                return result
            }

            if (attempt < maxRetries - 1) {
                delay(delayMs * (attempt + 1)) // Exponential backoff
            }
        }

        // Final attempt
        return createGreetingUseCase.execute(command)
    }
}

/**
 * Result of batch greeting processing with statistics.
 */
data class BatchGreetingResult(
    val totalProcessed: Int,
    val successful: Int,
    val failed: Int,
    val processingTimeMs: Long,
    val results: List<Either<ApplicationError, GreetingResult>>,
) {
    val averageTimePerGreeting: Double =
        if (totalProcessed > 0) processingTimeMs.toDouble() / totalProcessed else 0.0
}
