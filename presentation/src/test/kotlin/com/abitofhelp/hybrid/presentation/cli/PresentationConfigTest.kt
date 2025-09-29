////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Test Suite
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.presentation.cli

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Test suite for [PresentationConfig] data class.
 *
 * ## Purpose
 *
 * This test suite validates the configuration data class used by the presentation layer
 * to pass command-line arguments and user preferences to CLI implementations. The tests
 * ensure proper data class behavior and validate common usage patterns.
 *
 * ## Testing Data Classes
 *
 * Data classes in Kotlin provide several built-in features that should be tested:
 *
 * ### 1. Automatic Generated Methods
 * Data classes automatically generate these methods that need validation:
 * - `equals()`: Structural equality based on all properties
 * - `hashCode()`: Consistent with equals for collections and maps
 * - `toString()`: Readable string representation
 * - `copy()`: Create modified copies while preserving immutability
 * - `componentN()`: Destructuring declarations support
 *
 * ### 2. Property Access
 * Verify that all properties are correctly stored and accessible:
 * ```kotlin
 * val config = PresentationConfig(verbose = true, outputPath = null, name = "Test")
 * config.verbose shouldBe true
 * config.outputPath shouldBe null
 * config.name shouldBe "Test"
 * ```
 *
 * ### 3. Null Safety
 * Test nullable properties behave correctly:
 * - Accept null values where allowed
 * - Maintain null state properly
 * - Work correctly in equals/hashCode with nulls
 *
 * ## Configuration Testing Patterns
 *
 * ### Common Configuration Scenarios
 * Test real-world usage patterns that users will encounter:
 *
 * #### Minimal Configuration
 * ```kotlin
 * PresentationConfig(verbose = false, outputPath = null, name = "User")
 * ```
 * - Basic usage with just a name
 * - No file output, no verbose logging
 * - Default behavior
 *
 * #### Verbose Mode
 * ```kotlin
 * PresentationConfig(verbose = true, outputPath = null, name = "Developer")
 * ```
 * - Enable detailed logging
 * - Useful for debugging and development
 * - Console output only
 *
 * #### File Output Mode
 * ```kotlin
 * PresentationConfig(verbose = false, outputPath = "/path/to/file.txt", name = "User")
 * ```
 * - Redirect output to file
 * - Useful for automation and logging
 * - Quiet operation
 *
 * #### Anonymous Usage
 * ```kotlin
 * PresentationConfig(verbose = false, outputPath = null, name = null)
 * ```
 * - No name provided by user
 * - Application should use default greeting
 * - Common when no command-line args given
 *
 * ## Data Class Best Practices
 *
 * ### Immutability
 * Data classes should be immutable to ensure:
 * - Thread safety
 * - Predictable behavior
 * - No accidental state changes
 * - Safe sharing between components
 *
 * Use `copy()` to create modified versions:
 * ```kotlin
 * val newConfig = originalConfig.copy(verbose = true)
 * ```
 *
 * ### Destructuring
 * Data classes support destructuring for convenient access:
 * ```kotlin
 * val (verbose, outputPath, name) = config
 * ```
 * This is useful when you need multiple properties at once.
 *
 * ### Equality Semantics
 * Two configurations are equal if all their properties are equal:
 * ```kotlin
 * val config1 = PresentationConfig(true, "file.txt", "Alice")
 * val config2 = PresentationConfig(true, "file.txt", "Alice")
 * config1 == config2 // true
 * ```
 *
 * This enables:
 * - Using configs as map keys
 * - Comparing configurations in tests
 * - Detecting configuration changes
 *
 * ## Testing Strategy
 *
 * ### Property Validation
 * - Test that all properties are stored correctly
 * - Verify null handling for optional properties
 * - Ensure type safety
 *
 * ### Data Class Features
 * - Validate toString() format for debugging
 * - Test equals() and hashCode() consistency
 * - Verify copy() creates proper modified instances
 * - Test destructuring support
 *
 * ### Usage Patterns
 * - Test common real-world configurations
 * - Verify edge cases (all nulls, all values)
 * - Ensure configurations work with CLI factories
 *
 * ## Why These Tests Matter
 *
 * Configuration objects are critical because they:
 * - Control application behavior across layers
 * - Are passed between multiple components
 * - Affect user experience directly
 * - Must be reliable and predictable
 *
 * Proper testing ensures that configuration changes don't break
 * existing functionality and that new features integrate correctly.
 */
