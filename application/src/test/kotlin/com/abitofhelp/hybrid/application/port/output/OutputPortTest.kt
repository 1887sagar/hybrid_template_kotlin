////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Test Suite
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.application.port.output

import arrow.core.left
import arrow.core.right
import com.abitofhelp.hybrid.application.error.ApplicationError
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

/**
 * Test suite for OutputPort interface definition and contract validation.
 *
 * ## Purpose
 * This test suite validates the OutputPort interface, which defines the contract for sending
 * data to external systems in hexagonal architecture. It demonstrates how to test output ports
 * that provide boundaries between the application core and infrastructure layer adapters.
 *
 * ## What is Being Tested
 * 1. **Interface Definition**: Verifies the port exists and can be mocked/implemented
 * 2. **Method Signature**: Validates the send method accepts messages and returns Either results
 * 3. **Error Handling**: Ensures both success and failure scenarios are properly typed
 * 4. **Async Contract**: Confirms the port supports coroutine-based async operations
 * 5. **Type Safety**: Validates compile-time type checking for port implementations
 *
 * ## Testing Strategy
 * - **Mock-Based Testing**: Uses mocks to verify interface contracts without concrete adapters
 * - **Contract Testing**: Ensures method signatures match expected output patterns
 * - **Error Scenario Testing**: Validates both success and failure return paths
 * - **Type Verification**: Confirms Either-based error handling semantics
 * - **Async Testing**: Validates coroutine support in port definitions
 *
 * ## Key Patterns Used
 * 1. **Port/Adapter Pattern**: Defines boundaries between application and infrastructure layers
 * 2. **Interface Segregation**: Focused single responsibility for output operations
 * 3. **Result Pattern**: Uses Either type for functional error handling
 * 4. **Async Pattern**: Non-blocking operations with suspend functions
 * 5. **Dependency Inversion**: Core depends on abstractions, not concrete implementations
 *
 * ## Why These Tests Matter
 * - **Architecture Validation**: Ensures hexagonal architecture output boundaries are correct
 * - **Contract Stability**: Output ports define stable contracts for infrastructure adapters
 * - **Error Handling**: Validates proper error propagation from infrastructure to application
 * - **Testing Infrastructure**: Mocked ports enable isolated use case testing
 * - **Implementation Guidance**: Tests document expected adapter behavior patterns
 *
 * ## Educational Value
 * This test demonstrates several architectural concepts:
 * - **Output Port Design**: How to define clean outbound boundaries in hexagonal architecture
 * - **Interface Testing**: Strategies for testing abstract contracts
 * - **Error Modeling**: Using Either types for explicit error handling
 * - **Mock Verification**: Testing interfaces through mock behavior verification
 * - **Async Boundaries**: Designing and testing async interfaces between layers
 *
 * ## Hexagonal Architecture Context
 * In hexagonal architecture, output ports:
 * - Define how the application core communicates with external systems
 * - Are implemented by adapters in the infrastructure layer
 * - Enable dependency inversion (core depends on abstractions, not implementations)
 * - Provide testable boundaries for isolation testing
 * - Abstract away infrastructure-specific details from business logic
 *
 * ## Port Design Principles
 * - **Technology Agnostic**: No dependencies on specific infrastructure technologies
 * - **Error Transparency**: Clear error propagation from infrastructure to application
 * - **Async Support**: Non-blocking operations for better scalability
 * - **Single Responsibility**: Each port handles one specific output concern
 * - **Testability**: Easy to mock for unit testing
 *
 * ## Testing Patterns Demonstrated
 * - **Interface Mocking**: Creating mock implementations for behavior verification
 * - **Success Path Testing**: Validating successful operation return values
 * - **Error Path Testing**: Ensuring error scenarios are properly modeled
 * - **Type Safety Testing**: Verifying correct Either type usage
 * - **Async Testing**: Using runTest for coroutine-based verification
 *
 * ## Real-World Implementations
 * Output ports are typically implemented by:
 * - Console output adapters for CLI applications
 * - File output adapters for logging and data persistence
 * - Network adapters for HTTP APIs or message queues
 * - Database adapters for data storage
 * - Email/notification service adapters
 *
 * ## Common Adapter Implementations
 * ```kotlin
 * // Console adapter
 * class ConsoleOutputAdapter : OutputPort {
 *   override suspend fun send(message: String) = Either.catch { println(message) }
 * }
 *
 * // File adapter
 * class FileOutputAdapter(private val file: File) : OutputPort {
 *   override suspend fun send(message: String) = Either.catch { file.appendText(message) }
 * }
 *
 * // HTTP adapter
 * class HttpOutputAdapter(private val client: HttpClient) : OutputPort {
 *   override suspend fun send(message: String) = Either.catch { client.post(message) }
 * }
 * ```
 *
 * ## Error Handling Patterns
 * Output ports typically handle these error categories:
 * - **Infrastructure Errors**: Network failures, file system errors, database connection issues
 * - **Capacity Errors**: Disk full, rate limiting, queue overflow
 * - **Authentication Errors**: Invalid credentials, expired tokens
 * - **Validation Errors**: Invalid message format, size limits exceeded
 *
 * @see OutputPort The output port interface being tested
 * @see ApplicationError The error types that can be returned by implementations
 * @see CreateGreetingUseCase Example use case that depends on this port
 */
class OutputPortTest : DescribeSpec({

    describe("OutputPort") {

        it("should be an interface") {
            val port = mockk<OutputPort>()
            port shouldBe port // Just verify it compiles as interface
        }

        it("should define send method returning success") {
            runTest {
                // Given
                val port = mockk<OutputPort>()
                val message = "Test message"

                coEvery { port.send(message) } returns Unit.right()

                // When
                val result = port.send(message)

                // Then
                result shouldBe Unit.right()
            }
        }

        it("should define send method returning error") {
            runTest {
                // Given
                val port = mockk<OutputPort>()
                val message = "Test message"
                val error = ApplicationError.OutputError("Failed to send")

                coEvery { port.send(message) } returns error.left()

                // When
                val result = port.send(message)

                // Then
                result shouldBe error.left()
            }
        }
    }
})
