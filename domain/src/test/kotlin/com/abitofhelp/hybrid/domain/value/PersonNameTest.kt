// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Test Suite
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.domain.value

import arrow.core.left
import com.abitofhelp.hybrid.domain.error.DomainError
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for the PersonName value object.
 *
 * ## Purpose
 * This test suite validates the PersonName value object, which encapsulates name validation
 * and business rules in our domain. Value objects are fundamental building blocks in
 * Domain-Driven Design, ensuring data integrity at the domain level.
 *
 * ## What is Being Tested
 * 1. **Validation Rules**: All name validation constraints (length, characters, format)
 * 2. **Factory Methods**: Both create() and anonymous() factory methods
 * 3. **Edge Cases**: Boundary conditions like minimum/maximum lengths
 * 4. **Input Sanitization**: Whitespace trimming and normalization
 * 5. **Value Class Semantics**: Equality, hashCode, and toString behavior
 *
 * ## Why These Tests Matter
 * Value objects are the foundation of a robust domain model:
 * - **Data Integrity**: Invalid data never enters the system
 * - **Business Rules**: Name constraints are enforced consistently
 * - **Type Safety**: PersonName can't be confused with raw strings
 * - **Self-Documentation**: Tests document all valid/invalid name formats
 * - **Refactoring Safety**: Changes to validation logic are caught immediately
 *
 * ## Testing Strategy
 * - **Positive Testing**: Verify all valid name formats are accepted
 * - **Negative Testing**: Ensure all invalid inputs are rejected with proper errors
 * - **Boundary Testing**: Test exact limits (1 char minimum, 100 char maximum)
 * - **Equivalence Partitioning**: Group similar test cases (valid chars, invalid chars)
 * - **Property Testing**: Verify value object properties (immutability, equality)
 *
 * ## Key Patterns Used
 * 1. **Value Object Pattern**: Immutable objects with validation
 * 2. **Factory Pattern**: Static factory methods instead of constructors
 * 3. **Smart Constructor**: Validation happens during creation
 * 4. **Functional Error Handling**: Using Either instead of exceptions
 * 5. **Type-Safe Domain Modeling**: PersonName instead of String
 *
 * ## Educational Value
 * This test demonstrates several important concepts:
 * - **Value Object Testing**: How to thoroughly test validation and behavior
 * - **Factory Method Testing**: Testing static creation methods
 * - **Error-First Design**: Testing error cases is as important as success cases
 * - **Domain Invariants**: Rules that must always be true (e.g., no empty names)
 * - **Test as Specification**: Tests define what constitutes a valid name
 *
 * ## Business Rules Encoded
 * The tests document these business requirements:
 * 1. Names must be 1-100 characters after trimming
 * 2. Only letters, spaces, hyphens, and apostrophes allowed
 * 3. Leading/trailing whitespace is automatically removed
 * 4. "Anonymous" is a special case with its own factory method
 * 5. Empty or blank names are rejected
 *
 * ## Common Pitfalls to Avoid
 * - Don't test implementation details (e.g., regex patterns)
 * - Don't skip edge cases - they often reveal bugs
 * - Don't assume trimming - test it explicitly
 * - Don't forget special characters used in real names
 * - Don't test value objects as if they were entities
 *
 * ## Integration Notes
 * PersonName is used throughout the domain:
 * - GreetingPolicy uses name length for format selection
 * - GreetingService accepts PersonName for type safety
 * - Commands and queries use PersonName for validation
 *
 * @see PersonName The value object being tested
 * @see DomainError.ValidationError The error type returned for invalid names
 * @see GreetingPolicy How PersonName is used in business logic
 */
