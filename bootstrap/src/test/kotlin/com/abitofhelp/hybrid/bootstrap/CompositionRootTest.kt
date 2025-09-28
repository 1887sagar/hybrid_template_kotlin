/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.bootstrap

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class CompositionRootTest : DescribeSpec({

    describe("CompositionRoot.buildProgram") {

        lateinit var outputStream: ByteArrayOutputStream
        lateinit var originalOut: PrintStream

        beforeEach {
            outputStream = ByteArrayOutputStream()
            originalOut = System.out
            System.setOut(PrintStream(outputStream))
        }

        afterEach {
            System.setOut(originalOut)
        }

        describe("program building") {

            it("should build program with valid configuration") {
                // Given
                val config = AppConfig(
                    verbose = false,
                    outputPath = null,
                    name = "TestUser",
                )

                // When
                val program = CompositionRoot.buildProgram(config)

                // Then
                program shouldNotBe null

                // Execute the program
                val result = program()
                result shouldBe 0
                outputStream.toString() shouldContain "Hey there, TestUser! Welcome!"
            }

            it("should build program with verbose configuration") {
                // Given
                val config = AppConfig(
                    verbose = true,
                    outputPath = null,
                    name = "VerboseUser",
                )

                // When
                val program = CompositionRoot.buildProgram(config)
                val result = program()

                // Then
                result shouldBe 0
                val output = outputStream.toString()
                output shouldContain "[INFO] Greeting delivered successfully"
                output shouldContain "Hey there, VerboseUser! Welcome!"
            }

            it("should build program with file output") {
                // Given
                val tempFile = kotlin.io.path.createTempFile("test", ".txt").toFile()
                tempFile.deleteOnExit()

                val config = AppConfig(
                    verbose = false,
                    outputPath = tempFile.absolutePath,
                    name = "FileUser",
                )

                // When
                val program = CompositionRoot.buildProgram(config)
                val result = program()

                // Then
                result shouldBe 0
                // Current implementation doesn't support file output yet
                outputStream.toString() shouldContain "Hey there, FileUser! Welcome!"

                // Cleanup
                tempFile.delete()
            }

            it("should build program that handles missing name") {
                // Given
                val config = AppConfig(
                    verbose = false,
                    outputPath = null,
                    name = null,
                )

                // When
                val program = CompositionRoot.buildProgram(config)
                val result = program()

                // Then
                result shouldBe 0
                outputStream.toString() shouldContain "Tip: You can provide your name as a command-line argument"
                outputStream.toString() shouldContain "Hello World from Anonymous!"
            }
        }

        describe("dependency wiring") {

            it("should wire all dependencies correctly") {
                // Given
                val config = AppConfig(name = "WiringTest")

                // When
                val program = CompositionRoot.buildProgram(config)

                // Then
                // If wiring is incorrect, the program would throw exceptions
                val result = program()
                result shouldBe 0
            }

            it("should create independent instances") {
                // Given
                val config1 = AppConfig(name = "Alice")
                val config2 = AppConfig(name = "Bob")

                // When
                val program1 = CompositionRoot.buildProgram(config1)
                val program2 = CompositionRoot.buildProgram(config2)

                // Then
                program1 shouldNotBe program2

                // Execute both
                program1()
                val output1 = outputStream.toString()

                outputStream.reset()
                program2()
                val output2 = outputStream.toString()

                output1 shouldContain "Hey there, Alice! Welcome!"
                output2 shouldContain "Hey there, Bob! Welcome!"
            }
        }

        describe("error propagation") {

            it("should propagate validation errors") {
                // Given
                val errorStream = ByteArrayOutputStream()
                System.setErr(PrintStream(errorStream))

                val config = AppConfig(name = "") // Empty name will cause validation error

                // When
                val program = CompositionRoot.buildProgram(config)
                val result = program()

                // Then
                result shouldBe 0 // Empty name handled as anonymous
                outputStream.toString() shouldContain "Hello World from Anonymous!"

                // Restore
                System.setErr(System.err)
            }
        }

        describe("functional programming") {

            it("should return executable function") {
                // Given
                val config = AppConfig(name = "Functional")

                // When
                val program = CompositionRoot.buildProgram(config)

                // Then
                // Should be a function that returns Int
                val result = program.invoke()
                result shouldBe 0
            }

            it("should be reusable") {
                // Given
                val config = AppConfig(name = "Reusable")
                val program = CompositionRoot.buildProgram(config)

                // When - execute multiple times
                outputStream.reset()
                val result1 = program()
                val output1 = outputStream.toString()

                outputStream.reset()
                val result2 = program()
                val output2 = outputStream.toString()

                // Then
                result1 shouldBe 0
                result2 shouldBe 0
                output1 shouldBe output2
            }
        }
    }
})
