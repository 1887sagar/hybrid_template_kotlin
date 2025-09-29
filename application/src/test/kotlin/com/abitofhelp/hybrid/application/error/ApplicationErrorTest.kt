// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Test Suite
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.application.error

import com.abitofhelp.hybrid.domain.error.DomainError
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unit tests for the ApplicationError sealed class hierarchy.
 *
 * ## Purpose
 * This test suite validates the application layer's error handling mechanisms, including
 * how domain errors are wrapped and presented to outer layers. It demonstrates the
 * application layer's role in translating technical errors into user-friendly messages.
 *
 * ## What is Being Tested
 * 1. **Error Types**: Each ApplicationError subtype's construction and properties
 * 2. **Domain Error Wrapping**: How domain errors are wrapped with user-friendly messages
 * 3. **Message Formatting**: The userMessage property provides appropriate user feedback
 * 4. **Sealed Class Exhaustiveness**: All error types can be handled in when expressions
 *
 * ## Testing Strategy
 * - **Property Verification**: Each error type's properties are validated
 * - **Wrapper Testing**: Tests how DomainErrorWrapper translates different domain errors
 * - **Type Safety**: Verifies inheritance hierarchy using type assertions
 * - **Message Testing**: Ensures user messages are appropriately formatted
 *
 * ## Key Patterns Used
 * 1. **Error Wrapping Pattern**: Domain errors wrapped with additional context
 * 2. **Sealed Class Hierarchy**: Compile-time exhaustive error handling
 * 3. **Message Translation**: Technical errors translated to user-friendly messages
 * 4. **Layer Separation**: Application errors hide domain implementation details
 *
 * ## Why These Tests Matter
 * - **User Experience**: Ensures errors are presented clearly to users
 * - **Layer Boundaries**: Validates proper error translation between layers
 * - **Debugging Support**: toString() implementations aid in troubleshooting
 * - **API Stability**: Error contracts remain consistent for presentation layer
 * - **Error Handling**: Documents how different error scenarios should be handled
 *
 * ## Educational Value
 * This test demonstrates several architectural concepts:
 * - **Error Translation**: How to map domain errors to application errors
 * - **Layer Responsibilities**: Application layer's role in error presentation
 * - **User-Friendly Messages**: Converting technical errors for end users
 * - **Wrapper Pattern**: Enriching errors with additional context
 * - **Sealed Classes**: Using sealed hierarchies for exhaustive handling
 *
 * ## Design Insights
 * - **UseCaseError**: Represents failures in use case orchestration logic
 * - **OutputError**: Captures failures in presenting results to outer layers
 * - **DomainErrorWrapper**: Bridges domain and application error models
 * - **User Messages**: Each domain error type gets a specific message format
 *
 * ## Error Translation Examples
 * - ValidationError: "field: message" format highlights the problematic field
 * - BusinessRuleViolation: "rule - reason" format explains the violation
 * - NotFound: "Entity with id 'X' not found" clearly states what's missing
 *
 * ## Testing Best Practices
 * - Tests serve as documentation for error handling strategies
 * - Each error type is tested independently for maintainability
 * - User message formats are verified to ensure consistency
 * - Exhaustiveness tests prevent missing error cases in production
 *
 * @see ApplicationError The sealed class hierarchy being tested
 * @see DomainError The domain errors that get wrapped
 * @see CreateGreetingUseCase Example of where these errors are used
 */
class ApplicationErrorTest : DescribeSpec({

    describe("ApplicationError") {

        context("UseCaseError") {
            it("should create with use case name and cause") {
                val error = ApplicationError.UseCaseError("CreateOrder", "Insufficient inventory")
                error.useCase shouldBe "CreateOrder"
                error.cause shouldBe "Insufficient inventory"
            }

            it("should be an ApplicationError") {
                val error = ApplicationError.UseCaseError("CreateOrder", "reason")
                error.shouldBeInstanceOf<ApplicationError>()
            }

            it("should have correct toString") {
                val error = ApplicationError.UseCaseError("CreateOrder", "Insufficient inventory")
                error.toString() shouldBe "UseCaseError(useCase=CreateOrder, cause=Insufficient inventory)"
            }
        }

        context("OutputError") {
            it("should create with message") {
                val error = ApplicationError.OutputError("Failed to write to file")
                error.message shouldBe "Failed to write to file"
            }

            it("should be an ApplicationError") {
                val error = ApplicationError.OutputError("message")
                error.shouldBeInstanceOf<ApplicationError>()
            }

            it("should have correct toString") {
                val error = ApplicationError.OutputError("Connection lost")
                error.toString() shouldBe "OutputError(message=Connection lost)"
            }
        }

        context("DomainErrorWrapper") {
            it("should wrap ValidationError") {
                val domainError = DomainError.ValidationError("email", "Invalid format")
                val error = ApplicationError.DomainErrorWrapper(domainError)

                error.domainError shouldBe domainError
                error.userMessage shouldBe "email: Invalid format"
            }

            it("should wrap BusinessRuleViolation") {
                val domainError = DomainError.BusinessRuleViolation("OrderLimit", "Max 10 items allowed")
                val error = ApplicationError.DomainErrorWrapper(domainError)

                error.domainError shouldBe domainError
                error.userMessage shouldBe "OrderLimit - Max 10 items allowed"
            }

            it("should wrap NotFound") {
                val domainError = DomainError.NotFound("User", "123")
                val error = ApplicationError.DomainErrorWrapper(domainError)

                error.domainError shouldBe domainError
                error.userMessage shouldBe "User with id '123' not found"
            }

            it("should be an ApplicationError") {
                val domainError = DomainError.ValidationError("field", "message")
                val error = ApplicationError.DomainErrorWrapper(domainError)
                error.shouldBeInstanceOf<ApplicationError>()
            }

            it("should have correct toString") {
                val domainError = DomainError.ValidationError("name", "Too short")
                val error = ApplicationError.DomainErrorWrapper(domainError)
                error.toString() shouldBe "DomainErrorWrapper(domainError=ValidationError(" +
                    "field=name, message=Too short))"
            }
        }

        describe("sealed class behavior") {
            it("should handle all error types in when expression") {
                val errors = listOf<ApplicationError>(
                    ApplicationError.UseCaseError("Test", "Error"),
                    ApplicationError.OutputError("Error"),
                    ApplicationError.DomainErrorWrapper(DomainError.ValidationError("field", "error")),
                )

                errors.forEach { error ->
                    val message = when (error) {
                        is ApplicationError.UseCaseError -> "UseCase: ${error.useCase}"
                        is ApplicationError.OutputError -> "Output: ${error.message}"
                        is ApplicationError.DomainErrorWrapper -> "Domain: ${error.userMessage}"
                        is ApplicationError.ValidationError -> "Validation: ${error.field}"
                        is ApplicationError.BatchValidationError -> "Batch: ${error.item}"
                    }

                    message.isNotEmpty() shouldBe true
                }
            }
        }
    }
})
