////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

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
 *
 * ## What is Batch Processing?
 * Instead of processing items one at a time, batch processing handles multiple items
 * simultaneously. This is crucial for performance when you have many similar operations
 * to perform, like sending emails to multiple users or processing multiple files.
 *
 * ## Why Use Concurrent Processing?
 * - **Performance**: Process multiple items simultaneously instead of sequentially
 * - **Efficiency**: Better utilize system resources (CPU, I/O, network)
 * - **User Experience**: Faster completion of bulk operations
 * - **Scalability**: Handle larger workloads without proportional time increase
 *
 * ## Different Processing Patterns
 * This use case demonstrates several concurrent processing patterns:
 *
 * 1. **Flow-based Processing**: Stream results as they complete
 * 2. **Channel-based Processing**: Handle real-time data streams
 * 3. **Batch with Statistics**: Wait for all to complete, provide summary
 * 4. **Retry Logic**: Handle failures gracefully with retries
 *
 * ## When to Use Each Pattern
 * - **Flow**: When you want to display progress or handle results as they arrive
 * - **Channel**: When input is coming from a stream (websocket, file reader, etc.)
 * - **Batch with Stats**: When you need complete results and metrics
 * - **Retry**: When operations might fail due to temporary issues
 *
 * ## Example Usage
 * ```kotlin
 * class GreetingController(
 *     private val batchUseCase: CreateBatchGreetingsUseCase
 * ) {
 *     // Process and show progress
 *     suspend fun greetMultipleUsers(names: List<String>) {
 *         val commands = names.map { CreateGreetingCommand(it) }
 *         
 *         batchUseCase.executeBatch(commands)
 *             .collect { result ->
 *                 result.fold(
 *                     { error -> logger.warn("Failed greeting: $error") },
 *                     { greeting -> println("✓ ${greeting.greeting}") }
 *                 )
 *             }
 *     }
 *
 *     // Process and get final statistics
 *     suspend fun greetUsersWithReport(names: List<String>): String {
 *         val commands = names.map { CreateGreetingCommand(it) }
 *         val result = batchUseCase.executeBatchWithStats(commands)
 *         
 *         return """
 *             Batch Complete:
 *             - Total: ${result.totalProcessed}
 *             - Successful: ${result.successful}
 *             - Failed: ${result.failed}
 *             - Average time: ${result.averageTimePerGreeting}ms
 *         """.trimIndent()
 *     }
 * }
 * ```
 */
