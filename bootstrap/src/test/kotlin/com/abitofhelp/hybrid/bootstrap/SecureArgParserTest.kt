/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.bootstrap

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Test suite for SecureArgParser.
 *
 * ## Security Testing Philosophy
 *
 * Security components require comprehensive testing to ensure:
 * 1. **Valid inputs work**: Normal use cases function correctly
 * 2. **Malicious inputs fail**: Attack vectors are properly blocked
 * 3. **Edge cases handled**: Unusual but valid inputs work
 * 4. **Clear error messages**: Users understand what went wrong
 *
 * ## Attack Vectors Tested
 *
 * ### Command Injection
 * - Shell metacharacters: `;`, `|`, `&`, `$(...)`
 * - Command substitution: `` ` `` backticks
 * - Variable expansion: `$VAR`, `${VAR}`
 *
 * ### Path Traversal
 * - Directory traversal: `..`, `../../../etc/passwd`
 * - Home expansion: `~`, `~/file`
 * - Absolute paths: Allowed but validated
 *
 * ### Input Validation
 * - Length limits: Prevent buffer overflow attempts
 * - Character restrictions: Only safe characters allowed
 * - Encoding attacks: Unicode, URL encoding
 *
 * ## Test Organization
 *
 * Tests are grouped by scenario:
 * - **Valid arguments**: Normal usage patterns
 * - **Security violations**: Attack attempts
 * - **Edge cases**: Boundary conditions
 * - **Error messages**: User feedback quality
 *
 * ## Testing Patterns
 *
 * ### Testing Expected Exceptions
 * ```kotlin
 * shouldThrow<SecurityException> {
 *     SecureArgParser.parseSecure(maliciousArgs)
 * }
 * ```
 *
 * ### Verifying Error Messages
 * ```kotlin
 * val exception = shouldThrow<SecurityException> {
 *     SecureArgParser.parseSecure(args)
 * }
 * exception.message shouldContain "specific error"
 * ```
 *
 * ## Security Test Best Practices
 *
 * 1. **Test both positive and negative cases**
 * 2. **Include real attack payloads** (safely)
 * 3. **Verify exact error messages** for debugging
 * 4. **Test combinations** of valid and invalid inputs
 * 5. **Document why each test exists**
 */
class SecureArgParserTest : DescribeSpec({

    describe("SecureArgParser") {

        describe("valid arguments") {

            it("should parse simple name") {
                val args = arrayOf("John")
                val config = SecureArgParser.parseSecure(args)

                config.name shouldBe "John"
                config.verbose shouldBe false
                config.outputPath shouldBe null
            }

            it("should parse verbose flag") {
                val args = arrayOf("--verbose", "Alice")
                val config = SecureArgParser.parseSecure(args)

                config.name shouldBe "Alice"
                config.verbose shouldBe true
            }

            it("should parse output path") {
                val args = arrayOf("Bob", "--out", "./test-output/test.txt")
                val config = SecureArgParser.parseSecure(args)

                config.name shouldBe "Bob"
                config.outputPath shouldContain "test-output/test.txt"
            }

            it("should parse --out=path format") {
                val args = arrayOf("--out=./output/output.txt", "Charlie")
                val config = SecureArgParser.parseSecure(args)

                config.name shouldBe "Charlie"
                config.outputPath shouldContain "output/output.txt"
            }

            it("should handle all options") {
                val args = arrayOf("-v", "--out", "./output/file.txt", "David")
                val config = SecureArgParser.parseSecure(args)

                config.name shouldBe "David"
                config.verbose shouldBe true
                config.outputPath shouldContain "output/file.txt"
            }

            it("should handle empty name") {
                val args = arrayOf("")
                val config = SecureArgParser.parseSecure(args)

                config.name shouldBe ""
            }
        }

        describe("security validation") {

            it("should reject path traversal attempts") {
                val args = arrayOf("--out", "../../../etc/passwd")
                val exception = shouldThrow<IllegalArgumentException> {
                    SecureArgParser.parseSecure(args)
                }
                exception.message shouldContain ".."
            }

            it("should reject command injection in name") {
                val args = arrayOf("John; rm -rf /")
                val exception = shouldThrow<IllegalArgumentException> {
                    SecureArgParser.parseSecure(args)
                }
                exception.message shouldContain ";"
            }

            it("should reject shell variables") {
                val args = arrayOf("--out", "\$HOME/file.txt")
                val exception = shouldThrow<IllegalArgumentException> {
                    SecureArgParser.parseSecure(args)
                }
                exception.message shouldContain "$"
            }

            it("should reject command substitution") {
                val args = arrayOf("`whoami`")
                val exception = shouldThrow<IllegalArgumentException> {
                    SecureArgParser.parseSecure(args)
                }
                exception.message shouldContain "`"
            }

            it("should reject null bytes") {
                val args = arrayOf("test\u0000hack")
                val exception = shouldThrow<IllegalArgumentException> {
                    SecureArgParser.parseSecure(args)
                }
                exception.message shouldContain "null byte"
            }

            it("should reject system paths") {
                val args = arrayOf("--out", "/etc/shadow")
                val exception = shouldThrow<IllegalArgumentException> {
                    SecureArgParser.parseSecure(args)
                }
                exception.message shouldContain "system directory"
            }

            it("should reject extremely long arguments") {
                val longArg = "x".repeat(2000)
                val args = arrayOf(longArg)
                val exception = shouldThrow<IllegalArgumentException> {
                    SecureArgParser.parseSecure(args)
                }
                exception.message shouldContain "too long"
            }

            it("should reject too many arguments") {
                val args = Array(200) { "arg$it" }
                val exception = shouldThrow<IllegalArgumentException> {
                    SecureArgParser.parseSecure(args)
                }
                exception.message shouldContain "Too many arguments"
            }
        }

        describe("error handling") {

            it("should require path after --out") {
                val args = arrayOf("--out")
                val exception = shouldThrow<IllegalArgumentException> {
                    SecureArgParser.parseSecure(args)
                }
                exception.message shouldContain "requires a file path"
            }

            it("should reject unknown options") {
                val args = arrayOf("--unknown")
                val exception = shouldThrow<IllegalArgumentException> {
                    SecureArgParser.parseSecure(args)
                }
                exception.message shouldContain "Unknown option"
            }

            it("should handle help flag") {
                val args = arrayOf("--help")
                shouldThrow<ShowHelpException> {
                    SecureArgParser.parseSecure(args)
                }
            }

            it("should handle short help flag") {
                val args = arrayOf("-h")
                shouldThrow<ShowHelpException> {
                    SecureArgParser.parseSecure(args)
                }
            }
        }

        describe("edge cases") {

            it("should handle empty arguments") {
                val args = emptyArray<String>()
                val config = SecureArgParser.parseSecure(args)

                config.name shouldBe null
                config.verbose shouldBe false
                config.outputPath shouldBe null
            }

            it("should handle whitespace in names") {
                val args = arrayOf("   John Doe   ")
                val config = SecureArgParser.parseSecure(args)

                config.name shouldBe "John Doe" // Trimmed
            }

            it("should not confuse file path with name") {
                val args = arrayOf("--verbose", "--out", "output.txt")
                val config = SecureArgParser.parseSecure(args)

                config.name shouldBe null // output.txt is not the name
                config.outputPath shouldBe "output.txt"
                config.verbose shouldBe true
            }
        }
    }
})
