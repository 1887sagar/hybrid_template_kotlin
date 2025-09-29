////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Test Suite
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.bootstrap

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Comprehensive test suite for [AppConfig] data class and its parsing functionality.
 *
 * ## What This Tests
 *
 * This test suite validates the configuration system that handles command-line arguments
 * and application settings. It ensures that user inputs are correctly parsed into a
 * structured configuration object that drives the application's behavior.
 *
 * ## Why These Tests Are Important
 *
 * 1. **Input Validation**: Verifies that command-line arguments are parsed correctly
 * 2. **Data Integrity**: Ensures configuration objects maintain consistent state
 * 3. **Edge Case Handling**: Validates behavior with unusual but valid inputs
 * 4. **Error Handling**: Confirms appropriate errors for invalid arguments
 * 5. **Usability**: Tests that the parsing logic supports flexible argument ordering
 *
 * ## Test Scenarios Covered
 *
 * ### Configuration Creation
 * - Default configuration with no arguments
 * - Custom configuration with all parameters specified
 * - Partial configuration with some parameters
 *
 * ### Argument Parsing
 * - **Name parsing**: Positional arguments, names with spaces, empty names
 * - **Output path parsing**: File paths, paths with spaces, missing values
 * - **Verbose flag parsing**: Long form (--verbose), short form (-v), multiple flags
 * - **Combined arguments**: All options together, flexible ordering
 *
 * ### Edge Cases
 * - Empty argument arrays
 * - Flag-only arguments
 * - Malformed argument combinations
 * - Arguments in unexpected positions
 *
 * ### Data Class Features
 * - toString(), equals(), hashCode() implementations
 * - copy() method for immutable updates
 *
 * ## Example Usage Patterns
 *
 * ```kotlin
 * // Basic configuration
 * val config = AppConfig(name = "Alice")
 *
 * // From command line: ["Bob", "--verbose", "--out", "file.txt"]
 * val parsed = parseArgs(args)
 * ```
 *
 * ## Testing Best Practices Demonstrated
 *
 * 1. **Clear Test Structure**: Using Kotest's DescribeSpec for hierarchical organization
 * 2. **Descriptive Test Names**: Each test clearly states what it validates
 * 3. **Comprehensive Coverage**: Testing both happy path and edge cases
 * 4. **Assertion Clarity**: Using specific matchers for different validation types
 * 5. **Boundary Testing**: Validating behavior at input limits
 *
 * ## Common Parsing Patterns
 *
 * ### Flag Detection
 * Tests verify that the parser correctly identifies flags vs. values:
 * - `--verbose` sets a boolean flag
 * - `--out value` requires a parameter
 * - Flags can appear in any order
 *
 * ### Value Extraction
 * Tests confirm that non-flag arguments become the name parameter:
 * - First non-flag argument becomes the name
 * - Subsequent non-flag arguments are ignored
 * - Empty strings are valid names
 */
