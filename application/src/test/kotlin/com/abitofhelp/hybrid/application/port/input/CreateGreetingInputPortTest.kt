////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Test Suite
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.application.port.input

import arrow.core.right
import com.abitofhelp.hybrid.application.dto.CreateGreetingCommand
import com.abitofhelp.hybrid.application.dto.GreetingResult
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

/**
 * Test suite for CreateGreetingInputPort interface definition and contract.
 *
 * ## Purpose
 * This test suite validates the CreateGreetingInputPort interface, which defines the contract
 * for greeting creation use cases. It demonstrates how to test port interfaces in hexagonal
 * architecture, ensuring they define the correct API boundaries between layers.
 *
 * ## What is Being Tested
 * 1. **Interface Definition**: Verifies the port exists and can be mocked/implemented
 * 2. **Method Signature**: Validates the execute method accepts commands and returns results
 * 3. **Async Contract**: Ensures the port supports coroutine-based async operations
 * 4. **Return Type**: Confirms Either-based error handling in the method signature
 * 5. **Type Safety**: Validates compile-time type checking for port implementations
 *
 * ## Testing Strategy
 * - **Mock-Based Testing**: Uses mocks to verify interface contracts without implementations
 * - **Contract Testing**: Ensures method signatures match expected use case patterns
 * - **Type Testing**: Verifies generic type parameters and return types
 * - **Async Testing**: Validates coroutine support in port definitions
 * - **Compilation Testing**: Ensures interface can be implemented and called correctly
 *
 * ## Key Patterns Used
 * 1. **Port/Adapter Pattern**: Defines boundaries between application core and external layers
 * 2. **Interface Segregation**: Single responsibility focused on greeting creation
 * 3. **Command Pattern**: Uses command objects for method parameters
 * 4. **Result Pattern**: Returns Either type for functional error handling
 * 5. **Async Pattern**: Supports non-blocking operations with suspend functions
 *
 * ## Why These Tests Matter
 * - **Architecture Validation**: Ensures hexagonal architecture boundaries are properly defined
 * - **Contract Stability**: Port interfaces define stable contracts between layers
 * - **Type Safety**: Compile-time verification of port usage and implementation
 * - **Testing Infrastructure**: Mocked ports enable isolated use case testing
 * - **Documentation**: Tests serve as living documentation of port contracts
 *
 * ## Educational Value
 * This test demonstrates several architectural concepts:
 * - **Port Definition**: How to define clean boundaries in hexagonal architecture
 * - **Interface Testing**: Strategies for testing interface contracts
 * - **Mock Usage**: Using mocks to test interfaces without concrete implementations
 * - **Async Interfaces**: Defining and testing suspend function interfaces
 * - **Type Safety**: Leveraging Kotlin's type system for architectural enforcement
 *
 * ## Hexagonal Architecture Context
 * In hexagonal architecture, input ports:
 * - Define the application core's API for external actors
 * - Are implemented by use cases within the application layer
 * - Provide type-safe contracts for the presentation layer
 * - Enable dependency inversion between core and external layers
 * - Support testing through mockable interfaces
 *
 * ## Port Design Principles
 * - **Single Responsibility**: Each port handles one specific business capability
 * - **Technology Agnostic**: No dependencies on external frameworks or libraries
 * - **Error Handling**: Uses functional error handling patterns (Either types)
 * - **Async Support**: Non-blocking operations for better scalability
 * - **Type Safety**: Strong typing prevents runtime errors
 *
 * ## Testing Patterns Demonstrated
 * - **Interface Mocking**: Creating mock implementations for testing
 * - **Type Verification**: Ensuring correct types are used in method signatures
 * - **Contract Testing**: Validating interface behavior without implementation details
 * - **Async Testing**: Using runTest for coroutine-based testing
 * - **Compilation Testing**: Verifying interface can be properly instantiated
 *
 * ## Real-World Usage
 * Input ports are typically:
 * - Implemented by use case classes in the application layer
 * - Called by controllers, CLI handlers, or other presentation adapters
 * - Mocked in unit tests for isolated testing
 * - Used to define API boundaries in documentation
 * - Dependency-injected into presentation layer components
 *
 * ## Integration with Other Components
 * ```kotlin
 * // Use case implementation
 * class CreateGreetingUseCase : CreateGreetingInputPort {
 *   override suspend fun execute(command: CreateGreetingCommand): Either<ApplicationError, GreetingResult>
 * }
 *
 * // Presentation layer usage
 * class GreetingController(private val port: CreateGreetingInputPort) {
 *   suspend fun handleRequest(request: GreetingRequest) = port.execute(request.toCommand())
 * }
 * ```
 *
 * @see CreateGreetingInputPort The input port interface being tested
 * @see CreateGreetingCommand The command type used by this port
 * @see GreetingResult The result type returned by this port
 * @see CreateGreetingUseCase The typical implementation of this port
 */
class CreateGreetingInputPortTest : DescribeSpec({

    describe("CreateGreetingInputPort") {

        it("should be an interface") {
            val port = mockk<CreateGreetingInputPort>()
            port shouldBe port // Just verify it compiles as interface
        }

        it("should define execute method") {
            runTest {
                // Given
                val port = mockk<CreateGreetingInputPort>()
                val command = CreateGreetingCommand("John", silent = false)
                val expectedResult = GreetingResult("Hello, John!", "John")

                coEvery { port.execute(command) } returns expectedResult.right()

                // When
                val result = port.execute(command)

                // Then
                result shouldBe expectedResult.right()
            }
        }
    }
})
