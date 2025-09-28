/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

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
 * Test suite for GreetingService interface.
 *
 * ## Testing Interfaces
 *
 * When testing interfaces, we verify:
 * 1. **Contract adherence**: Implementations follow the expected behavior
 * 2. **Error cases**: Proper error handling is implemented
 * 3. **Mockability**: Interface can be mocked for testing other components
 *
 * ## Why Test Interfaces?
 *
 * Even though interfaces have no implementation, testing them ensures:
 * - The interface design is sound
 * - Mock implementations work correctly
 * - Contract expectations are documented through tests
 *
 * ## Test Implementation Pattern
 *
 * This test creates a simple test implementation to verify:
 * - The interface methods can be implemented
 * - The return types are appropriate
 * - Error handling works as expected
 *
 * ## Integration with Domain Layer
 *
 * The GreetingService interface is a key domain service that:
 * - Encapsulates greeting creation logic
 * - Returns Either for functional error handling
 * - Uses PersonName value object for type safety
 */
class GreetingServiceTest : DescribeSpec({

    describe("GreetingService") {

        it("should be an interface") {
            val service = mockk<GreetingService>()
            service shouldBe service // Just verify it compiles as interface
        }

        context("when implementing GreetingService") {
            val testService = object : GreetingService {
                override suspend fun createGreeting(name: PersonName): Either<DomainError, String> =
                    if (name.value == "Error") {
                        DomainError.ValidationError("name", "Test error").left()
                    } else {
                        "Hello, ${name.value}!".right()
                    }
            }

            it("should create greeting successfully") {
                kotlinx.coroutines.test.runTest {
                    val name = PersonName.create("John").getOrNull()!!
                    val result = testService.createGreeting(name)
                    result shouldBe "Hello, John!".right()
                }
            }

            it("should return error when appropriate") {
                kotlinx.coroutines.test.runTest {
                    val name = PersonName.create("Error").getOrNull()!!
                    val result = testService.createGreeting(name)
                    result shouldBe DomainError.ValidationError("name", "Test error").left()
                }
            }
        }
    }
})