class PresentationConfigTest : DescribeSpec({

    describe("PresentationConfig") {

        describe("creation") {
            /**
             * Validates that PresentationConfig can be created with all parameters specified.
             * This tests the primary constructor and verifies that all properties are
             * correctly initialized and accessible.
             */
            it("should create with all parameters") {
                val config = PresentationConfig(
                    verbose = true,
                    outputPath = "/tmp/output.txt",
                    name = "John",
                )

                config.verbose shouldBe true
                config.outputPath shouldBe "/tmp/output.txt"
                config.name shouldBe "John"
            }

            /**
             * Tests configuration creation with mixed null and non-null values.
             * This is a common scenario where some options are specified while
             * others use default (null) values. Validates proper null handling.
             */
            it("should create with mixed null and non-null values") {
                val config = PresentationConfig(
                    verbose = true,
                    outputPath = null,
                    name = "Jane",
                )

                config.verbose shouldBe true
                config.outputPath shouldBe null
                config.name shouldBe "Jane"
            }

            /**
             * Verifies that all properties can be set to null when appropriate.
             * This represents the minimal configuration case and ensures
             * null safety is properly implemented.
             */
            it("should create with null values") {
                val config = PresentationConfig(
                    verbose = false,
                    outputPath = null,
                    name = null,
                )

                config.verbose shouldBe false
                config.outputPath shouldBe null
                config.name shouldBe null
            }
        }

        describe("data class features") {
            /**
             * Validates the automatically generated toString() method.
             * The toString() format is important for debugging and logging.
             * Data classes should produce consistent, readable string representations.
             */
            it("should have correct toString") {
                val config = PresentationConfig(
                    verbose = true,
                    outputPath = "out.txt",
                    name = "Alice",
                )

                config.toString() shouldBe "PresentationConfig(verbose=true, outputPath=out.txt, name=Alice)"
            }

            /**
             * Tests the automatically generated equals() method.
             * Structural equality means two objects are equal if all their
             * properties are equal. This is essential for comparing configurations
             * and using them in collections.
             */
            it("should have correct equals") {
                val config1 = PresentationConfig(true, "out.txt", "Bob")
                val config2 = PresentationConfig(true, "out.txt", "Bob")
                val config3 = PresentationConfig(false, "out.txt", "Bob")

                config1 shouldBe config2
                config1 shouldNotBe config3
            }

            /**
             * Validates the automatically generated hashCode() method.
             * Hash codes must be consistent with equals() - if two objects
             * are equal, they must have the same hash code. This enables
             * using configurations as keys in maps and sets.
             */
            it("should have correct hashCode") {
                val config1 = PresentationConfig(true, "out.txt", "Charlie")
                val config2 = PresentationConfig(true, "out.txt", "Charlie")

                config1.hashCode() shouldBe config2.hashCode()
            }

            /**
             * Tests the automatically generated copy() method.
             * The copy() method allows creating modified versions while
             * maintaining immutability. This is crucial for functional
             * programming patterns and avoiding side effects.
             */
            it("should support copy") {
                val original = PresentationConfig(
                    verbose = false,
                    outputPath = "original.txt",
                    name = "David",
                )

                val copy = original.copy(verbose = true)

                copy.verbose shouldBe true
                copy.outputPath shouldBe "original.txt"
                copy.name shouldBe "David"
                original.verbose shouldBe false
            }

            /**
             * Validates destructuring declaration support via componentN() methods.
             * Destructuring allows convenient access to multiple properties at once.
             * This is syntactic sugar that improves code readability in some contexts.
             */
            it("should support destructuring") {
                val config = PresentationConfig(
                    verbose = true,
                    outputPath = "test.txt",
                    name = "Eve",
                )

                val (verbose, outputPath, name) = config

                verbose shouldBe true
                outputPath shouldBe "test.txt"
                name shouldBe "Eve"
            }
        }

        describe("common usage patterns") {
            /**
             * Tests minimal configuration pattern - basic usage with just a name.
             * This represents the most common use case where a user provides
             * only their name without additional options. Should work with
             * default behavior (console output, no verbose logging).
             */
            it("should represent minimal configuration") {
                val config = PresentationConfig(
                    verbose = false,
                    outputPath = null,
                    name = "User",
                )

                config.verbose shouldBe false
                config.outputPath shouldBe null
                config.name shouldBe "User"
            }

            /**
             * Tests verbose mode configuration for development and debugging.
             * When verbose is enabled, the application should provide detailed
             * logging and diagnostic information. This is commonly used by
             * developers to understand application behavior.
             */
            it("should represent verbose configuration") {
                val config = PresentationConfig(
                    verbose = true,
                    outputPath = null,
                    name = "Developer",
                )

                config.verbose shouldBe true
                config.outputPath shouldBe null
                config.name shouldBe "Developer"
            }

            /**
             * Tests file output configuration for automation and batch processing.
             * When outputPath is specified, the application should redirect
             * output to a file instead of the console. This is useful for
             * scripting, automation, and creating permanent records.
             */
            it("should represent file output configuration") {
                val config = PresentationConfig(
                    verbose = false,
                    outputPath = "/home/user/greetings.txt",
                    name = "FileUser",
                )

                config.verbose shouldBe false
                config.outputPath shouldBe "/home/user/greetings.txt"
                config.name shouldBe "FileUser"
            }

            /**
             * Tests anonymous usage when no name is provided.
             * This occurs when users run the application without command-line
             * arguments. The application should handle this gracefully,
             * typically by using a default greeting or prompting for input.
             */
            it("should represent missing name configuration") {
                val config = PresentationConfig(
                    verbose = false,
                    outputPath = null,
                    name = null,
                )

                config.verbose shouldBe false
                config.outputPath shouldBe null
                config.name shouldBe null
            }
        }
    }
})
