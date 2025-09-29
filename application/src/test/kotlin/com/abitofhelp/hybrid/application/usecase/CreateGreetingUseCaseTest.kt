////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Test Suite
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.application.usecase

import arrow.core.left
import arrow.core.right
import com.abitofhelp.hybrid.application.dto.CreateGreetingCommand
import com.abitofhelp.hybrid.application.dto.GreetingResult
import com.abitofhelp.hybrid.application.error.ApplicationError
import com.abitofhelp.hybrid.application.port.input.CreateGreetingInputPort
import com.abitofhelp.hybrid.application.port.output.OutputPort
import com.abitofhelp.hybrid.domain.error.DomainError
import com.abitofhelp.hybrid.domain.service.GreetingService
import com.abitofhelp.hybrid.domain.value.PersonName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

/**
 * Test suite for CreateGreetingUseCase.
 *
 * ## Testing Use Cases
 *
 * Use case tests focus on:
 * 1. **Orchestration logic**: Verify the correct sequence of operations
 * 2. **Error handling**: Each failure point should be handled appropriately
 * 3. **Business flow**: The use case coordinates domain and infrastructure correctly
 * 4. **Edge cases**: Null inputs, empty values, special flags
 *
 * ## Mocking Strategy
 *
 * This test uses MockK to:
 * - Mock domain services (GreetingService)
 * - Mock output ports (OutputPort)
 * - Verify interactions between components
 * - Test error scenarios without real implementations
 *
 * ## Test Organization
 *
 * Tests are organized by scenario:
 * - **Success paths**: All components work correctly
 * - **Domain failures**: Business rule violations
 * - **Infrastructure failures**: Output port errors
 * - **Edge cases**: Silent mode, anonymous users
 *
 * ## Key Testing Patterns
 *
 * ### Given-When-Then Pattern
 * ```kotlin
 * // Given - setup test data and mocks
 * val command = CreateGreetingCommand(...)
 * coEvery { mock.method() } returns expected
 *
 * // When - execute the use case
 * val result = useCase.execute(command)
 *
 * // Then - verify results and interactions
 * result shouldBe expected
 * coVerify { mock.method() }
 * ```
 *
 * ### Testing Either Results
 * - Use `shouldBe` with `.right()` or `.left()` for exact matching
 * - Use `isRight()` or `isLeft()` for boolean checks
 * - Use `getOrNull()` for safe value extraction in tests
 *
 * ## Integration Points
 *
 * This use case integrates:
 * - **Domain layer**: PersonName validation, GreetingService
 * - **Application layer**: Command DTOs, error wrapping
 * - **Infrastructure layer**: OutputPort interface
 */
