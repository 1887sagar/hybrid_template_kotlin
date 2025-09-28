/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.domain.value

import arrow.core.left
import com.abitofhelp.hybrid.domain.error.DomainError
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

/**
 * Test suite for PersonName value object.
 *
 * ## Testing Value Objects
 * Value objects need thorough testing because they:
 * - Enforce business rules through validation
 * - Are used throughout the application
 * - Represent core domain concepts
 *
 * ## Test Organization with Kotest
 * This uses Kotest's DescribeSpec style:
 * - `describe`: Groups related tests (like a test class)
 * - `context`: Sets up a scenario (like "when valid")
 * - `it`: Individual test case (like @Test method)
 *
 * ## What We Test
 * 1. **Valid Cases**: All acceptable inputs
 * 2. **Invalid Cases**: All validation rules
 * 3. **Edge Cases**: Boundaries and special values
 * 4. **Value Class Behavior**: equals, hashCode, toString
 *
 * ## Testing Philosophy
 * - Test behavior, not implementation
 * - Use descriptive test names that explain the requirement
 * - Each test should verify one thing
 * - Tests should be independent
 *
 * ## Example Test Pattern
 * ```kotlin
 * it("should [expected behavior] when [condition]") {
 *     // Given (setup)
 *     val input = "test data"
 *
 *     // When (action)
 *     val result = PersonName.create(input)
 *
 *     // Then (assertion)
 *     result.isRight() shouldBe true
 * }
 * ```
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