class PersonNameTest : DescribeSpec({

    describe("PersonName.create") {

        /**
         * Tests for valid PersonName creation.
         * Each test verifies one aspect of valid input.
         */
        context("when creating valid PersonName") {
            it("should create PersonName with valid name") {
                // Given: A simple, valid name
                // When: Creating a PersonName
                val result = PersonName.create("John")

                // Then: Should succeed with the exact value
                result.isRight() shouldBe true
                result.getOrNull()?.value shouldBe "John"
            }

            it("should create PersonName with minimum length name") {
                // Testing boundary condition: shortest valid name
                val result = PersonName.create("J")
                result.isRight() shouldBe true
                result.getOrNull()?.value shouldBe "J"
            }

            it("should create PersonName with maximum length name") {
                // Testing boundary condition: longest valid name (100 chars)
                val name = "A".repeat(100)
                val result = PersonName.create(name)
                result.isRight() shouldBe true
                result.getOrNull()?.value shouldBe name
            }

            it("should trim whitespace from name") {
                val result = PersonName.create("  John  ")
                result.isRight() shouldBe true
                result.getOrNull()?.value shouldBe "John"
            }

            it("should accept names with spaces") {
                val result = PersonName.create("John Doe")
                result.isRight() shouldBe true
                result.getOrNull()?.value shouldBe "John Doe"
            }

            it("should accept names with hyphens") {
                val result = PersonName.create("Mary-Jane")
                result.isRight() shouldBe true
                result.getOrNull()?.value shouldBe "Mary-Jane"
            }

            it("should accept names with apostrophes") {
                val result = PersonName.create("O'Brien")
                result.isRight() shouldBe true
                result.getOrNull()?.value shouldBe "O'Brien"
            }
        }

        /**
         * Tests for validation failures.
         * Each test verifies one validation rule.
         */
        context("when creating invalid PersonName") {
            it("should fail with empty name") {
                // Empty strings should be rejected
                val result = PersonName.create("")

                // Verify exact error for precise error handling
                result shouldBe DomainError.ValidationError(
                    field = "name",
                    message = "Name cannot be blank",
                ).left()
            }

            it("should fail with blank name") {
                val result = PersonName.create("   ")
                result shouldBe DomainError.ValidationError(
                    field = "name",
                    message = "Name cannot be blank",
                ).left()
            }

            it("should fail with name too long") {
                // Testing boundary: one character over the limit
                val name = "A".repeat(101)
                val result = PersonName.create(name)

                // Specific error for length violations
                result shouldBe DomainError.ValidationError(
                    field = "name",
                    message = "Name must be between 1 and 100 characters",
                ).left()
            }

            it("should fail with invalid characters") {
                val result = PersonName.create("John123")
                result shouldBe DomainError.ValidationError(
                    field = "name",
                    message = "Name can only contain letters, spaces, hyphens, and apostrophes",
                ).left()
            }

            it("should fail with special characters") {
                val result = PersonName.create("John@Doe")
                result shouldBe DomainError.ValidationError(
                    field = "name",
                    message = "Name can only contain letters, spaces, hyphens, and apostrophes",
                ).left()
            }
        }
    }

    describe("PersonName.anonymous") {
        /**
         * Tests the factory method for anonymous users.
         * This bypasses validation since "Anonymous" is always valid.
         */
        it("should create anonymous PersonName") {
            val anonymous = PersonName.anonymous()
            anonymous.value shouldBe "Anonymous"
        }
    }

    describe("PersonName value class") {
        /**
         * Tests for value class behavior.
         * Value classes in Kotlin should behave like the wrapped value
         * for equals, hashCode, and toString.
         */

        it("should provide toString") {
            // Useful for debugging and logging
            val name = PersonName.anonymous()
            name.toString() shouldBe "PersonName(value=Anonymous)"
        }

        it("should compare equal for same value") {
            // Value objects with same data should be equal
            val name1 = PersonName.anonymous()
            val name2 = PersonName.anonymous()
            (name1 == name2) shouldBe true
        }

        it("should generate same hashCode for same value") {
            // Required for using in HashMaps and Sets
            val name1 = PersonName.anonymous()
            val name2 = PersonName.anonymous()
            name1.hashCode() shouldBe name2.hashCode()
        }
    }
})
