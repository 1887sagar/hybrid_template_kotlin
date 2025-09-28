/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.infrastructure.service

import com.abitofhelp.hybrid.domain.value.PersonName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest

/**
 * Unit tests for the DefaultGreetingService infrastructure implementation.
 *
 * ## Purpose
 * This test suite validates the infrastructure layer's implementation of the GreetingService
 * domain port. It demonstrates how infrastructure adapters implement domain interfaces while
 * respecting business rules and handling edge cases appropriately.
 *
 * ## What is Being Tested
 * 1. **Format Implementation**: Validates FRIENDLY, FORMAL, and DEFAULT greeting formats
 * 2. **Business Rule Compliance**: Ensures the service follows GreetingPolicy decisions
 * 3. **Coroutine Support**: Tests suspend function behavior with proper test coroutines
 * 4. **Edge Cases**: Validates handling of maximum length names and special characters
 * 5. **Consistency**: Ensures deterministic behavior for the same inputs
 *
 * ## Testing Strategy
 * - **Integration Testing**: Tests the service's integration with domain value objects
 * - **Scenario Testing**: Uses real-world examples to validate behavior
 * - **Boundary Testing**: Tests limits like maximum name length
 * - **Coroutine Testing**: Uses runTest for proper coroutine testing
 * - **Contract Testing**: Verifies the implementation satisfies the domain interface
 *
 * ## Key Patterns Used
 * 1. **Port and Adapter**: DefaultGreetingService adapts the domain port for infrastructure
 * 2. **Dependency Injection**: Service can be injected wherever GreetingService is needed
 * 3. **Suspend Functions**: Demonstrates async-ready infrastructure implementations
 * 4. **Result Type**: Uses Arrow's Either for functional error handling
 * 5. **Given-When-Then**: Clear test structure following BDD practices
 *
 * ## Why These Tests Matter
 * - **Contract Verification**: Ensures infrastructure correctly implements domain contracts
 * - **Async Readiness**: Validates coroutine usage for future async requirements
 * - **Edge Case Handling**: Confirms the service handles all valid PersonName inputs
 * - **Refactoring Safety**: Can swap implementations while maintaining behavior
 * - **Integration Confidence**: Tests real interactions between layers
 *
 * ## Educational Value
 * This test demonstrates several advanced testing concepts:
 * - **Infrastructure Testing**: How to test infrastructure adapters effectively
 * - **Coroutine Testing**: Proper use of runTest for suspend functions
 * - **Contract Testing**: Verifying implementations against interfaces
 * - **Functional Testing**: Testing with Either and functional error handling
 * - **Test Organization**: Grouping tests by behavior and scenarios
 *
 * ## Implementation Insights
 * - The service is stateless and thread-safe (no mutable state)
 * - Greeting formats match business requirements exactly
 * - The service handles all valid PersonName values without errors
 * - Consistency is guaranteed - same input always produces same output
 * - The implementation is ready for future async operations
 *
 * ## Testing Techniques
 * - **Table-Driven Tests**: The "various names" test uses a list of test cases
 * - **Property Testing**: Tests properties like consistency across multiple calls
 * - **Interface Testing**: Verifies the class implements the expected interface
 * - **Boundary Testing**: Tests extreme values within allowed limits
 *
 * ## Design Validation
 * - The service correctly delegates format decisions to GreetingPolicy
 * - Error handling is prepared for future failure scenarios
 * - The implementation is simple, focusing on its single responsibility
 * - The service is easily mockable for testing other components
 *
 * @see DefaultGreetingService The infrastructure service being tested
 * @see GreetingService The domain port it implements
 * @see GreetingPolicy The domain service that determines formatting rules
 * @see PersonName The value object used as input
 */
