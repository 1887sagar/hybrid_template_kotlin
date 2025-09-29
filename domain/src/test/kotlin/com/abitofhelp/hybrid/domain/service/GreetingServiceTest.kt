// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Test Suite
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.abitofhelp.hybrid.domain.error.DomainError
import com.abitofhelp.hybrid.domain.value.PersonName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

/**
 * Unit tests for the GreetingService interface.
 *
 * ## Purpose
 * This test suite validates the contract and design of the GreetingService interface,
 * which defines how greetings are created in our domain. Testing interfaces ensures
 * they can be implemented correctly and provides documentation of expected behavior.
 *
 * ## What is Being Tested
 * 1. **Interface Contract**: Verifies the interface can be implemented
 * 2. **Return Type Design**: Validates the use of Either for error handling
 * 3. **Method Signatures**: Ensures methods accept correct types (PersonName)
 * 4. **Mockability**: Confirms the interface can be mocked for testing
 * 5. **Suspend Function Support**: Tests async/coroutine compatibility
 *
 * ## Why Test Interfaces?
 * Testing interfaces might seem counterintuitive since they have no implementation,
 * but it serves several important purposes:
 * - **Design Validation**: Ensures the interface is well-designed and implementable
 * - **Contract Documentation**: Tests serve as executable specifications
 * - **Type Safety**: Verifies correct use of domain types (PersonName, DomainError)
 * - **Mock Verification**: Ensures the interface works with mocking frameworks
 * - **Evolution Safety**: Changes to interfaces are caught early
 *
 * ## Testing Strategy
 * - **Test Implementation**: Create a minimal implementation to verify the contract
 * - **Success Path Testing**: Verify normal greeting creation works
 * - **Error Path Testing**: Ensure error handling is possible
 * - **Coroutine Testing**: Use runTest to verify suspend functions work correctly
 *
 * ## Key Patterns Used
 * 1. **Interface Segregation**: Small, focused interface with single responsibility
 * 2. **Functional Error Handling**: Using Arrow's Either instead of exceptions
 * 3. **Value Objects**: Using PersonName instead of raw strings
 * 4. **Dependency Inversion**: Interface in domain, implementations in infrastructure
 * 5. **Coroutine Support**: Async operations with suspend functions
 *
 * ## Educational Value
 * This test demonstrates several advanced concepts:
 * - **Interface Design**: How to create testable, mockable interfaces
 * - **Functional Programming**: Using Either for explicit error handling
 * - **Domain-Driven Design**: Interfaces that speak the domain language
 * - **Test Doubles**: Creating test implementations vs using mocks
 * - **Coroutine Testing**: Testing suspend functions with runTest
 *
 * ## Implementation Guidelines
 * When implementing this interface:
 * 1. Return Right(greeting) for successful cases
 * 2. Return Left(DomainError) for failures
 * 3. Never throw exceptions - use Either for all error cases
 * 4. Validate inputs and apply business rules
 * 5. Keep implementations pure and side-effect free
 *
 * ## Example Implementation
 * ```kotlin
 * class DefaultGreetingService : GreetingService {
 *     override suspend fun createGreeting(name: PersonName): Either<DomainError, String> {
 *         return if (name.value.length > 100) {
 *             DomainError.ValidationError("name", "Name too long").left()
 *         } else {
 *             "Hello, ${name.value}!".right()
 *         }
 *     }
 * }
 * ```
 *
 * @see GreetingService The interface being tested
 * @see PersonName The value object used for type-safe names
 * @see DomainError The error types that can be returned
 * @see Either Arrow's functional error handling type
 */
class GreetingServiceTest : DescribeSpec({

    describe("GreetingService") {

        context("interface characteristics") {
            it("should be mockable with MockK") {
                // This test verifies that our interface design works well with
                // mocking frameworks, which is crucial for unit testing components
                // that depend on GreetingService
                val service = mockk<GreetingService>()
                service shouldBe service // Verifies successful mock creation
            }
        }

        context("when implementing GreetingService") {
            // Create a test implementation that demonstrates the interface contract
            // This implementation shows both success and error paths
            val testService = object : GreetingService {
                override suspend fun createGreeting(name: PersonName): Either<DomainError, String> =
                    if (name.value == "Error") {
                        // Demonstrate error handling path
                        DomainError.ValidationError("name", "Test error").left()
                    } else {
                        // Demonstrate success path with simple greeting
                        "Hello, ${name.value}!".right()
                    }
            }

            context("successful greeting creation") {
                it("should create greeting and return Right") {
                    // This test demonstrates the happy path where:
                    // 1. A valid PersonName is provided
                    // 2. The service creates a greeting successfully
                    // 3. The result is wrapped in Either.Right
                    kotlinx.coroutines.test.runTest {
                        // Given: A valid person name
                        val name = PersonName.create("John").getOrNull()!!

                        // When: Creating a greeting
                        val result = testService.createGreeting(name)

                        // Then: Should return Right with greeting
                        result shouldBe "Hello, John!".right()
                    }
                }

                it("should handle complex names correctly") {
                    // Test that implementations can handle various name formats
                    kotlinx.coroutines.test.runTest {
                        val complexName = PersonName.create("Mary-Jane O'Brien").getOrNull()!!
                        val result = testService.createGreeting(complexName)
                        result shouldBe "Hello, Mary-Jane O'Brien!".right()
                    }
                }
            }

            context("error handling") {
                it("should return Left with DomainError on failure") {
                    // This test demonstrates the error path where:
                    // 1. A specific input triggers an error condition
                    // 2. The service returns a DomainError
                    // 3. The error is wrapped in Either.Left
                    // This pattern allows callers to handle errors functionally
                    kotlinx.coroutines.test.runTest {
                        // Given: A name that triggers error behavior
                        val errorTriggeringName = PersonName.create("Error").getOrNull()!!

                        // When: Creating a greeting with error-triggering input
                        val result = testService.createGreeting(errorTriggeringName)

                        // Then: Should return Left with specific error
                        result shouldBe DomainError.ValidationError("name", "Test error").left()
                    }
                }
            }

            context("coroutine support") {
                it("should work within coroutine context") {
                    // This test verifies that the suspend function works correctly
                    // in a coroutine context, which is important for async operations
                    kotlinx.coroutines.test.runTest {
                        // The fact that we can call createGreeting inside runTest
                        // confirms proper suspend function implementation
                        val name = PersonName.anonymous()
                        val result = testService.createGreeting(name)

                        // Verify we get a valid Either result
                        result.isRight() shouldBe true
                    }
                }
            }
        }
    }
})