class AppConfigTest : DescribeSpec({

    describe("AppConfig") {

        describe("creation") {
            it("should create with defaults") {
                val config = AppConfig()

                config.verbose shouldBe false
                config.outputPath shouldBe null
                config.name shouldBe null
            }

            it("should create with all parameters") {
                val config = AppConfig(
                    verbose = true,
                    outputPath = "/tmp/output.txt",
                    name = "TestUser",
                )

                config.verbose shouldBe true
                config.outputPath shouldBe "/tmp/output.txt"
                config.name shouldBe "TestUser"
            }
        }

        describe("parseArgs function") {

            context("parsing name argument") {
                it("should parse positional argument as name") {
                    val args = arrayOf("John")
                    val config = parseArgs(args)

                    config.name shouldBe "John"
                    config.verbose shouldBe false
                    config.outputPath shouldBe null
                }

                it("should use first non-flag argument as name") {
                    val args = arrayOf("--verbose", "Jane")
                    val config = parseArgs(args)

                    config.name shouldBe "Jane"
                    config.verbose shouldBe true
                }

                it("should handle name with spaces") {
                    val args = arrayOf("John Doe")
                    val config = parseArgs(args)

                    config.name shouldBe "John Doe"
                }

                it("should handle empty name") {
                    val args = arrayOf("")
                    val config = parseArgs(args)

                    config.name shouldBe ""
                }

                it("should skip flag arguments when finding name") {
                    val args = arrayOf("--verbose", "-v", "TestUser", "--out", "file.txt")
                    val config = parseArgs(args)

                    config.name shouldBe "TestUser"
                }
            }

            context("parsing output argument") {
                it("should parse --out with value") {
                    val args = arrayOf("--out", "./test-output/greeting.txt")
                    val config = parseArgs(args)

                    config.outputPath shouldContain "test-output/greeting.txt"
                    config.verbose shouldBe false
                    config.name shouldBe null
                }

                it("should handle paths with spaces") {
                    val args = arrayOf("--out", "./test output/my file.txt")

                    // Spaces are allowed in paths
                    val config = parseArgs(args)
                    config.outputPath shouldContain "test output/my file.txt"
                }

                it("should throw error if --out has no value") {
                    val args = arrayOf("--out")

                    shouldThrow<IllegalArgumentException> {
                        parseArgs(args)
                    }.message shouldBe "--out requires a file path"
                }

                it("should use value after --out") {
                    val args = arrayOf("User", "--out", "output.txt", "--verbose")
                    val config = parseArgs(args)

                    config.outputPath shouldBe "output.txt"
                    config.name shouldBe "User"
                }
            }

            context("parsing verbose argument") {
                it("should parse --verbose") {
                    val args = arrayOf("--verbose")
                    val config = parseArgs(args)

                    config.verbose shouldBe true
                    config.name shouldBe null
                    config.outputPath shouldBe null
                }

                it("should parse -v short form") {
                    val args = arrayOf("-v")
                    val config = parseArgs(args)

                    config.verbose shouldBe true
                }

                it("should handle multiple verbose flags") {
                    val args = arrayOf("--verbose", "-v", "--verbose")
                    val config = parseArgs(args)

                    config.verbose shouldBe true
                }
            }

            context("parsing combined arguments") {
                it("should parse all arguments together") {
                    val args = arrayOf(
                        "Alice",
                        "--out",
                        "greeting.txt",
                        "--verbose",
                    )
                    val config = parseArgs(args)

                    config.name shouldBe "Alice"
                    config.outputPath shouldBe "greeting.txt"
                    config.verbose shouldBe true
                }

                it("should parse arguments in any order") {
                    val args = arrayOf(
                        "--verbose",
                        "--out",
                        "file.txt",
                        "Bob",
                    )
                    val config = parseArgs(args)

                    config.name shouldBe "Bob"
                    config.outputPath shouldContain "file.txt"
                    config.verbose shouldBe true
                }

                it("should parse mixed long and short forms") {
                    val args = arrayOf(
                        "-v",
                        "--out",
                        "out.txt",
                        "Charlie",
                    )
                    val config = parseArgs(args)

                    config.name shouldBe "Charlie"
                    config.outputPath shouldContain "out.txt"
                    config.verbose shouldBe true
                }
            }

            context("handling edge cases") {
                it("should handle empty args") {
                    val args = emptyArray<String>()
                    val config = parseArgs(args)

                    config.name shouldBe null
                    config.outputPath shouldBe null
                    config.verbose shouldBe false
                }

                it("should handle only flags") {
                    val args = arrayOf("--verbose", "--out", "file.txt")
                    val config = parseArgs(args)

                    config.name shouldBe null
                    config.verbose shouldBe true
                    config.outputPath shouldContain "file.txt"
                }

                it("should handle missing out value") {
                    val args = arrayOf("--out", "--verbose")

                    // The parser will actually create a valid config with outputPath="--verbose"
                    // and then continue parsing, finding no more arguments
                    val config = parseArgs(args)
                    config.outputPath shouldContain "--verbose"
                    config.verbose shouldBe false // --verbose was consumed as a path
                }

                it("should handle non-flag arguments after flags") {
                    val args = arrayOf(
                        "--verbose",
                        "Eve",
                        "extra",
                        "--out",
                        "out.txt",
                    )
                    val config = parseArgs(args)

                    config.name shouldBe "Eve" // First non-flag
                    config.verbose shouldBe true
                    config.outputPath shouldBe "out.txt"
                }
            }
        }

        describe("data class features") {
            it("should have correct toString") {
                val config = AppConfig(true, "out.txt", "User")
                config.toString() shouldBe "AppConfig(verbose=true, outputPath=out.txt, name=User)"
            }

            it("should have correct equals") {
                val config1 = AppConfig(true, "out.txt", "User")
                val config2 = AppConfig(true, "out.txt", "User")
                val config3 = AppConfig(false, "out.txt", "User")

                config1 shouldBe config2
                config1 shouldNotBe config3
            }

            it("should have correct hashCode") {
                val config1 = AppConfig(true, "out.txt", "User")
                val config2 = AppConfig(true, "out.txt", "User")

                config1.hashCode() shouldBe config2.hashCode()
            }

            it("should support copy") {
                val original = AppConfig(false, "original.txt", "Original")
                val copy = original.copy(verbose = true)

                copy.verbose shouldBe true
                copy.outputPath shouldBe "original.txt"
                copy.name shouldBe "Original"
                original.verbose shouldBe false
            }
        }
    }
})
