////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Test Suite
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.application.dto

import com.abitofhelp.hybrid.application.error.ApplicationError
import io.kotest.assertions.fail
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Comprehensive test suite for BatchGreetingCommand validation and batch processing functionality.
 *
 * ## Purpose
 * This test suite validates the BatchGreetingCommand, which handles bulk greeting operations
 * with concurrent processing capabilities. It demonstrates how to test complex DTOs that
 * include business rules, validation logic, and utility methods for batch operations.
 *
 * ## What is Being Tested
 * 1. **Batch Size Validation**: Enforces minimum (1) and maximum (1000) batch sizes
 * 2. **Duplicate Detection**: Prevents duplicate names within a single batch
 * 3. **Concurrency Limits**: Validates concurrent processing boundaries (1-100)
 * 4. **Template Validation**: Ensures greeting templates contain required placeholders
 * 5. **Conversion Methods**: Tests PersonName conversion and error handling
 * 6. **Utility Functions**: Batch chunking, size detection, and template application
 *
 * ## Testing Strategy
 * - **Boundary Testing**: Tests minimum, maximum, and invalid values for all limits
 * - **Error Path Coverage**: Every validation rule has corresponding failure tests
 * - **Functional Testing**: Utility methods are tested with realistic scenarios
 * - **Edge Case Handling**: Empty strings, null values, and whitespace scenarios
 * - **Business Rule Validation**: Each business constraint is individually verified
 *
 * ## Key Patterns Used
 * 1. **Command Pattern**: Encapsulates batch operation parameters and validation
 * 2. **Builder Pattern**: Uses defaults and copy semantics for flexible construction
 * 3. **Validation Pattern**: Comprehensive validation with detailed error messages
 * 4. **Batch Processing**: Chunking and concurrent processing utilities
 * 5. **Template Pattern**: Customizable greeting templates with placeholders
 *
 * ## Why These Tests Matter
 * - **Performance Boundaries**: Validates system limits prevent resource exhaustion
 * - **Data Integrity**: Duplicate detection ensures clean batch processing
 * - **Error Prevention**: Validation catches issues before expensive operations
 * - **Template Safety**: Ensures templates are well-formed before processing
 * - **Concurrency Control**: Prevents overwhelming downstream systems
 *
 * ## Educational Value
 * This test demonstrates several important concepts:
 * - **Complex DTO Testing**: How to test DTOs with embedded business logic
 * - **Validation Strategy**: Comprehensive validation with clear error messages
 * - **Batch Processing Patterns**: Chunking, concurrency, and template application
 * - **Business Rule Testing**: Each constraint tested independently
 * - **Error Message Quality**: User-friendly error messages for validation failures
 *
 * ## Business Rules Validated
 * - **Batch Size**: 1-1000 items to balance efficiency and resource usage
 * - **Uniqueness**: No duplicate names to prevent redundant processing
 * - **Concurrency**: 1-100 concurrent operations for optimal throughput
 * - **Templates**: Must contain {name} placeholder for proper substitution
 * - **Large Batch Threshold**: 100+ items trigger different processing strategies
 *
 * ## Testing Patterns Demonstrated
 * - **Given-When-Then**: Clear test structure for validation scenarios
 * - **Equivalence Partitioning**: Tests representative values from each category
 * - **Boundary Value Analysis**: Tests at the edges of valid ranges
 * - **Error Case Testing**: Validates each failure mode independently
 * - **Utility Method Testing**: Functional tests for helper methods
 *
 * ## Real-World Applications
 * This command pattern is commonly used in:
 * - Bulk email/notification systems
 * - Batch data processing pipelines
 * - Mass user operations (invitations, updates)
 * - Report generation for multiple recipients
 * - Any scenario requiring controlled batch processing
 *
 * @see BatchGreetingCommand The DTO being tested
 * @see CreateBatchGreetingsUseCase The use case that consumes this command
 * @see PersonName Domain value object for name validation
 */
