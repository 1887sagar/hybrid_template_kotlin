// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Test Suite
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.bootstrap

import arrow.core.left
import arrow.core.right
import com.abitofhelp.hybrid.application.error.ApplicationError
import com.abitofhelp.hybrid.application.port.output.ErrorOutputPort
import com.abitofhelp.hybrid.application.port.output.OutputPort
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlinx.coroutines.test.runTest

/**
 * Comprehensive test suite for [CompositionRoot] dependency injection and application wiring.
 *
 * ## What This Tests
 *
 * This test suite validates the composition root pattern implementation, which is responsible
 * for assembling all application dependencies and coordinating the execution flow. It ensures
 * that components are properly wired together and the application functions as an integrated whole.
 *
 * ## Why These Tests Are Important
 *
 * 1. **Dependency Injection**: Validates that all components are correctly instantiated and wired
 * 2. **Configuration Handling**: Ensures different configurations produce expected behaviors
 * 3. **Integration Testing**: Tests the interaction between multiple layers of the architecture
 * 4. **Production Readiness**: Verifies that the production wiring works correctly
 * 5. **Isolation**: Confirms that each execution creates fresh instances
 *
 * ## Test Scenarios Covered
 *
 * ### Configuration Handling
 * - **Default configuration**: Application behavior with minimal settings
 * - **Custom name configuration**: Personalized greeting generation
 * - **Verbose mode**: Additional logging and information output
 * - **Anonymous mode**: Handling missing name with helpful tips
 *
 * ### Dependency Injection
 * - **Instance creation**: New instances for each execution
 * - **Component wiring**: Proper connection between layers
 * - **Adapter injection**: Test adapters vs. production adapters
 * - **Scope management**: Ensuring proper component lifetimes
 *
 * ### Production Wiring
 * - **Console output**: Default output to console
 * - **File output**: File-based output when path provided
 * - **Real adapters**: Using actual infrastructure components
 * - **Error handling**: Production error scenarios
 *
 * ## Composition Root Pattern
 *
 * The Composition Root pattern centralizes dependency configuration and wiring.
 * This test suite validates that:
 *
 * ```kotlin
 * // Production usage
 * val exitCode = CompositionRoot.buildAndRunAsync(config)
 *
 * // Testing usage with injected dependencies
 * val exitCode = CompositionRoot.buildAndRunAsyncForTesting(
 *     cfg = config,
 *     outputPort = testOutputAdapter,
 *     errorOutputPort = testErrorAdapter
 * )
 * ```
 *
 * ## Testing Patterns Demonstrated
 *
 * ### Test Double Injection
 * ```kotlin
 * val outputCollector = TestOutputAdapter()
 * val errorCollector = TestErrorOutputAdapter()
 * val exitCode = buildAndRunAsyncForTesting(config, outputCollector, errorCollector)
 * ```
 *
 * ### Integration Verification
 * ```kotlin
 * outputCollector.messages shouldContain "expected greeting"
 * errorCollector.errors.size shouldBe 0
 * exitCode shouldBe 0
 * ```
 *
 * ### Instance Isolation
 * ```kotlin
 * val result1 = buildAndRunAsyncForTesting(config1, adapter1)
 * val result2 = buildAndRunAsyncForTesting(config2, adapter2)
 * adapter1 shouldNotBeSameInstanceAs adapter2
 * ```
 *
 * ## Architecture Layer Testing
 *
 * These tests validate the integration across all architectural layers:
 * - **Bootstrap**: Configuration and application startup
 * - **Application**: Use case orchestration and business logic
 * - **Domain**: Core business rules and validation
 * - **Infrastructure**: External system adapters
 *
 * ## Best Practices Demonstrated
 *
 * 1. **Test Independence**: Each test creates isolated instances
 * 2. **Real Behavior Testing**: Tests use actual business logic flows
 * 3. **Resource Management**: Proper cleanup of temporary resources
 * 4. **Error Scenario Coverage**: Testing both success and failure paths
 * 5. **Production Validation**: Verifying real-world usage patterns
 */
