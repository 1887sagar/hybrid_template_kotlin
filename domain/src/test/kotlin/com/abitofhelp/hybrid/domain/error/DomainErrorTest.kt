/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.domain.error

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unit tests for the DomainError sealed class hierarchy.
 *
 * ## Purpose
 * This test suite validates the behavior and structure of domain-specific errors in our
 * hybrid architecture. Domain errors represent business rule violations, validation failures,
 * and entity-not-found conditions that occur within the domain layer.
 *
 * ## What is Being Tested
 * 1. **Error Creation**: Validates that each error type correctly stores its properties
 * 2. **Type Safety**: Ensures the sealed class hierarchy provides compile-time exhaustiveness
 * 3. **Data Class Behavior**: Verifies toString() implementations for debugging
 * 4. **Inheritance**: Confirms all error types are part of the DomainError hierarchy
 *
 * ## Testing Strategy
 * - **Property-Based Testing**: Each error type's properties are verified after construction
 * - **Type Verification**: Uses Kotest's `shouldBeInstanceOf` to confirm inheritance
 * - **Exhaustiveness Testing**: Demonstrates sealed class pattern with when expressions
 * - **String Representation**: Validates toString() for effective error logging
 *
 * ## Key Patterns Used
 * 1. **Sealed Class Pattern**: DomainError is sealed, making error handling exhaustive
 * 2. **Data Classes**: Each error type is a data class for automatic equals/hashCode/toString
 * 3. **BDD-Style Testing**: Uses Kotest's DescribeSpec for readable test organization
 * 4. **Context Grouping**: Related tests are grouped by error type for clarity
 *
 * ## Why These Tests Matter
 * - **Type Safety**: Sealed classes ensure all error cases are handled at compile time
 * - **Debugging**: Proper toString() implementations make error logs more informative
 * - **Domain Integrity**: Well-tested errors ensure business rules are properly communicated
 * - **API Contract**: These tests document the expected error structure for API consumers
 * - **Refactoring Safety**: Tests ensure error contracts remain stable during refactoring
 *
 * ## Educational Value
 * This test demonstrates several important testing concepts:
 * - How to test sealed class hierarchies effectively
 * - The importance of testing data class implementations
 * - Using type verification to ensure proper inheritance
 * - Organizing tests by behavior rather than implementation
 * - Writing tests that serve as living documentation
 *
 * @see DomainError The sealed class being tested
 * @see DomainError.ValidationError For field-level validation failures
 * @see DomainError.BusinessRuleViolation For business rule violations
 * @see DomainError.NotFound For entity not found errors
 */
class DomainErrorTest : DescribeSpec({

    describe("DomainError") {

        context("ValidationError") {
            it("should create with field and message") {
                val error = DomainError.ValidationError("email", "Invalid email format")
                error.field shouldBe "email"
                error.message shouldBe "Invalid email format"
            }

            it("should be a DomainError") {
                val error = DomainError.ValidationError("field", "message")
                error.shouldBeInstanceOf<DomainError>()
            }

            it("should have correct toString") {
                val error = DomainError.ValidationError("email", "Invalid email format")
                error.toString() shouldBe "ValidationError(field=email, message=Invalid email format)"
            }
        }

        context("BusinessRuleViolation") {
            it("should create with rule and reason") {
                val error = DomainError.BusinessRuleViolation("MinimumOrderAmount", "Order must be at least $10")
                error.rule shouldBe "MinimumOrderAmount"
                error.reason shouldBe "Order must be at least $10"
            }

            it("should be a DomainError") {
                val error = DomainError.BusinessRuleViolation("rule", "reason")
                error.shouldBeInstanceOf<DomainError>()
            }

            it("should have correct toString") {
                val error = DomainError.BusinessRuleViolation("MinimumOrderAmount", "Order must be at least $10")
                error.toString() shouldBe "BusinessRuleViolation(rule=MinimumOrderAmount, " +
                    "reason=Order must be at least $10)"
            }
        }

        context("NotFound") {
            it("should create with entity and id") {
                val error = DomainError.NotFound("User", "123")
                error.entity shouldBe "User"
                error.id shouldBe "123"
            }

            it("should be a DomainError") {
                val error = DomainError.NotFound("entity", "id")
                error.shouldBeInstanceOf<DomainError>()
            }

            it("should have correct toString") {
                val error = DomainError.NotFound("User", "123")
                error.toString() shouldBe "NotFound(entity=User, id=123)"
            }
        }

        context("sealed class behavior") {
            it("should be exhaustive in when expression") {
                val errors: List<DomainError> = listOf(
                    DomainError.ValidationError("field", "message"),
                    DomainError.BusinessRuleViolation("rule", "reason"),
                    DomainError.NotFound("entity", "id"),
                )

                errors.forEach { error ->
                    val message = when (error) {
                        is DomainError.ValidationError -> "Validation: ${error.field}"
                        is DomainError.BusinessRuleViolation -> "Rule: ${error.rule}"
                        is DomainError.NotFound -> "NotFound: ${error.entity}"
                    }
                    message.isNotEmpty() shouldBe true
                }
            }
        }
    }
})