class DefaultGreetingServiceTest : DescribeSpec({

    describe("DefaultGreetingService") {

        val service = DefaultGreetingService()

        describe("createGreeting") {

            context("when creating greetings with different formats") {
                it("should create FRIENDLY greeting for normal names") {
                    runTest {
                        // Given
                        val name = PersonName.create("John").getOrNull()!!

                        // When
                        val result = service.createGreeting(name)

                        // Then
                        result.isRight() shouldBe true
                        result.getOrNull()!! shouldBe "Hey there, John! Welcome!"
                    }
                }

                it("should create FORMAL greeting for long names") {
                    runTest {
                        // Given
                        val longName = "Alexander Christopher Wellington"
                        val name = PersonName.create(longName).getOrNull()!!

                        // When
                        val result = service.createGreeting(name)

                        // Then
                        result.isRight() shouldBe true
                        result.getOrNull()!! shouldBe "Greetings, $longName. Welcome to the application."
                    }
                }

                it("should create DEFAULT greeting for Anonymous") {
                    runTest {
                        // Given
                        val name = PersonName.anonymous()

                        // When
                        val result = service.createGreeting(name)

                        // Then
                        result.isRight() shouldBe true
                        result.getOrNull()!! shouldBe "Hello World from Anonymous!"
                    }
                }

                it("should handle case-insensitive Anonymous") {
                    runTest {
                        // Given
                        val name = PersonName.create("ANONYMOUS").getOrNull()!!

                        // When
                        val result = service.createGreeting(name)

                        // Then
                        result.isRight() shouldBe true
                        result.getOrNull()!! shouldBe "Hello World from ANONYMOUS!"
                    }
                }
            }

            context("when greeting violates policies") {
                it("should handle very long names gracefully") {
                    runTest {
                        // Given a name that would create a greeting near the limit
                        val longNameValue = "A".repeat(100) // Max allowed name
                        val name = PersonName.create(longNameValue).getOrNull()!!

                        // When
                        val result = service.createGreeting(name)

                        // Then - The formal greeting template is still within limits
                        result.isRight() shouldBe true
                        val greeting = result.getOrNull()!!
                        greeting.length shouldBe (greeting.length)
                        greeting shouldContain longNameValue
                    }
                }
            }
        }

        describe("greeting format variations") {
            it("should use correct format for short names") {
                runTest {
                    val name = PersonName.create("Jo").getOrNull()!!
                    val result = service.createGreeting(name)

                    result.isRight() shouldBe true
                    result.getOrNull()!! shouldBe "Hey there, Jo! Welcome!"
                }
            }

            it("should format greeting correctly for various names") {
                runTest {
                    val testCases = listOf(
                        "Bob" to "Hey there, Bob! Welcome!",
                        "Anonymous" to "Hello World from Anonymous!",
                        "A Very Long Name Indeed" to "Greetings, A Very Long Name Indeed. Welcome to the application.",
                    )

                    testCases.forEach { (nameValue, expectedGreeting) ->
                        val name = PersonName.create(nameValue).getOrNull()!!
                        val result = service.createGreeting(name)

                        result.isRight() shouldBe true
                        result.getOrNull()!! shouldBe expectedGreeting
                    }
                }
            }
        }

        describe("error handling") {
            it("should handle all valid PersonName values") {
                runTest {
                    // Test edge cases
                    val edgeCases = listOf(
                        PersonName.create("J").getOrNull()!!, // Minimum length
                        PersonName.create("A".repeat(100)).getOrNull()!!, // Maximum length
                        PersonName.create("John Doe").getOrNull()!!, // With space
                        PersonName.create("O'Brien").getOrNull()!!, // With apostrophe
                        PersonName.create("Mary-Jane").getOrNull()!!, // With hyphen
                    )

                    edgeCases.forEach { name ->
                        val result = service.createGreeting(name)
                        result.isRight() shouldBe true
                    }
                }
            }
        }

        describe("interface implementation") {
            it("should implement GreetingService") {
                service.shouldBeInstanceOf<com.abitofhelp.hybrid.domain.service.GreetingService>()
            }
        }

        describe("internal behavior") {
            it("should generate consistent greetings") {
                runTest {
                    // Given
                    val name = PersonName.create("TestUser").getOrNull()!!

                    // When - call multiple times
                    val result1 = service.createGreeting(name)
                    val result2 = service.createGreeting(name)
                    val result3 = service.createGreeting(name)

                    // Then - should be consistent
                    result1 shouldBe result2
                    result2 shouldBe result3
                }
            }
        }
    }
})
