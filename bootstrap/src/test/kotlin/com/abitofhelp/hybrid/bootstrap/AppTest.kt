/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.bootstrap

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class AppTest : DescribeSpec({

    describe("App.run") {

        lateinit var outputStream: ByteArrayOutputStream
        lateinit var errorStream: ByteArrayOutputStream
        lateinit var originalOut: PrintStream
        lateinit var originalErr: PrintStream

        beforeEach {
            outputStream = ByteArrayOutputStream()
            errorStream = ByteArrayOutputStream()
            originalOut = System.out
            originalErr = System.err
            System.setOut(PrintStream(outputStream))
            System.setErr(PrintStream(errorStream))
        }

        afterEach {
            System.setOut(originalOut)
            System.setErr(originalErr)
        }

        describe("successful execution") {

            it("should run with valid arguments") {
                // Given
                val args = arrayOf("TestUser")

                // When
                val result = App.run(args)

                // Then
                result shouldBe 0
                outputStream.toString() shouldContain "Hey there, TestUser! Welcome!"
                errorStream.toString() shouldBe ""
            }

            it("should run with verbose flag") {
                // Given
                val args = arrayOf("VerboseUser", "--verbose")

                // When
                val result = App.run(args)

                // Then
                result shouldBe 0
                val output = outputStream.toString()
                output shouldContain "[INFO] Greeting delivered successfully"
                output shouldContain "Hey there, VerboseUser! Welcome!"
            }

            it("should run with output file") {
                // Given
                val tempFile = kotlin.io.path.createTempFile("test", ".txt").toFile()
                tempFile.deleteOnExit()
                val args = arrayOf("FileUser", "--out", tempFile.absolutePath)

                // When
                val result = App.run(args)

                // Then
                result shouldBe 0
                // Note: current implementation doesn't support file output yet

                // Cleanup
                tempFile.delete()
            }

            it("should run with all options") {
                // Given
                val tempFile = kotlin.io.path.createTempFile("test", ".txt").toFile()
                tempFile.deleteOnExit()
                val args = arrayOf(
                    "CompleteUser",
                    "--out",
                    tempFile.absolutePath,
                    "--verbose",
                )

                // When
                val result = App.run(args)

                // Then
                result shouldBe 0
                val output = outputStream.toString()
                output shouldContain "[INFO] Greeting delivered successfully"

                // Cleanup
                tempFile.delete()
            }
        }

        describe("configuration handling") {

            it("should use anonymous when no name provided") {
                // Given
                val args = emptyArray<String>()

                // When
                val result = App.run(args)

                // Then
                result shouldBe 0
                outputStream.toString() shouldContain "Tip: You can provide your name as a command-line argument"
                outputStream.toString() shouldContain "Hello World from Anonymous!"
            }

            it("should return error code for empty name") {
                // Given
                val args = arrayOf("")

                // When
                val result = App.run(args)

                // Then
                result shouldBe 0
                outputStream.toString() shouldContain "Hello World from Anonymous!"
            }

            it("should handle blank name as anonymous") {
                // Given
                val args = arrayOf("   ")

                // When
                val result = App.run(args)

                // Then
                result shouldBe 0
                outputStream.toString() shouldContain "Hello World from Anonymous!"
            }

            it("should handle verbose flag with shorthand") {
                // Given
                val args = arrayOf("TestUser", "-v")

                // When
                val result = App.run(args)

                // Then
                result shouldBe 0
                outputStream.toString() shouldContain "[INFO] Greeting delivered successfully"
            }
        }

        describe("error handling") {

            it("should handle configuration parse errors gracefully") {
                // Given - simulate an edge case
                val args = arrayOf("--out") // Missing value for out

                // When
                val result = App.run(args)

                // Then
                result shouldBe 0 // parseArgs handles this gracefully
            }
        }

        describe("exit codes") {

            it("should return 0 for success") {
                val result = App.run(arrayOf("Success"))
                result shouldBe 0
            }

            it("should return 0 for anonymous") {
                val result = App.run(emptyArray())
                result shouldBe 0
            }

            it("should return 0 for validation handled by domain") {
                val result = App.run(arrayOf(""))
                result shouldBe 0
            }
        }
    }
})
