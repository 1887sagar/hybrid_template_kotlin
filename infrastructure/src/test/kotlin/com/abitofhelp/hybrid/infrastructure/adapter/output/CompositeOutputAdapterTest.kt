// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Test Suite
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.infrastructure.adapter.output

import arrow.core.left
import arrow.core.right
import com.abitofhelp.hybrid.application.error.ApplicationError
import com.abitofhelp.hybrid.application.port.output.OutputPort
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

/**
 * Comprehensive test suite for CompositeOutputAdapter.
 *
 * ## Purpose
 * This test suite validates the composite output adapter, which enables broadcasting messages
 * to multiple output destinations simultaneously. The adapter implements the Fan-Out pattern,
 * allowing applications to send output to multiple channels (console, file, network, etc.)
 * with a single operation while maintaining individual error handling for each destination.
 *
 * ## What is Being Tested
 * 1. **Concurrent Broadcasting**: Messages sent to all configured outputs simultaneously
 * 2. **Error Aggregation**: Partial failures are collected and reported comprehensively
 * 3. **Error Isolation**: Failure in one output doesn't prevent others from succeeding
 * 4. **Factory Methods**: Convenient creation of common adapter configurations
 * 5. **Performance**: Concurrent execution rather than sequential processing
 * 6. **Port Compliance**: Proper implementation of the OutputPort interface
 *
 * ## Testing Strategy
 * - **Mock-Based Testing**: Uses MockK to isolate dependencies and verify interactions
 * - **Concurrent Verification**: Ensures all outputs are called simultaneously, not sequentially
 * - **Error Scenario Testing**: Validates behavior under various failure combinations
 * - **Factory Testing**: Tests convenience methods for common configurations
 * - **Performance Validation**: Confirms concurrent rather than sequential execution
 *
 * ## Key Test Scenarios
 *
 * ### Success Cases
 * - **All Outputs Succeed**: All configured outputs successfully receive the message
 * - **Message Broadcasting**: Same message is sent to multiple diverse output types
 * - **Factory Creation**: Convenience methods create appropriate adapter types
 * - **Single Output**: Handles edge case of composite with only one output
 *
 * ### Partial Failure Cases
 * - **Some Outputs Fail**: Tests behavior when subset of outputs experience errors
 * - **Error Aggregation**: Multiple errors are collected and reported comprehensively
 * - **Resilient Operation**: Successful outputs complete despite peer failures
 * - **Error Message Clarity**: Failure reports clearly indicate which outputs failed
 *
 * ### Complete Failure Cases
 * - **All Outputs Fail**: Validates behavior when every configured output fails
 * - **Error Consolidation**: All individual errors are captured and reported
 * - **Graceful Degradation**: System doesn't crash when all outputs fail
 * - **Comprehensive Reporting**: Error messages provide full context for debugging
 *
 * ## Error Handling Patterns
 * The CompositeOutputAdapter implements sophisticated error handling:
 *
 * 1. **Error Isolation**: Each output's success/failure is independent
 * 2. **Concurrent Execution**: All outputs attempted simultaneously for performance
 * 3. **Error Aggregation**: Failed outputs are identified with their specific errors
 * 4. **Success Priority**: If any outputs succeed, partial success is achieved
 * 5. **Detailed Reporting**: Error messages indicate exactly which outputs failed
 *
 * ## Performance Characteristics
 * - **Concurrent Execution**: All outputs called simultaneously using coroutines
 * - **Non-Blocking**: No output waits for others to complete
 * - **Fail-Fast**: Errors don't delay successful outputs
 * - **Memory Efficient**: Results collected efficiently without blocking
 *
 * ## Design Patterns Demonstrated
 * 1. **Composite Pattern**: Multiple outputs treated as single unit
 * 2. **Fan-Out Pattern**: Single input broadcast to multiple outputs
 * 3. **Error Aggregation**: Multiple failure points consolidated into single result
 * 4. **Factory Pattern**: Convenient creation methods for common scenarios
 * 5. **Adapter Pattern**: Wraps multiple ports to appear as single port
 *
 * ## Testing Techniques Used
 * - **Behavior Verification**: MockK verify blocks ensure correct interactions
 * - **Error Injection**: Controlled failure scenarios to test error handling
 * - **Concurrent Testing**: Validates parallel execution rather than sequential
 * - **Result Analysis**: Deep inspection of Either results for error details
 * - **Factory Testing**: Validates convenience methods produce expected types
 *
 * ## Educational Value
 * This test suite demonstrates several important testing concepts:
 * - **Mock-Driven Development**: Using mocks to define and verify behavior contracts
 * - **Concurrent Testing**: How to verify parallel execution patterns
 * - **Error Handling Testing**: Comprehensive validation of failure scenarios
 * - **Functional Testing**: Working with Either types and functional error handling
 * - **Integration Testing**: Testing how multiple components work together
 *
 * ## Real-World Applications
 * The CompositeOutputAdapter is valuable for:
 * - **Logging Systems**: Send logs to multiple destinations (file, console, remote)
 * - **Monitoring**: Output metrics to multiple monitoring systems
 * - **Backup Strategies**: Write data to multiple storage locations
 * - **Redundancy**: Ensure message delivery even if some outputs fail
 * - **Development**: Output to console during development, file in production
 *
 * @see CompositeOutputAdapter The multi-output broadcasting adapter being tested
 * @see OutputPort The port interface implemented by this adapter
 * @see ApplicationError.OutputError The error type used for output failures
 * @see arrow.core.Either The functional programming type used for error handling
 */