class BatchGreetingCommandTest : DescribeSpec({

    describe("BatchGreetingCommand validation") {

        context("batch size validation") {

            it("should reject empty batch") {
                val command = BatchGreetingCommand(
                    names = emptyList(),
                )

                val result = command.validate()

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        error shouldBe ApplicationError.ValidationError(
                            field = "names",
                            message = "Batch must contain at least one name",
                        )
                    },
                    { fail("Expected Left but got Right") },
                )
            }

            it("should accept single name") {
                val command = BatchGreetingCommand(
                    names = listOf("Alice"),
                )

                val result = command.validate()

                result.isRight() shouldBe true
            }

            it("should reject oversized batch") {
                val names = (1..1001).map { "Name$it" }
                val command = BatchGreetingCommand(
                    names = names,
                    maxConcurrent = BatchGreetingCommand.MAX_CONCURRENT,
                )

                val result = command.validate()

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        error shouldBe ApplicationError.ValidationError(
                            field = "names",
                            message = "Batch size 1001 exceeds maximum of 1000",
                        )
                    },
                    { fail("Expected Left but got Right") },
                )
            }

            it("should accept maximum allowed batch size") {
                val names = (1..1000).map { "Name$it" }
                val command = BatchGreetingCommand(
                    names = names,
                    maxConcurrent = BatchGreetingCommand.MAX_CONCURRENT,
                )

                val result = command.validate()

                result.isRight() shouldBe true
            }
        }

        context("duplicate validation") {

            it("should reject duplicate names") {
                val command = BatchGreetingCommand(
                    names = listOf("Alice", "Bob", "Alice"),
                )

                val result = command.validate()

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        error shouldBe ApplicationError.ValidationError(
                            field = "names",
                            message = "Duplicate names found: Alice",
                        )
                    },
                    { fail("Expected Left but got Right") },
                )
            }

            it("should accept unique names") {
                val command = BatchGreetingCommand(
                    names = listOf("Alice", "Bob", "Charlie"),
                )

                val result = command.validate()

                result.isRight() shouldBe true
            }
        }

        context("concurrency validation") {

            it("should reject too low concurrency") {
                val command = BatchGreetingCommand(
                    names = listOf("Alice"),
                    maxConcurrent = 0,
                )

                val result = command.validate()

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        error shouldBe ApplicationError.ValidationError(
                            field = "maxConcurrent",
                            message = "Concurrent limit must be at least 1",
                        )
                    },
                    { fail("Expected Left but got Right") },
                )
            }

            it("should accept minimum concurrency") {
                val command = BatchGreetingCommand(
                    names = listOf("Alice"),
                    maxConcurrent = 1,
                )

                val result = command.validate()

                result.isRight() shouldBe true
            }

            it("should reject too high concurrency") {
                val command = BatchGreetingCommand(
                    names = listOf("Alice"),
                    maxConcurrent = 101,
                )

                val result = command.validate()

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        error shouldBe ApplicationError.ValidationError(
                            field = "maxConcurrent",
                            message = "Concurrent limit cannot exceed 100",
                        )
                    },
                    { fail("Expected Left but got Right") },
                )
            }

            it("should accept maximum concurrency") {
                val command = BatchGreetingCommand(
                    names = listOf("Alice"),
                    maxConcurrent = 100,
                )

                val result = command.validate()

                result.isRight() shouldBe true
            }
        }

        context("template validation") {

            it("should reject template without placeholder") {
                val command = BatchGreetingCommand(
                    names = listOf("Alice"),
                    greetingTemplate = "Hello there!",
                )

                val result = command.validate()

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        error shouldBe ApplicationError.ValidationError(
                            field = "greetingTemplate",
                            message = "Template must contain {name} placeholder",
                        )
                    },
                    { fail("Expected Left but got Right") },
                )
            }

            it("should accept template with placeholder") {
                val command = BatchGreetingCommand(
                    names = listOf("Alice"),
                    greetingTemplate = "Hello {name}!",
                )

                val result = command.validate()

                result.isRight() shouldBe true
            }

            it("should accept null template") {
                val command = BatchGreetingCommand(
                    names = listOf("Alice"),
                    greetingTemplate = null,
                )

                val result = command.validate()

                result.isRight() shouldBe true
            }
        }
    }

    describe("BatchGreetingCommand functionality") {

        context("toPersonNames conversion") {

            it("should convert valid names") {
                val command = BatchGreetingCommand(
                    names = listOf("Alice", "Bob"),
                )

                val result = command.toPersonNames()

                result.isRight() shouldBe true
                result.fold(
                    { fail("Expected Right but got Left: $it") },
                    { personNames ->
                        personNames shouldHaveSize 2
                        personNames.map { it.value } shouldBe listOf("Alice", "Bob")
                    },
                )
            }

            it("should fail on invalid name") {
                val command = BatchGreetingCommand(
                    names = listOf("Alice", ""), // Empty name should fail
                )

                val result = command.toPersonNames()

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        (error is ApplicationError.BatchValidationError) shouldBe true
                        if (error is ApplicationError.BatchValidationError) {
                            error.item shouldBe ""
                        }
                    },
                    { fail("Expected Left but got Right") },
                )
            }
        }

        context("template application") {

            it("should apply custom template") {
                val command = BatchGreetingCommand(
                    names = listOf("Alice"),
                    greetingTemplate = "Welcome, {name}!",
                )

                val result = command.applyTemplate("Alice") { "Hello $it" }

                result shouldBe "Welcome, Alice!"
            }

            it("should use default when no template") {
                val command = BatchGreetingCommand(
                    names = listOf("Alice"),
                )

                val result = command.applyTemplate("Alice") { "Hello $it" }

                result shouldBe "Hello Alice"
            }
        }

        context("utility methods") {

            it("should detect large batch") {
                val names = (1..150).map { "Name$it" }
                val command = BatchGreetingCommand(names = names)

                command.isLargeBatch() shouldBe true
            }

            it("should detect small batch") {
                val command = BatchGreetingCommand(
                    names = listOf("Alice", "Bob"),
                )

                command.isLargeBatch() shouldBe false
            }

            it("should chunk batch correctly") {
                val command = BatchGreetingCommand(
                    names = listOf("Alice", "Bob", "Charlie", "David", "Eve"),
                    maxConcurrent = 2,
                )

                val chunks = command.chunked()

                chunks shouldHaveSize 3
                chunks[0].names shouldBe listOf("Alice", "Bob")
                chunks[1].names shouldBe listOf("Charlie", "David")
                chunks[2].names shouldBe listOf("Eve")
            }
        }
    }
})
