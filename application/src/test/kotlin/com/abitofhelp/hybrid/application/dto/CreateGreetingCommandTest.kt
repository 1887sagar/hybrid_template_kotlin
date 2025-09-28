/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.application.dto

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unit tests for the CreateGreetingCommand data transfer object.
 *
 * ## Purpose
 * This test suite validates the behavior of the CreateGreetingCommand, which serves as
 * the input DTO for the greeting creation use case. It demonstrates comprehensive testing
 * of Kotlin data classes, including all auto-generated methods and edge cases.
 *
 * ## What is Being Tested
 * 1. **Construction**: Various ways to create the command with different parameters
 * 2. **Default Values**: Kotlin's default parameter behavior
 * 3. **Nullable Handling**: How the command handles null vs empty vs whitespace names
 * 4. **Data Class Features**: toString, equals, hashCode, copy, and destructuring
 * 5. **Immutability**: Ensures the command remains immutable after creation
 *
 * ## Testing Strategy
 * - **Comprehensive Coverage**: Tests all data class features, not just construction
 * - **Edge Case Testing**: Null, empty string, whitespace, and normal values
 * - **Feature Testing**: Validates Kotlin-specific features like destructuring
 * - **Contract Testing**: Ensures equals/hashCode contract is properly maintained
 *
 * ## Key Patterns Used
 * 1. **Data Transfer Object (DTO)**: Command pattern for use case inputs
 * 2. **Nullable Design**: Intentionally allows null names for anonymous users
 * 3. **Immutability**: Data class ensures thread-safe value objects
 * 4. **Builder Pattern Alternative**: Copy method provides flexible object creation
 *
 * ## Why These Tests Matter
 * - **API Contract**: DTOs define the contract between layers in the architecture
 * - **Kotlin Features**: Validates that Kotlin's data class features work correctly
 * - **Edge Case Documentation**: Shows how the DTO handles various input scenarios
 * - **Refactoring Safety**: Can safely modify DTO structure with test coverage
 * - **Integration Confidence**: DTOs are often serialized/deserialized in real apps
 *
 * ## Educational Value
 * This test demonstrates several important testing concepts:
 * - **Data Class Testing**: How to thoroughly test Kotlin data classes
 * - **Nullable vs Empty**: The semantic difference between null and empty strings
 * - **Structural vs Behavioral**: Testing structure (fields) vs behavior (methods)
 * - **Contract Testing**: Verifying equals/hashCode/toString contracts
 * - **Copy Testing**: Ensuring immutability through copy operations
 *
 * ## Design Decisions Validated
 * - **Nullable Name**: Allows representing anonymous users with null
 * - **Silent Flag**: Supports different output modes without complex inheritance
 * - **No Validation**: Commands are simple DTOs; validation happens in use cases
 * - **Public Fields**: Data class properties are public for easy access
 *
 * ## Testing Insights
 * - The command preserves whitespace in names (e.g., "  Jane  ")
 * - Default values make the command easy to construct for common cases
 * - Destructuring support enables elegant pattern matching in use cases
 * - The toString format is useful for logging and debugging
 *
 * @see CreateGreetingCommand The DTO being tested
 * @see CreateGreetingUseCase The use case that consumes this command
 */
class CreateGreetingCommandTest : DescribeSpec({

    describe("CreateGreetingCommand") {

        it("should create command with name") {
            val command = CreateGreetingCommand(name = "John")
            command.name shouldBe "John"
            command.silent shouldBe false
        }

        it("should create command with empty name") {
            val command = CreateGreetingCommand(name = "")
            command.name shouldBe ""
            command.silent shouldBe false
        }

        it("should create command with whitespace name") {
            val command = CreateGreetingCommand(name = "  Jane  ")
            command.name shouldBe "  Jane  "
            command.silent shouldBe false
        }

        it("should create command with null name") {
            val command = CreateGreetingCommand(name = null)
            command.name shouldBe null
            command.silent shouldBe false
        }

        it("should create command with silent flag") {
            val command = CreateGreetingCommand(name = "John", silent = true)
            command.name shouldBe "John"
            command.silent shouldBe true
        }

        it("should use default values") {
            val command = CreateGreetingCommand()
            command.name shouldBe null
            command.silent shouldBe false
        }

        it("should have correct toString") {
            val command = CreateGreetingCommand(name = "John")
            command.toString() shouldBe "CreateGreetingCommand(name=John, silent=false)"
        }

        it("should have correct equals") {
            val command1 = CreateGreetingCommand(name = "John")
            val command2 = CreateGreetingCommand(name = "John")
            val command3 = CreateGreetingCommand(name = "Jane")

            command1 shouldBe command2
            command1 shouldNotBe command3
        }

        it("should have correct hashCode") {
            val command1 = CreateGreetingCommand(name = "John")
            val command2 = CreateGreetingCommand(name = "John")

            command1.hashCode() shouldBe command2.hashCode()
        }

        it("should support copy") {
            val original = CreateGreetingCommand(name = "John", silent = false)
            val copy = original.copy(name = "Jane", silent = true)

            copy.name shouldBe "Jane"
            copy.silent shouldBe true
            original.name shouldBe "John"
            original.silent shouldBe false
        }

        it("should support destructuring") {
            val command = CreateGreetingCommand(name = "John", silent = true)
            val (name, silent) = command

            name shouldBe "John"
            silent shouldBe true
        }
    }
})
