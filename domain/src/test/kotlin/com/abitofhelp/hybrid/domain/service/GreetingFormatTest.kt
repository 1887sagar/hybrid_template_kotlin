// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Test Suite
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.domain.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for the GreetingFormat enum class.
 *
 * ## Purpose
 * This test suite validates the behavior of the GreetingFormat enum, which defines
 * the available greeting styles in our domain. While enum testing might seem trivial,
 * it ensures the enum contract remains stable as the codebase evolves.
 *
 * ## What is Being Tested
 * 1. **Enum Constants**: Verifies all expected formats exist
 * 2. **Enum Completeness**: Ensures no formats are accidentally added or removed
 * 3. **Enum Operations**: Tests standard enum methods (valueOf, values)
 * 4. **Type Safety**: Confirms enums provide compile-time safety
 *
 * ## Why Test Enums?
 * Testing enums might seem unnecessary, but it serves important purposes:
 * - **API Stability**: Ensures the enum values don't change unexpectedly
 * - **Documentation**: Tests document which formats are available
 * - **Refactoring Safety**: Catch accidental enum modifications
 * - **Integration Contracts**: Other systems may depend on these exact values
 *
 * ## Testing Strategy
 * - **Existence Testing**: Verify each enum constant exists
 * - **Count Verification**: Ensure no extra values are added
 * - **Name Resolution**: Test string-to-enum conversion
 * - **Identity Verification**: Confirm enum constants are singletons
 *
 * ## Key Patterns Used
 * 1. **Enum Pattern**: Using enums for type-safe constants
 * 2. **Singleton Pattern**: Each enum value is a singleton instance
 * 3. **Factory Pattern**: valueOf() acts as a factory method
 * 4. **Exhaustive Testing**: Testing all enum values ensures completeness
 *
 * ## Educational Value
 * This test demonstrates several important concepts:
 * - **Enum Behavior**: How Kotlin enums work as singleton instances
 * - **Type Safety Benefits**: Why enums are better than string constants
 * - **API Evolution**: How to test that APIs remain backward compatible
 * - **Test Coverage**: Even simple components deserve tests
 * - **Living Documentation**: Tests document available options
 *
 * ## Common Enum Pitfalls
 * - Adding new enum values can break existing switch/when statements
 * - Removing enum values breaks backward compatibility
 * - Changing enum names affects serialization and database storage
 * - Enum ordinal values can change if order is modified
 *
 * ## Example Usage in Domain
 * ```kotlin
 * val format = GreetingFormat.FRIENDLY
 * val greeting = when (format) {
 *     GreetingFormat.DEFAULT -> "Hello!"
 *     GreetingFormat.FRIENDLY -> "Hey there!"
 *     GreetingFormat.FORMAL -> "Greetings."
 * }
 * ```
 *
 * @see GreetingFormat The enum being tested
 * @see GreetingPolicy Where these formats are selected
 * @see GreetingService Where these formats are applied
 */
class GreetingFormatTest : DescribeSpec({

    describe("GreetingFormat") {

        context("enum constants") {
            it("should have DEFAULT format") {
                // Verifies DEFAULT exists and is accessible
                GreetingFormat.DEFAULT shouldBe GreetingFormat.DEFAULT
            }

            it("should have FRIENDLY format") {
                // Verifies FRIENDLY exists and is accessible
                GreetingFormat.FRIENDLY shouldBe GreetingFormat.FRIENDLY
            }

            it("should have FORMAL format") {
                // Verifies FORMAL exists and is accessible
                GreetingFormat.FORMAL shouldBe GreetingFormat.FORMAL
            }
        }

        context("enum completeness") {
            it("should have exactly 3 formats") {
                // Guards against accidental additions/removals
                // If this fails, either update the test or fix the enum
                GreetingFormat.values().size shouldBe 3
            }

            it("should contain all expected formats") {
                // Explicit verification of complete enum contents
                val allFormats = GreetingFormat.values().toSet()
                allFormats shouldBe setOf(
                    GreetingFormat.DEFAULT,
                    GreetingFormat.FRIENDLY,
                    GreetingFormat.FORMAL,
                )
            }
        }

        context("enum operations") {
            it("should find format by name using valueOf") {
                // valueOf() is crucial for deserializing enums from strings
                GreetingFormat.valueOf("DEFAULT") shouldBe GreetingFormat.DEFAULT
                GreetingFormat.valueOf("FRIENDLY") shouldBe GreetingFormat.FRIENDLY
                GreetingFormat.valueOf("FORMAL") shouldBe GreetingFormat.FORMAL
            }

            it("should have stable ordinal values") {
                // Ordinals should remain stable for backward compatibility
                // Warning: Relying on ordinals is generally discouraged
                GreetingFormat.DEFAULT.ordinal shouldBe 0
                GreetingFormat.FRIENDLY.ordinal shouldBe 1
                GreetingFormat.FORMAL.ordinal shouldBe 2
            }

            it("should have correct name property") {
                // The name property is used for serialization
                GreetingFormat.DEFAULT.name shouldBe "DEFAULT"
                GreetingFormat.FRIENDLY.name shouldBe "FRIENDLY"
                GreetingFormat.FORMAL.name shouldBe "FORMAL"
            }
        }
    }
})