class CreateBatchGreetingsUseCase(
    private val createGreetingUseCase: CreateGreetingUseCase,
    private val concurrencyLimit: Int = 10,
) {

    /**
     * Process multiple greetings concurrently using Flow.
     * Returns a flow of results that can be collected asynchronously.
     *
     * ## What is Flow?
     * Flow is Kotlin's way of handling streams of data that arrive over time. Unlike
     * collections (List, Set) that have all data immediately available, Flows emit
     * values one at a time as they become ready.
     *
     * ## Why Use Flow for Batch Processing?
     * - **Progressive Results**: Display results as they complete, not all at once
     * - **Memory Efficient**: Don't hold all results in memory simultaneously
     * - **Backpressure**: Automatically handle fast producers/slow consumers
     * - **Cancellation**: Stop processing when no longer needed
     *
     * ## How This Method Works
     * 1. Convert the list of commands to a Flow
     * 2. Map each command to an async greeting creation
     * 3. Buffer results to limit concurrent operations
     * 4. Return a Flow that emits results as they complete
     *
     * ## Example Usage
     * ```kotlin
     * // Process 100 users and show progress
     * val commands = users.map { CreateGreetingCommand(it.name) }
     * var completed = 0
     * 
     * batchUseCase.executeBatch(commands)
     *     .collect { result ->
     *         completed++
     *         result.fold(
     *             { error -> 
     *                 println("❌ Error: ${error.message}")
     *             },
     *             { greeting -> 
     *                 println("✅ ${greeting.greeting}")
     *             }
     *         )
     *         
     *         // Update progress bar
     *         progressBar.update(completed, commands.size)
     *     }
     * 
     * println("All greetings processed!")
     * ```
     *
     * @param commands List of greeting commands to process
     * @return Flow that emits Either results as greetings complete
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
     *
     * ## What is Channel-Based Processing?
     * Channels are like pipes - you put data in one end and take it out the other.
     * This method creates a pool of workers that process commands as they arrive
     * through the channel, perfect for real-time data streams.
     *
     * ## When to Use This Pattern
     * - **WebSocket Streams**: Messages arriving from real-time connections
     * - **File Processing**: Reading large files line by line
     * - **Message Queues**: Processing messages from RabbitMQ, Kafka, etc.
     * - **User Actions**: Handling rapid user interactions
     *
     * ## How Worker Pool Works
     * 1. Create multiple worker coroutines (based on concurrencyLimit)
     * 2. Each worker waits for commands from the channel
     * 3. When a command arrives, one worker picks it up and processes it
     * 4. Results are sent back through the return Flow
     * 5. Workers automatically balance the load
     *
     * ## Example Usage
     * ```kotlin
     * // Real-time greeting processing from WebSocket
     * class RealtimeGreetingService(
     *     private val batchUseCase: CreateBatchGreetingsUseCase
     * ) {
     *     private val commandChannel = Channel<CreateGreetingCommand>(capacity = 100)
     *     
     *     suspend fun startProcessing() {
     *         batchUseCase.executeStream(commandChannel)
     *             .collect { result ->
     *                 result.fold(
     *                     { error -> sendErrorToClient(error) },
     *                     { greeting -> sendGreetingToClient(greeting) }
     *                 )
     *             }
     *     }
     *     
     *     fun handleWebSocketMessage(message: String) {
     *         val command = CreateGreetingCommand(message)
     *         commandChannel.trySend(command)
     *     }
     * }
     *
     * // File processing example
     * suspend fun processGreetingFile(file: File) {
     *     val commandChannel = Channel<CreateGreetingCommand>()
     *     
     *     // Producer: Read file and send commands
     *     launch {
     *         file.forEachLine { line ->
     *             val command = CreateGreetingCommand(line.trim())
     *             commandChannel.send(command)
     *         }
     *         commandChannel.close()
     *     }
     *     
     *     // Consumer: Process commands and collect results
     *     batchUseCase.executeStream(commandChannel)
     *         .collect { result ->
     *             // Handle each result as it arrives
     *         }
     * }
     * ```
     *
     * @param commandChannel Channel that provides commands to process
     * @return Flow that emits results as they're processed by the worker pool
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
     *
     * ## What is Batch Processing with Statistics?
     * This method processes all greetings concurrently and waits for everything to complete
     * before returning comprehensive statistics. It's perfect when you need to know the
     * final outcome and performance metrics of a batch operation.
     *
     * ## When to Use This Pattern
     * - **Reporting**: Need complete success/failure counts
     * - **Performance Analysis**: Want to measure processing time
     * - **Batch Jobs**: Scheduled tasks that process many items
     * - **Admin Operations**: Bulk operations that need confirmation
     *
     * ## How Concurrent Processing Works
     * ```kotlin
     * // Sequential (slow) - 10 items × 100ms each = 1000ms total
     * commands.forEach { command ->
     *     createGreetingUseCase.execute(command)  // 100ms each
     * }
     *
     * // Concurrent (fast) - 10 items in parallel = ~100ms total
     * val results = commands.map { command ->
     *     async { createGreetingUseCase.execute(command) }  // All start together
     * }.awaitAll()  // Wait for all to complete
     * ```
     *
     * ## Statistics Provided
     * - **Total Processed**: How many items were attempted
     * - **Successful**: How many completed without errors
     * - **Failed**: How many encountered errors
     * - **Processing Time**: Total time from start to finish
     * - **Average Time**: Processing time divided by number of items
     * - **All Results**: Individual results for detailed analysis
     *
     * ## Example Usage
     * ```kotlin
     * // Bulk email notification system
     * suspend fun sendWelcomeEmails(newUsers: List<User>): EmailReport {
     *     val commands = newUsers.map { CreateGreetingCommand(it.name) }
     *     val result = batchUseCase.executeBatchWithStats(commands)
     *     
     *     // Generate detailed report
     *     val report = EmailReport(
     *         totalUsers = result.totalProcessed,
     *         emailsSent = result.successful,
     *         failures = result.failed,
     *         duration = "${result.processingTimeMs}ms",
     *         averageTimePerEmail = "${result.averageTimePerGreeting}ms"
     *     )
     *     
     *     // Log any failures for investigation
     *     result.results.filterIsInstance<Either.Left<ApplicationError>>()
     *         .forEach { error ->
     *             logger.warn("Email failed: ${error.value}")
     *         }
     *     
     *     return report
     * }
     *
     * // Daily batch job with monitoring
     * suspend fun dailyGreetingJob() {
     *     val commands = getUsersForDailyGreeting()
     *     val result = batchUseCase.executeBatchWithStats(commands)
     *     
     *     // Send metrics to monitoring system
     *     metrics.gauge("daily_greetings.total", result.totalProcessed)
     *     metrics.gauge("daily_greetings.successful", result.successful)
     *     metrics.gauge("daily_greetings.failed", result.failed)
     *     metrics.timer("daily_greetings.duration", result.processingTimeMs)
     *     
     *     // Alert if failure rate is too high
     *     val failureRate = result.failed.toDouble() / result.totalProcessed
     *     if (failureRate > 0.1) {  // More than 10% failed
     *         alerting.sendAlert("High failure rate in daily greeting job: ${failureRate * 100}%")
     *     }
     * }
     * ```
     *
     * @param commands List of greeting commands to process
     * @return BatchGreetingResult with complete statistics and all individual results
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
     *
     * ## What is Retry Logic?
     * Sometimes operations fail due to temporary issues (network hiccups, server busy, etc.).
     * Retry logic automatically attempts the operation again after a brief delay, often
     * succeeding on subsequent attempts.
     *
     * ## Why Use Retries?
     * - **Resilience**: Handle temporary failures gracefully
     * - **User Experience**: Avoid showing errors for transient issues
     * - **Success Rate**: Dramatically improve overall success rates
     * - **Cost Reduction**: Avoid manual intervention for temporary problems
     *
     * ## When NOT to Retry
     * Be careful with retries - some failures shouldn't be retried:
     * - **Validation Errors**: Bad input won't get better with retries
     * - **Authentication Failures**: Wrong credentials need user intervention
     * - **Rate Limiting**: Retrying too fast makes the problem worse
     * - **Resource Exhaustion**: System needs time to recover
     *
     * ## How This Implementation Works
     * 1. Try the operation initially
     * 2. If it succeeds, return immediately
     * 3. If it fails, wait for the specified delay
     * 4. Increase the delay for next attempt (exponential backoff)
     * 5. Repeat until maxRetries reached
     * 6. Return the final attempt result (success or failure)
     *
     * ## Example Usage
     * ```kotlin
     * // Basic retry for unstable network conditions
     * val result = batchUseCase.executeWithRetry(
     *     command = CreateGreetingCommand("Alice"),
     *     maxRetries = 3,
     *     delayMs = 200
     * )
     *
     * result.fold(
     *     { error -> logger.error("Failed after 3 retries: $error") },
     *     { greeting -> logger.info("Succeeded: ${greeting.greeting}") }
     * )
     *
     * // Custom retry logic for different error types
     * class ResilientGreetingService(
     *     private val batchUseCase: CreateBatchGreetingsUseCase
     * ) {
     *     suspend fun createResilientGreeting(name: String): GreetingResult? {
     *         val command = CreateGreetingCommand(name)
     *         
     *         return batchUseCase.executeWithRetry(
     *             command = command,
     *             maxRetries = 5,
     *             delayMs = 100
     *         ).fold(
     *             { error ->
     *                 when (error) {
     *                     is ApplicationError.ValidationError -> {
     *                         // Don't retry validation errors
     *                         logger.warn("Invalid input: ${error.message}")
     *                         null
     *                     }
     *                     is ApplicationError.OutputError -> {
     *                         // Log infrastructure issues but continue
     *                         logger.error("Infrastructure failure: ${error.message}")
     *                         null
     *                     }
     *                     else -> {
     *                         logger.error("Unexpected error: $error")
     *                         null
     *                     }
     *                 }
     *             },
     *             { greeting -> greeting }
     *         )
     *     }
     * }
     *
     * // Combining retry with batch processing
     * suspend fun processWithRetry(commands: List<CreateGreetingCommand>) {
     *     commands.forEach { command ->
     *         batchUseCase.executeWithRetry(command, maxRetries = 2)
     *             .fold(
     *                 { error -> metrics.incrementCounter("greeting_failures") },
     *                 { greeting -> metrics.incrementCounter("greeting_successes") }
     *             )
     *     }
     * }
     * ```
     *
     * @param command The greeting command to execute with retry logic
     * @param maxRetries Maximum number of retry attempts (default: 3)
     * @param delayMs Initial delay between retries in milliseconds (default: 100ms)
     * @return Either the successful GreetingResult or the final ApplicationError
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
 *
 * ## What is a Result DTO with Statistics?
 * This data class aggregates the results of a batch operation, providing both
 * individual results and summary statistics. It's designed to give you complete
 * visibility into what happened during batch processing.
 *
 * ## Why Include Statistics?
 * When processing hundreds or thousands of items, you need to know:
 * - **Overall Success Rate**: Are most operations succeeding?
 * - **Performance Metrics**: How long did the batch take?
 * - **Error Analysis**: Which specific items failed and why?
 * - **Resource Planning**: How much capacity do we need for future batches?
 *
 * ## Real-World Usage
 * ```kotlin
 * // Daily report generation
 * suspend fun generateDailyReport(): DailyReport {
 *     val commands = getDailyGreetingTasks()
 *     val batchResult = batchUseCase.executeBatchWithStats(commands)
 *     
 *     return DailyReport(
 *         date = LocalDate.now(),
 *         summary = """
 *             Daily Greeting Batch Complete:
 *             • Total users processed: ${batchResult.totalProcessed}
 *             • Successful greetings: ${batchResult.successful}
 *             • Failed attempts: ${batchResult.failed}
 *             • Success rate: ${batchResult.successRate}%
 *             • Total processing time: ${batchResult.processingTimeMs}ms
 *             • Average time per greeting: ${batchResult.averageTimePerGreeting}ms
 *         """.trimIndent(),
 *         failedUsers = batchResult.getFailedItems(),
 *         performanceMetrics = batchResult.getPerformanceMetrics()
 *     )
 * }
 *
 * // Monitoring and alerting
 * suspend fun monitorBatchHealth(result: BatchGreetingResult) {
 *     // Send metrics to monitoring system
 *     metrics.gauge("batch_greetings.total", result.totalProcessed)
 *     metrics.gauge("batch_greetings.successful", result.successful)
 *     metrics.gauge("batch_greetings.failed", result.failed)
 *     metrics.timer("batch_greetings.duration", result.processingTimeMs)
 *     metrics.gauge("batch_greetings.avg_time", result.averageTimePerGreeting)
 *     
 *     // Alert on high failure rates
 *     if (result.successRate < 95.0) {
 *         alerting.sendAlert(
 *             severity = AlertSeverity.HIGH,
 *             message = "Batch greeting success rate below threshold: ${result.successRate}%"
 *         )
 *     }
 *     
 *     // Alert on slow performance
 *     if (result.averageTimePerGreeting > 500) { // More than 500ms per greeting
 *         alerting.sendAlert(
 *             severity = AlertSeverity.MEDIUM,
 *             message = "Batch greeting performance degraded: ${result.averageTimePerGreeting}ms avg"
 *         )
 *     }
 * }
 * ```
 *
 * @property totalProcessed Total number of greeting commands that were attempted
 * @property successful Number of greetings that completed successfully
 * @property failed Number of greetings that encountered errors
 * @property processingTimeMs Total time in milliseconds from start to completion
 * @property results List of individual Either results for detailed analysis
 */
data class BatchGreetingResult(
    val totalProcessed: Int,
    val successful: Int,
    val failed: Int,
    val processingTimeMs: Long,
    val results: List<Either<ApplicationError, GreetingResult>>,
) {
    /**
     * Average processing time per greeting in milliseconds.
     * 
     * Calculated as total processing time divided by number of items.
     * Returns 0.0 if no items were processed (avoids division by zero).
     */
    val averageTimePerGreeting: Double =
        if (totalProcessed > 0) processingTimeMs.toDouble() / totalProcessed else 0.0
    
    /**
     * Success rate as a percentage (0.0 to 100.0).
     * 
     * Calculated as (successful / total) * 100.
     * Returns 0.0 if no items were processed.
     */
    val successRate: Double =
        if (totalProcessed > 0) (successful.toDouble() / totalProcessed) * 100.0 else 0.0
    
    /**
     * Gets all the errors from failed greeting attempts.
     * 
     * Useful for error analysis and debugging batch failures.
     * 
     * @return List of ApplicationErrors from failed attempts
     */
    fun getErrors(): List<ApplicationError> =
        results.mapNotNull { result ->
            when (result) {
                is Either.Left -> result.value
                is Either.Right -> null
            }
        }
    
    /**
     * Gets all successful greeting results.
     * 
     * Useful when you need to process or display the successful outcomes.
     * 
     * @return List of GreetingResults from successful attempts
     */
    fun getSuccessfulResults(): List<GreetingResult> =
        results.mapNotNull { result ->
            when (result) {
                is Either.Left -> null
                is Either.Right -> result.value
            }
        }
}
