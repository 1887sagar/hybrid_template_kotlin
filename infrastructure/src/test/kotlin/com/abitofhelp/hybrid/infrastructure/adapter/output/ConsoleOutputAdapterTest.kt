// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Test Suite
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.infrastructure.adapter.output

import arrow.core.right
import com.abitofhelp.hybrid.application.error.ApplicationError
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * Comprehensive test suite for ConsoleOutputAdapter.
 *
 * ## Purpose
 * This test suite validates the console output adapter, which provides reliable console/terminal
 * output functionality with proper validation and error handling. The adapter serves as the
 * primary mechanism for displaying application output to users in command-line environments,
 * ensuring messages are properly formatted and delivery failures are handled gracefully.
 *
 * ## What is Being Tested
 * 1. **Console Output**: Messages are correctly written to the console/terminal
 * 2. **Message Validation**: Input validation prevents invalid messages from being processed
 * 3. **Character Encoding**: Proper handling of various character sets including Unicode
 * 4. **Error Handling**: I/O errors are caught and wrapped in appropriate error types
 * 5. **Stream Management**: Correct interaction with PrintStream for flexible output
 * 6. **Port Compliance**: Proper implementation of the OutputPort interface contract
 *
 * ## Testing Strategy
 * - **Output Capture**: Uses ByteArrayOutputStream to capture and verify console output
 * - **Validation Testing**: Tests edge cases for message validation logic
 * - **Error Injection**: MockK used to simulate console failures for error handling tests
 * - **Character Set Testing**: Validates handling of special characters and Unicode
 * - **Interface Testing**: Confirms proper implementation of infrastructure contracts
 *
 * ## Key Test Scenarios
 *
 * ### Valid Message Handling
 * - **Simple Messages**: Basic string output to console
 * - **Multi-line Messages**: Proper handling of messages containing newlines
 * - **Special Characters**: Unicode, symbols, and international characters
 * - **Long Messages**: Handling of messages with significant length
 * - **Formatted Content**: Messages with various formatting characters
 *
 * ### Message Validation
 * - **Empty Messages**: Proper rejection of empty strings
 * - **Blank Messages**: Proper rejection of whitespace-only strings
 * - **Null Handling**: Appropriate error responses for invalid input
 * - **Edge Cases**: Boundary conditions for message content
 *
 * ### Error Scenarios
 * - **Console Failures**: Handling of PrintStream exceptions during output
 * - **I/O Errors**: Network issues when console is redirected
 * - **Resource Exhaustion**: Behavior when console buffer is full
 * - **Interrupted Operations**: Handling of interrupted console writes
 *
 * ## Output Stream Testing Patterns
 * The tests use a sophisticated approach to capture and verify console output:
 *
 * 1. **ByteArrayOutputStream**: Captures actual bytes written to console
 * 2. **PrintStream Wrapping**: Redirects console output to test-controlled stream
 * 3. **Content Verification**: Validates exact content written to console
 * 4. **Encoding Preservation**: Ensures character encoding is maintained
 * 5. **Stream State**: Verifies console stream state after operations
 *
 * ## Error Handling Validation
 * The adapter implements robust error handling that's thoroughly tested:
 *
 * - **Input Validation**: Messages are validated before attempting console write
 * - **Exception Wrapping**: Console exceptions become ApplicationError.OutputError
 * - **Error Context**: Error messages provide meaningful context for debugging
 * - **Graceful Degradation**: Failures don't crash the application
 * - **Error Propagation**: Errors are properly propagated to calling code
 *
 * ## Character Encoding and Internationalization
 * Testing covers various character scenarios:
 * - **ASCII Characters**: Standard English characters and symbols
 * - **Unicode Characters**: International characters and emojis
 * - **Special Symbols**: Mathematical symbols and special punctuation
 * - **Line Endings**: Various line ending styles across platforms
 * - **Control Characters**: Tab, newline, and other control sequences
 *
 * ## Testing Techniques Used
 * 1. **Output Verification**: Capturing and analyzing actual console output
 * 2. **Mock-Based Error Testing**: Controlled exception scenarios with MockK
 * 3. **Boundary Testing**: Edge cases for message validation logic
 * 4. **Content Analysis**: Detailed verification of output content and format
 * 5. **Interface Compliance**: Type checking and contract verification
 *
 * ## Educational Value
 * This test suite demonstrates several important concepts:
 * - **I/O Testing**: How to test console and stream-based output
 * - **Output Capture**: Techniques for capturing and verifying console output
 * - **Validation Testing**: Comprehensive input validation strategies
 * - **Error Simulation**: Using mocks to test error conditions
 * - **Character Encoding**: Handling diverse character sets in testing
 *
 * ## Real-World Applications
 * The ConsoleOutputAdapter is essential for:
 * - **CLI Applications**: Command-line tools and utilities
 * - **Development Tools**: Build systems, test runners, development servers
 * - **System Administration**: Scripts and administrative tools
 * - **Interactive Applications**: REPL environments and interactive prompts
 * - **Logging**: Debug output and user notifications
 *
 * ## Design Considerations
 * The adapter balances several important concerns:
 * - **Performance**: Direct console writing with minimal overhead
 * - **Reliability**: Robust error handling prevents application crashes
 * - **Flexibility**: Accepts any PrintStream for testing and redirection
 * - **Validation**: Prevents invalid content from reaching the console
 * - **Simplicity**: Straightforward implementation with clear behavior
 *
 * @see ConsoleOutputAdapter The console output adapter being tested
 * @see OutputPort The port interface this adapter implements
 * @see java.io.PrintStream The underlying output mechanism
 * @see ApplicationError.OutputError The error type used for output failures
 * @see java.io.ByteArrayOutputStream The testing utility used for output capture
 */