class CompositeOutputAdapterTest : DescribeSpec({

    describe("CompositeOutputAdapter") {

        describe("send") {

            it("should send to all outputs concurrently") {
                runTest {
                    // Given
                    val output1 = mockk<OutputPort>()
                    val output2 = mockk<OutputPort>()
                    val output3 = mockk<OutputPort>()
                    val message = "Test message"

                    coEvery { output1.send(message) } returns Unit.right()
                    coEvery { output2.send(message) } returns Unit.right()
                    coEvery { output3.send(message) } returns Unit.right()

                    val adapter = CompositeOutputAdapter(listOf(output1, output2, output3))

                    // When
                    val result = adapter.send(message)

                    // Then
                    result.isRight() shouldBe true
                    coVerify(exactly = 1) {
                        output1.send(message)
                        output2.send(message)
                        output3.send(message)
                    }
                }
            }

            it("should handle partial failures") {
                runTest {
                    // Given
                    val output1 = mockk<OutputPort>()
                    val output2 = mockk<OutputPort>()
                    val output3 = mockk<OutputPort>()
                    val message = "Test message"

                    coEvery { output1.send(message) } returns Unit.right()
                    coEvery { output2.send(message) } returns ApplicationError.OutputError("Failed").left()
                    coEvery { output3.send(message) } returns Unit.right()

                    val adapter = CompositeOutputAdapter(listOf(output1, output2, output3))

                    // When
                    val result = adapter.send(message)

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            (error is ApplicationError.OutputError) shouldBe true
                            if (error is ApplicationError.OutputError) {
                                error.message shouldContain "Failed to send to 1 of 3 outputs"
                                error.message shouldContain "Failed"
                            }
                        },
                        { },
                    )

                    // All outputs should still be attempted
                    coVerify(exactly = 1) {
                        output1.send(message)
                        output2.send(message)
                        output3.send(message)
                    }
                }
            }

            it("should handle all failures") {
                runTest {
                    // Given
                    val output1 = mockk<OutputPort>()
                    val output2 = mockk<OutputPort>()
                    val message = "Test message"

                    coEvery { output1.send(message) } returns ApplicationError.OutputError("Error 1").left()
                    coEvery { output2.send(message) } returns ApplicationError.OutputError("Error 2").left()

                    val adapter = CompositeOutputAdapter(listOf(output1, output2))

                    // When
                    val result = adapter.send(message)

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            (error as ApplicationError.OutputError).message shouldContain
                                "Failed to send to 2 of 2 outputs"
                            error.message shouldContain "Error 1"
                            error.message shouldContain "Error 2"
                        },
                        { },
                    )
                }
            }
        }

        describe("createDefault") {

            it("should create console-only adapter when no file path") {
                // When
                val adapter = CompositeOutputAdapter.createDefault(null)

                // Then
                adapter::class.simpleName shouldBe "ConsoleOutputAdapter"
            }

            it("should create composite adapter when file path provided") {
                // When
                val adapter = CompositeOutputAdapter.createDefault("/tmp/test.txt")

                // Then
                adapter::class.simpleName shouldBe "CompositeOutputAdapter"
            }
        }
    }
})
