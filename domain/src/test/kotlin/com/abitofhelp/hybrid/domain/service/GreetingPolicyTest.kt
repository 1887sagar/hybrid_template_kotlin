/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.domain.service

import com.abitofhelp.hybrid.domain.value.PersonName
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for the GreetingPolicy domain service.
 *
 * ## Purpose
 * This test suite validates the business logic encapsulated in the GreetingPolicy service,
 * which determines how greetings should be formatted based on domain rules. This demonstrates
 * the concept of a Domain Service - stateless operations that don't naturally fit on entities.
 *
 * ## What is Being Tested
 * 1. **Format Selection Logic**: Tests the rules for choosing FRIENDLY, FORMAL, or DEFAULT formats
 * 2. **Edge Cases**: Validates behavior at exact boundaries (e.g., name length threshold)
 * 3. **Special Cases**: Tests handling of "Anonymous" names with case-insensitive matching
 * 4. **Constants**: Verifies domain-specific constants are correctly defined
 *
 * ## Testing Strategy
 * - **Boundary Testing**: Tests values at, just below, and just above thresholds
 * - **Equivalence Partitioning**: Groups inputs into categories (normal, long, anonymous)
 * - **Case Variation Testing**: Ensures case-insensitive logic works correctly
 * - **Descriptive Contexts**: Uses BDD-style contexts to clearly communicate intent
 *
 * ## Key Patterns Used
 * 1. **Domain Service Pattern**: GreetingPolicy is a stateless service with pure functions
 * 2. **Policy Pattern**: Encapsulates business rules for greeting format selection
 * 3. **Value Object Usage**: Works with PersonName value objects, not primitives
 * 4. **Arrange-Act-Assert**: Clear test structure with explicit test data setup
 *
 * ## Why These Tests Matter
 * - **Business Rule Documentation**: Tests serve as executable specifications of business rules
 * - **Boundary Confidence**: Ensures edge cases don't cause unexpected behavior
 * - **Refactoring Safety**: Can confidently modify implementation while preserving behavior
 * - **Domain Knowledge**: Tests capture why certain thresholds and rules exist
 * - **Integration Points**: Validates how domain services interact with value objects
 *
 * ## Educational Value
 * This test demonstrates several important domain-driven design concepts:
 * - **Domain Services vs Entities**: When to use stateless services for domain logic
 * - **Policy Objects**: Encapsulating business rules in testable components
 * - **Value Object Testing**: Testing behavior that depends on value object state
 * - **Business Rule Testing**: How to test complex conditional logic clearly
 * - **Test as Documentation**: Writing tests that explain the "why" behind rules
 *
 * ## Testing Insights
 * - The 20-character threshold represents a business decision about formality
 * - Anonymous users get special treatment to maintain privacy
 * - Case-insensitive matching prevents bypassing anonymous detection
 * - The MAX_GREETING_LENGTH constant ensures downstream systems can handle output
 *
 * @see GreetingPolicy The domain service being tested
 * @see GreetingFormat The enum defining available greeting formats
 * @see PersonName The value object used as input to the policy
 */
class GreetingPolicyTest : DescribeSpec({

    describe("GreetingPolicy") {

        describe("determineGreetingFormat") {
            context("when name length is normal") {
                it("should return FRIENDLY format") {
                    val name = PersonName.create("John").getOrNull()!!
                    val format = GreetingPolicy.determineGreetingFormat(name)
                    format shouldBe GreetingFormat.FRIENDLY
                }

                it("should return FRIENDLY for names up to threshold") {
                    val name = PersonName.create("A".repeat(20)).getOrNull()!! // Exactly at threshold
                    val format = GreetingPolicy.determineGreetingFormat(name)
                    format shouldBe GreetingFormat.FRIENDLY
                }
            }

            context("when name is long") {
                it("should return FORMAL format for long names") {
                    val name = PersonName.create("A".repeat(21)).getOrNull()!! // Just over threshold
                    val format = GreetingPolicy.determineGreetingFormat(name)
                    format shouldBe GreetingFormat.FORMAL
                }

                it("should return FORMAL for very long names") {
                    val name = PersonName.create("Alexander Christopher Wellington").getOrNull()!!
                    val format = GreetingPolicy.determineGreetingFormat(name)
                    format shouldBe GreetingFormat.FORMAL
                }
            }

            context("when name is Anonymous") {
                it("should return DEFAULT format") {
                    val name = PersonName.anonymous()
                    val format = GreetingPolicy.determineGreetingFormat(name)
                    format shouldBe GreetingFormat.DEFAULT
                }

                it("should be case insensitive for Anonymous") {
                    val name = PersonName.create("ANONYMOUS").getOrNull()!!
                    val format = GreetingPolicy.determineGreetingFormat(name)
                    format shouldBe GreetingFormat.DEFAULT
                }

                it("should handle mixed case Anonymous") {
                    val name = PersonName.create("AnOnYmOuS").getOrNull()!!
                    val format = GreetingPolicy.determineGreetingFormat(name)
                    format shouldBe GreetingFormat.DEFAULT
                }
            }
        }

        describe("constants") {
            it("should have MAX_GREETING_LENGTH of 200") {
                GreetingPolicy.MAX_GREETING_LENGTH shouldBe 200
            }
        }
    }
})