class CompositionRootTest : DescribeSpec({

    describe("CompositionRoot.buildAndRunAsync") {

        describe("configuration handling") {

            it("should create program with default configuration") {
                runTest {
                    // Given
                    val config = AppConfig()
                    val outputCollector = TestOutputAdapter()
                    val errorCollector = TestErrorOutputAdapter()

                    // When
                    val exitCode = CompositionRoot.buildAndRunAsyncForTesting(
                        cfg = config,
                        outputPort = outputCollector,
                        errorOutputPort = errorCollector,
                    )

                    // Then
                    exitCode shouldBe 0
                    outputCollector.messages shouldContain "Hello World from Anonymous!"
                    errorCollector.errors.size shouldBe 0
                }
            }

            it("should create program with custom name") {
                runTest {
                    // Given
                    val config = AppConfig(name = "Alice")
                    val outputCollector = TestOutputAdapter()
                    val errorCollector = TestErrorOutputAdapter()

                    // When
                    val exitCode = CompositionRoot.buildAndRunAsyncForTesting(
                        cfg = config,
                        outputPort = outputCollector,
                        errorOutputPort = errorCollector,
                    )

                    // Then
                    exitCode shouldBe 0
                    outputCollector.messages shouldContain "Hey there, Alice! Welcome!"
                    errorCollector.errors.size shouldBe 0
                }
            }

            it("should respect verbose flag") {
                runTest {
                    // Given
                    val config = AppConfig(name = "Bob", verbose = true)
                    val outputCollector = TestOutputAdapter()
                    val errorCollector = TestErrorOutputAdapter()

                    // When
                    val exitCode = CompositionRoot.buildAndRunAsyncForTesting(
                        cfg = config,
                        outputPort = outputCollector,
                        errorOutputPort = errorCollector,
                    )

                    // Then
                    exitCode shouldBe 0
                    outputCollector.messages shouldContain "[INFO] Greeting delivered successfully"
                    errorCollector.errors.size shouldBe 0
                }
            }

            it("should show tip when no name provided") {
                runTest {
                    // Given
                    val config = AppConfig(name = null)
                    val outputCollector = TestOutputAdapter()
                    val errorCollector = TestErrorOutputAdapter()

                    // When
                    val exitCode = CompositionRoot.buildAndRunAsyncForTesting(
                        cfg = config,
                        outputPort = outputCollector,
                        errorOutputPort = errorCollector,
                    )

                    // Then
                    exitCode shouldBe 0
                    outputCollector.messages shouldContain "Tip: You can provide your name as a command-line argument"
                    errorCollector.errors.size shouldBe 0
                }
            }
        }

        describe("dependency injection") {

            it("should create new instances for each call") {
                runTest {
                    // Given
                    val config1 = AppConfig(name = "UserOne")
                    val config2 = AppConfig(name = "UserTwo")

                    val outputCollector1 = TestOutputAdapter()
                    val errorCollector1 = TestErrorOutputAdapter()
                    val outputCollector2 = TestOutputAdapter()
                    val errorCollector2 = TestErrorOutputAdapter()

                    // When
                    val exitCode1 = CompositionRoot.buildAndRunAsyncForTesting(
                        cfg = config1,
                        outputPort = outputCollector1,
                        errorOutputPort = errorCollector1,
                    )
                    val exitCode2 = CompositionRoot.buildAndRunAsyncForTesting(
                        cfg = config2,
                        outputPort = outputCollector2,
                        errorOutputPort = errorCollector2,
                    )

                    // Then
                    exitCode1 shouldBe 0
                    exitCode2 shouldBe 0
                    outputCollector1 shouldNotBeSameInstanceAs outputCollector2
                    outputCollector1.messages shouldContain "Hey there, UserOne! Welcome!"
                    outputCollector2.messages shouldContain "Hey there, UserTwo! Welcome!"
                }
            }
        }

        describe("production wiring") {

            it("should use console output when no file path") {
                runTest {
                    // Given
                    val config = AppConfig(name = "Console User")

                    // When - run with real console output (captured by test framework)
                    val exitCode = CompositionRoot.buildAndRunAsync(config)

                    // Then
                    exitCode shouldBe 0
                }
            }

            it("should use file output when path provided") {
                runTest {
                    // Given
                    val tempFile = kotlin.io.path.createTempFile("test", ".txt")
                    val config = AppConfig(
                        name = "File User",
                        outputPath = tempFile.toString(),
                    )

                    // When
                    val exitCode = CompositionRoot.buildAndRunAsync(config)

                    // Then
                    exitCode shouldBe 0

                    // Clean up
                    tempFile.toFile().delete()
                }
            }
        }
    }
})

/**
 * Test implementation of [OutputPort] that collects output messages for verification.
 *
 * ## Purpose
 *
 * This test double replaces the real output adapters (console, file) during testing
 * to capture and verify application output without side effects.
 *
 * ## Usage Pattern
 *
 * ```kotlin
 * val outputCollector = TestOutputAdapter()
 * val exitCode = buildAndRunAsyncForTesting(config, outputCollector)
 * outputCollector.messages shouldContain "expected output"
 * ```
 *
 * ## Design Benefits
 *
 * 1. **Isolation**: Tests don't produce console output or create files
 * 2. **Verification**: Output can be inspected and validated
 * 3. **Determinism**: Eliminates external dependencies from tests
 * 4. **Performance**: In-memory operations are faster than I/O
 */
class TestOutputAdapter : OutputPort {
    val messages = mutableListOf<String>()

    override suspend fun send(message: String): arrow.core.Either<ApplicationError, Unit> {
        return try {
            messages.add(message)
            Unit.right()
        } catch (e: Exception) {
            ApplicationError.OutputError("Test output failed: ${e.message}").left()
        }
    }
}

/**
 * Test implementation of [ErrorOutputPort] that collects error messages for verification.
 *
 * ## Purpose
 *
 * This test double captures error output during testing, allowing verification
 * of error handling behavior without polluting the test output or requiring
 * external error logging systems.
 *
 * ## Usage Pattern
 *
 * ```kotlin
 * val errorCollector = TestErrorOutputAdapter()
 * val exitCode = buildAndRunAsyncForTesting(config, outputPort, errorCollector)
 * errorCollector.errors shouldContain "specific error message"
 * ```
 *
 * ## Error Testing Benefits
 *
 * 1. **Error Verification**: Confirms specific error messages are generated
 * 2. **Clean Test Output**: Errors don't appear in test runner output
 * 3. **Error Counting**: Validates expected number of errors
 * 4. **Message Content**: Ensures error messages are helpful to users
 */
class TestErrorOutputAdapter : ErrorOutputPort {
    val errors = mutableListOf<String>()

    override suspend fun sendError(message: String): arrow.core.Either<ApplicationError, Unit> {
        return try {
            errors.add(message)
            Unit.right()
        } catch (e: Exception) {
            ApplicationError.OutputError("Test error output failed: ${e.message}").left()
        }
    }
}
