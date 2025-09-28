// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.application.dto

import com.abitofhelp.hybrid.application.error.ApplicationError
import io.kotest.assertions.fail
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Comprehensive tests for BatchGreetingCommand validation and functionality.
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