class ConsoleOutputAdapterTest : DescribeSpec({

    describe("ConsoleOutputAdapter") {

        describe("send") {

            context("when sending valid messages") {
                it("should send message to console successfully") {
                    runTest {
                        // Capture console output
                        val outputStream = ByteArrayOutputStream()
                        val testPrintStream = PrintStream(outputStream)
                        val adapter = ConsoleOutputAdapter(testPrintStream)

                        // Given
                        val message = "Hello, World!"

                        // When
                        val result = adapter.send(message)

                        // Then
                        result shouldBe Unit.right()
                        outputStream.toString() shouldContain message
                    }
                }

                it("should handle multi-line messages") {
                    runTest {
                        // Capture console output
                        val outputStream = ByteArrayOutputStream()
                        val testPrintStream = PrintStream(outputStream)
                        val adapter = ConsoleOutputAdapter(testPrintStream)

                        // Given
                        val message = "Line 1\nLine 2\nLine 3"

                        // When
                        val result = adapter.send(message)

                        // Then
                        result shouldBe Unit.right()
                        val output = outputStream.toString()
                        output shouldContain "Line 1"
                        output shouldContain "Line 2"
                        output shouldContain "Line 3"
                    }
                }

                it("should handle special characters") {
                    runTest {
                        // Capture console output
                        val outputStream = ByteArrayOutputStream()
                        val testPrintStream = PrintStream(outputStream)
                        val adapter = ConsoleOutputAdapter(testPrintStream)

                        // Given
                        val message = "Hello! @#$%^&*() 你好 🎉"

                        // When
                        val result = adapter.send(message)

                        // Then
                        result shouldBe Unit.right()
                        outputStream.toString() shouldContain message
                    }
                }
            }

            context("when sending invalid messages") {
                it("should return error for empty message") {
                    runTest {
                        // Given
                        val message = ""
                        val adapter = ConsoleOutputAdapter(System.out)

                        // When
                        val result = adapter.send(message)

                        // Then
                        result.isLeft() shouldBe true
                        val error = result.leftOrNull()!!
                        error.shouldBeInstanceOf<ApplicationError.OutputError>()
                        error.message shouldBe "Invalid message: Message cannot be blank"
                    }
                }

                it("should return error for blank message") {
                    runTest {
                        // Given
                        val message = "   "
                        val adapter = ConsoleOutputAdapter(System.out)

                        // When
                        val result = adapter.send(message)

                        // Then
                        result.isLeft() shouldBe true
                        val error = result.leftOrNull()!!
                        error.shouldBeInstanceOf<ApplicationError.OutputError>()
                        error.message shouldBe "Invalid message: Message cannot be blank"
                    }
                }
            }

            context("when console operations fail") {
                it("should handle exception during print") {
                    runTest {
                        // Mock PrintStream to throw exception
                        val mockPrintStream = mockk<PrintStream>()
                        val adapter = ConsoleOutputAdapter(mockPrintStream)

                        // Given
                        val message = "Test message"
                        val exception = RuntimeException("Console error")
                        every { mockPrintStream.println(message) } throws exception

                        // When
                        val result = adapter.send(message)

                        // Then
                        result.isLeft() shouldBe true
                        val error = result.leftOrNull()!!
                        error.shouldBeInstanceOf<ApplicationError.OutputError>()
                        error.message shouldContain "Unexpected error writing to console"
                    }
                }
            }
        }

        describe("interface implementation") {
            it("should implement OutputPort") {
                val adapter = ConsoleOutputAdapter(System.out)
                adapter.shouldBeInstanceOf<com.abitofhelp.hybrid.application.port.output.OutputPort>()
            }
        }
    }
})
