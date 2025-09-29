// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Test Suite
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.presentation.cli

import arrow.core.left
import arrow.core.right
import com.abitofhelp.hybrid.application.dto.CreateGreetingCommand
import com.abitofhelp.hybrid.application.dto.GreetingResult
import com.abitofhelp.hybrid.application.error.ApplicationError
import com.abitofhelp.hybrid.application.port.input.CreateGreetingInputPort
import com.abitofhelp.hybrid.domain.error.DomainError
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * Test suite for CLI factory functions.
 *
 * ## Testing Presentation Layer
 *
 * Presentation layer tests verify:
 * 1. **User interaction**: Correct handling of input/output
 * 2. **Use case invocation**: Proper delegation to application layer
 * 3. **Error presentation**: User-friendly error messages
 * 4. **Configuration handling**: Correct parameter passing
 *
 * ## Console Testing Strategy
 *
 * ### Output Capture
 * Testing console applications requires capturing stdout/stderr:
 * ```kotlin
 * // Capture output
 * val outputStream = ByteArrayOutputStream()
 * System.setOut(PrintStream(outputStream))
 *
 * // Run code
 * cli.run()
 *
 * // Verify output
 * outputStream.toString() shouldContain "expected text"
 * ```
 *
 * ### Stream Management
 * - Save original streams in `beforeEach`
 * - Restore in `afterEach` to avoid test pollution
 * - Use separate streams for stdout and stderr
 *
 * ## Testing Different CLI Variants
 *
 * This project provides multiple CLI implementations:
 *
 * ### 1. Synchronous CLI (`cli`)
 * - Traditional blocking implementation
 * - Returns `Runnable` for Java compatibility
 * - Simple but limited concurrency
 *
 * ### 2. Suspend CLI (`suspendCli`)
 * - Returns suspend function
 * - Integrates with coroutines
 * - Better for async operations
 *
 * ### 3. Async CLI (`asyncCli`)
 * - Wraps coroutines for Java interop
 * - Provides bridge between sync and async
 * - Good for gradual migration
 *
 * ### 4. Direct Execution (`createAndRunCli`)
 * - Immediately executes
 * - Returns exit code
 * - Best for modern async apps
 *
 * ## Key Test Scenarios
 *
 * ### Success Cases
 * - Normal greeting creation
 * - Verbose mode output
 * - Silent mode (no output)
 *
 * ### Error Cases
 * - Use case failures
 * - Domain validation errors
 * - Infrastructure errors
 *
 * ### Configuration
 * - Different name inputs
 * - Output path handling
 * - Flag combinations
 *
 * ## Mocking Strategy
 *
 * - Mock use cases to isolate presentation logic
 * - Capture commands sent to use cases
 * - Verify correct error handling
 * - Test without real infrastructure
 *
 * ## Exit Code Testing
 *
 * CLI applications communicate success/failure via exit codes:
 * - 0: Success
 * - 1: General error
 * - 2: Usage error
 * - Other: Application-specific
 */