class CreateGreetingUseCaseTest : DescribeSpec({

    describe("CreateGreetingUseCase") {

        val mockGreetingService = mockk<GreetingService>()
        val mockOutputPort = mockk<OutputPort>()
        val useCase = CreateGreetingUseCase(mockGreetingService, mockOutputPort)

        beforeEach {
            // Reset mocks before each test
            io.mockk.clearMocks(mockGreetingService, mockOutputPort)
        }

        describe("execute") {

            context("when all operations succeed") {
                it("should create greeting and send to output") {
                    runTest {
                        // Given
                        val command = CreateGreetingCommand(name = "John", silent = false)
                        val personName = PersonName.create("John").getOrNull()!!
                        val greeting = "Hey there, John! Welcome!"

                        coEvery { mockGreetingService.createGreeting(personName) } returns greeting.right()
                        coEvery { mockOutputPort.send(greeting) } returns Unit.right()

                        // When
                        val result = useCase.execute(command)

                        // Then
                        result shouldBe GreetingResult(greeting, "John").right()
                        coVerify { mockGreetingService.createGreeting(personName) }
                        coVerify { mockOutputPort.send(greeting) }
                    }
                }

                it("should handle different names") {
                    runTest {
                        // Given
                        val command = CreateGreetingCommand(name = "Jane Doe", silent = false)
                        val personName = PersonName.create("Jane Doe").getOrNull()!!
                        val greeting = "Hey there, Jane Doe! Welcome!"

                        coEvery { mockGreetingService.createGreeting(personName) } returns greeting.right()
                        coEvery { mockOutputPort.send(greeting) } returns Unit.right()

                        // When
                        val result = useCase.execute(command)

                        // Then
                        result shouldBe GreetingResult(greeting, "Jane Doe").right()
                    }
                }
            }

            context("when domain validation fails") {
                it("should handle empty name as anonymous") {
                    runTest {
                        // Given
                        val command = CreateGreetingCommand(name = "", silent = false)
                        val greeting = "Hello World from Anonymous!"

                        coEvery { mockGreetingService.createGreeting(PersonName.anonymous()) } returns greeting.right()
                        coEvery { mockOutputPort.send(greeting) } returns Unit.right()

                        // When
                        val result = useCase.execute(command)

                        // Then
                        result shouldBe GreetingResult(greeting, "Anonymous").right()
                    }
                }

                it("should handle null name as anonymous") {
                    runTest {
                        // Given
                        val command = CreateGreetingCommand(name = null, silent = false)
                        val greeting = "Hello World from Anonymous!"

                        coEvery { mockGreetingService.createGreeting(PersonName.anonymous()) } returns greeting.right()
                        coEvery { mockOutputPort.send(greeting) } returns Unit.right()

                        // When
                        val result = useCase.execute(command)

                        // Then
                        result shouldBe GreetingResult(greeting, "Anonymous").right()
                    }
                }

                it("should handle blank name as anonymous") {
                    runTest {
                        // Given
                        val command = CreateGreetingCommand(name = "   ", silent = false)
                        val greeting = "Hello World from Anonymous!"

                        coEvery { mockGreetingService.createGreeting(PersonName.anonymous()) } returns greeting.right()
                        coEvery { mockOutputPort.send(greeting) } returns Unit.right()

                        // When
                        val result = useCase.execute(command)

                        // Then
                        result shouldBe GreetingResult(greeting, "Anonymous").right()
                    }
                }
            }

            context("when greeting service returns domain error") {
                it("should wrap domain validation error") {
                    runTest {
                        // Given
                        val command = CreateGreetingCommand(name = "ValidName", silent = false)
                        val personName = PersonName.create("ValidName").getOrNull()!!
                        val domainError = DomainError.ValidationError("greeting", "Test validation error")

                        coEvery { mockGreetingService.createGreeting(personName) } returns domainError.left()

                        // When
                        val result = useCase.execute(command)

                        // Then
                        result.isLeft() shouldBe true
                        val error = result.leftOrNull()!!
                        error.shouldBeInstanceOf<ApplicationError.DomainErrorWrapper>()
                        error.domainError shouldBe domainError
                        error.userMessage shouldBe "greeting: Test validation error"
                    }
                }

                it("should wrap domain business rule violation") {
                    runTest {
                        // Given
                        val command = CreateGreetingCommand(name = "ValidName", silent = false)
                        val personName = PersonName.create("ValidName").getOrNull()!!
                        val domainError = DomainError.BusinessRuleViolation(
                            "GreetingPolicy",
                            "Too many greetings today",
                        )

                        coEvery { mockGreetingService.createGreeting(personName) } returns domainError.left()

                        // When
                        val result = useCase.execute(command)

                        // Then
                        result.isLeft() shouldBe true
                        val error = result.leftOrNull()!!
                        error.shouldBeInstanceOf<ApplicationError.DomainErrorWrapper>()
                        error.domainError shouldBe domainError
                        error.userMessage shouldBe "GreetingPolicy - Too many greetings today"
                    }
                }

                it("should wrap domain not found error") {
                    runTest {
                        // Given
                        val command = CreateGreetingCommand(name = "ValidName", silent = false)
                        val personName = PersonName.create("ValidName").getOrNull()!!
                        val domainError = DomainError.NotFound("Template", "default")

                        coEvery { mockGreetingService.createGreeting(personName) } returns domainError.left()

                        // When
                        val result = useCase.execute(command)

                        // Then
                        result.isLeft() shouldBe true
                        val error = result.leftOrNull()!!
                        error.shouldBeInstanceOf<ApplicationError.DomainErrorWrapper>()
                        error.domainError shouldBe domainError
                        error.userMessage shouldBe "Template with id 'default' not found"
                    }
                }
            }

            context("when output port fails") {
                it("should return output port error") {
                    runTest {
                        // Given
                        val command = CreateGreetingCommand(name = "John", silent = false)
                        val personName = PersonName.create("John").getOrNull()!!
                        val greeting = "Hey there, John! Welcome!"
                        val outputError = ApplicationError.OutputError("Output failed")

                        coEvery { mockGreetingService.createGreeting(personName) } returns greeting.right()
                        coEvery { mockOutputPort.send(greeting) } returns outputError.left()

                        // When
                        val result = useCase.execute(command)

                        // Then
                        result shouldBe outputError.left()
                        coVerify { mockGreetingService.createGreeting(personName) }
                        coVerify { mockOutputPort.send(greeting) }
                    }
                }

                it("should skip output when silent flag is true") {
                    runTest {
                        // Given
                        val command = CreateGreetingCommand(name = "John", silent = true)
                        val personName = PersonName.create("John").getOrNull()!!
                        val greeting = "Hey there, John! Welcome!"

                        coEvery { mockGreetingService.createGreeting(personName) } returns greeting.right()

                        // When
                        val result = useCase.execute(command)

                        // Then
                        result shouldBe GreetingResult(greeting, "John").right()
                        coVerify { mockGreetingService.createGreeting(personName) }
                        coVerify(exactly = 0) { mockOutputPort.send(any()) }
                    }
                }
            }
        }

        describe("interface implementation") {
            it("should implement CreateGreetingInputPort") {
                useCase.shouldBeInstanceOf<CreateGreetingInputPort>()
            }
        }
    }
})