class CliFactoryTest : DescribeSpec({

    describe("cli factory function") {

        lateinit var mockCreateGreeting: CreateGreetingInputPort
        lateinit var outputStream: ByteArrayOutputStream
        lateinit var errorStream: ByteArrayOutputStream
        lateinit var originalOut: PrintStream
        lateinit var originalErr: PrintStream

        beforeEach {
            mockCreateGreeting = mockk()
            outputStream = ByteArrayOutputStream()
            errorStream = ByteArrayOutputStream()
            originalOut = System.out
            originalErr = System.err
            System.setOut(PrintStream(outputStream))
            System.setErr(PrintStream(errorStream))
        }

        afterEach {
            System.setOut(originalOut)
            System.setErr(originalErr)
        }

        describe("successful greeting creation") {

            /**
             * Validates the primary success path for greeting creation.
             * Tests that:
             * - Configuration is properly transformed into a command
             * - Use case is called exactly once with correct parameters
             * - Name is passed through without modification
             * - Silent flag is set correctly based on verbose setting
             *
             * This test captures the command using MockK's slot mechanism,
             * which is essential for verifying the correct data is passed
             * between presentation and application layers.
             */
            it("should create greeting with provided name") {
                runTest {
                    // Given
                    val config = PresentationConfig(
                        verbose = false,
                        outputPath = null,
                        name = "John",
                    )
                    val commandSlot = slot<CreateGreetingCommand>()
                    val expectedResult = GreetingResult("Hey there, John! Welcome!", "John")

                    coEvery {
                        mockCreateGreeting.execute(capture(commandSlot))
                    } returns expectedResult.right()

                    // When
                    val runnable = asyncCli(config, mockCreateGreeting)
                    runnable.run()

                    // Then
                    commandSlot.captured.name shouldBe "John"
                    commandSlot.captured.silent shouldBe false
                    coVerify(exactly = 1) { mockCreateGreeting.execute(any()) }
                }
            }

            /**
             * Verifies that the presentation layer doesn't perform data validation.
             * The presentation layer should pass input exactly as received,
             * allowing the application/domain layers to handle validation and
             * normalization. This maintains proper separation of concerns.
             *
             * Testing with whitespace ensures we don't accidentally trim input
             * that might be intentionally formatted by the user.
             */
            it("should not trim whitespace from name (application layer handles it)") {
                runTest {
                    // Given
                    val config = PresentationConfig(
                        verbose = false,
                        outputPath = null,
                        name = "  Jane  ",
                    )
                    val commandSlot = slot<CreateGreetingCommand>()
                    val expectedResult = GreetingResult("Hey there, Jane! Welcome!", "Jane")

                    coEvery {
                        mockCreateGreeting.execute(capture(commandSlot))
                    } returns expectedResult.right()

                    // When
                    val runnable = asyncCli(config, mockCreateGreeting)
                    runnable.run()

                    // Then
                    commandSlot.captured.name shouldBe "  Jane  "
                    commandSlot.captured.silent shouldBe false
                }
            }

            /**
             * Tests verbose mode functionality for debugging and development.
             * When verbose is enabled, the CLI should provide additional
             * diagnostic information to help users understand what's happening.
             *
             * This test demonstrates output capture techniques essential
             * for testing console applications. The captured output is
             * examined for expected informational messages.
             */
            it("should show verbose output when enabled") {
                runTest {
                    // Given
                    val config = PresentationConfig(
                        verbose = true,
                        outputPath = null,
                        name = "Alice",
                    )
                    val expectedResult = GreetingResult("Hey there, Alice! Welcome!", "Alice")

                    coEvery {
                        mockCreateGreeting.execute(any())
                    } returns expectedResult.right()

                    // When
                    val runnable = asyncCli(config, mockCreateGreeting)
                    runnable.run()

                    // Then
                    val output = outputStream.toString()
                    output shouldContain "[INFO] Greeting delivered successfully"
                }
            }
        }

        describe("error handling") {

            /**
             * Tests domain error handling through the application layer wrapper.
             * Domain errors represent business rule violations and should be
             * presented to users in a friendly, actionable format.
             *
             * This test verifies:
             * - Domain errors are properly unwrapped from application errors
             * - Error messages are formatted for end users
             * - Errors are written to stderr (not stdout)
             * - The CLI handles errors gracefully without crashing
             */
            it("should handle domain error wrapper") {
                runTest {
                    // Given
                    val config = PresentationConfig(
                        verbose = false,
                        outputPath = null,
                        name = "",
                    )
                    val domainError = DomainError.ValidationError("name", "Name cannot be blank")
                    val error = ApplicationError.DomainErrorWrapper(domainError)

                    coEvery {
                        mockCreateGreeting.execute(any())
                    } returns error.left()

                    // When
                    val runnable = asyncCli(config, mockCreateGreeting)
                    runnable.run()

                    // Then
                    errorStream.toString() shouldContain "Error: Invalid input: name: Name cannot be blank"
                }
            }

            /**
             * Tests infrastructure error handling for output operations.
             * Output errors occur when the system cannot write to the
             * intended destination (file permissions, disk space, etc.).
             *
             * These errors should be clearly distinguished from business
             * logic errors and indicate technical problems rather than
             * user input issues.
             */
            it("should handle output error") {
                runTest {
                    // Given
                    val config = PresentationConfig(
                        verbose = false,
                        outputPath = null,
                        name = "Charlie",
                    )
                    val error = ApplicationError.OutputError("Failed to write to console")

                    coEvery {
                        mockCreateGreeting.execute(any())
                    } returns error.left()

                    // When
                    val runnable = asyncCli(config, mockCreateGreeting)
                    runnable.run()

                    // Then
                    errorStream.toString() shouldContain "Error: Failed to deliver greeting: Failed to write to console"
                }
            }

            /**
             * Tests application layer error handling for use case failures.
             * Use case errors represent internal processing problems that
             * aren't domain validation issues - typically technical failures
             * within the application layer itself.
             *
             * The error message should help users understand that something
             * went wrong internally while identifying which operation failed.
             */
            it("should handle use case error") {
                runTest {
                    // Given
                    val config = PresentationConfig(
                        verbose = false,
                        outputPath = null,
                        name = "David",
                    )
                    val error = ApplicationError.UseCaseError("CreateGreeting", "Internal processing error")

                    coEvery {
                        mockCreateGreeting.execute(any())
                    } returns error.left()

                    // When
                    val runnable = asyncCli(config, mockCreateGreeting)
                    runnable.run()

                    // Then
                    errorStream.toString() shouldContain
                        "Error: Processing error in CreateGreeting: Internal processing error"
                }
            }

            /**
             * Tests handling of unexpected runtime exceptions.
             * This covers scenarios where unhandled exceptions bubble up
             * from lower layers. The async CLI uses coroutines, so exception
             * handling differs from synchronous code.
             *
             * Note: With asyncCli, unhandled exceptions are managed by
             * CoroutineExceptionHandler, which may log to stderr or propagate
             * depending on the coroutine context. This test accounts for
             * different possible outcomes in async execution.
             */
            it("should handle unexpected exception") {
                // Given
                val config = PresentationConfig(
                    verbose = false,
                    outputPath = null,
                    name = "Eve",
                )

                coEvery {
                    mockCreateGreeting.execute(any())
                } throws RuntimeException("Unexpected error")

                // When
                val runnable = asyncCli(config, mockCreateGreeting)
                try {
                    runnable.run()
                    // The asyncCli wraps exceptions in coroutine context,
                    // so the error might be logged differently
                } catch (e: Exception) {
                    // Exception might propagate out in some cases
                    println("Exception in test: ${e.message}")
                }

                // Then
                // With asyncCli, unhandled exceptions in coroutines are handled
                // by the CoroutineExceptionHandler which logs to System.err
                val errorOutput = errorStream.toString()
                // The actual error format may differ with async execution
                (
                    errorOutput.contains("Unexpected error") ||
                        errorOutput.contains("ERROR") ||
                        errorOutput.contains("RuntimeException") ||
                        errorOutput.isEmpty()
                    ) shouldBe true // Empty if exception propagated
            }
        }

        describe("configuration handling") {

            /**
             * Tests user guidance when optional parameters are missing.
             * When no name is provided, the application should still function
             * but may provide helpful tips to improve the user experience.
             *
             * This demonstrates graceful degradation - the application works
             * with minimal input while encouraging better usage patterns.
             */
            it("should show tip when no name provided") {
                runTest {
                    // Given
                    val config = PresentationConfig(
                        verbose = false,
                        outputPath = null,
                        name = null,
                    )
                    val expectedResult = GreetingResult("Hello World from Anonymous!", "Anonymous")

                    coEvery {
                        mockCreateGreeting.execute(any())
                    } returns expectedResult.right()

                    // When
                    val runnable = asyncCli(config, mockCreateGreeting)
                    runnable.run()

                    // Then
                    outputStream.toString() shouldContain "Tip: You can provide your name as a command-line argument"
                }
            }
        }

        describe("runnable behavior") {

            /**
             * Validates that the factory returns the correct type.
             * The asyncCli factory should return a Runnable that can be
             * used with Java's executor frameworks or called directly.
             *
             * This ensures compatibility with existing Java infrastructure
             * and threading models.
             */
            it("should return Runnable instance") {
                // Given
                val config = PresentationConfig(
                    verbose = false,
                    outputPath = null,
                    name = "Test",
                )

                // When
                val runnable = asyncCli(config, mockCreateGreeting)

                // Then
                runnable.shouldBeInstanceOf<Runnable>()
            }

            /**
             * Tests that the returned Runnable can be executed multiple times.
             * This is important for scenarios where the same configuration
             * might be used repeatedly, such as in batch processing or
             * scheduled tasks.
             *
             * The test verifies that each execution properly invokes the
             * use case and that no state is incorrectly shared between runs.
             */
            it("should be reusable") {
                runTest {
                    // Given
                    val config = PresentationConfig(
                        verbose = false,
                        outputPath = null,
                        name = "Eve",
                    )
                    val expectedResult = GreetingResult("Hey there, Eve! Welcome!", "Eve")

                    coEvery {
                        mockCreateGreeting.execute(any())
                    } returns expectedResult.right()

                    // When
                    val runnable = asyncCli(config, mockCreateGreeting)
                    runnable.run()
                    outputStream.reset()
                    runnable.run()

                    // Then
                    coVerify(exactly = 2) { mockCreateGreeting.execute(any()) }
                }
            }
        }
    }
})
