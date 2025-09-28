/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.infrastructure.adapter.output

import arrow.core.right
import com.abitofhelp.hybrid.application.error.ApplicationError
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ConsoleOutputAdapterTest : DescribeSpec({

    describe("ConsoleOutputAdapter") {

        val adapter = ConsoleOutputAdapter()

        describe("send") {

            context("when sending valid messages") {
                it("should send message to console successfully") {
                    runTest {
                        // Capture console output
                        val outputStream = ByteArrayOutputStream()
                        val originalOut = System.out
                        System.setOut(PrintStream(outputStream))

                        try {
                            // Given
                            val message = "Hello, World!"

                            // When
                            val result = adapter.send(message)

                            // Then
                            result shouldBe Unit.right()
                            outputStream.toString() shouldContain message
                        } finally {
                            // Restore original console
                            System.setOut(originalOut)
                        }
                    }
                }

                it("should handle multi-line messages") {
                    runTest {
                        // Capture console output
                        val outputStream = ByteArrayOutputStream()
                        val originalOut = System.out
                        System.setOut(PrintStream(outputStream))

                        try {
                            // Given
                            val message = "Line 1\nLine 2\nLine 3"

                            // When
                            val result = adapter.send(message)

                            // Then
                            result shouldBe Unit.right()
                            val output = outputStream.toString()
                            output shouldContain "Line 1"
                            output shouldContain "Line 2"
                            output shouldContain "Line 3"
                        } finally {
                            // Restore original console
                            System.setOut(originalOut)
                        }
                    }
                }

                it("should handle special characters") {
                    runTest {
                        // Capture console output
                        val outputStream = ByteArrayOutputStream()
                        val originalOut = System.out
                        System.setOut(PrintStream(outputStream))

                        try {
                            // Given
                            val message = "Hello! @#$%^&*() 你好 🎉"

                            // When
                            val result = adapter.send(message)

                            // Then
                            result shouldBe Unit.right()
                            outputStream.toString() shouldContain message
                        } finally {
                            // Restore original console
                            System.setOut(originalOut)
                        }
                    }
                }
            }

            context("when sending invalid messages") {
                it("should return error for empty message") {
                    runTest {
                        // Given
                        val message = ""

                        // When
                        val result = adapter.send(message)

                        // Then
                        result.isLeft() shouldBe true
                        val error = result.leftOrNull()!!
                        error.shouldBeInstanceOf<ApplicationError.OutputError>()
                        error.message shouldBe "Invalid message: Message cannot be blank"
                    }
                }

                it("should return error for blank message") {
                    runTest {
                        // Given
                        val message = "   "

                        // When
                        val result = adapter.send(message)

                        // Then
                        result.isLeft() shouldBe true
                        val error = result.leftOrNull()!!
                        error.shouldBeInstanceOf<ApplicationError.OutputError>()
                        error.message shouldBe "Invalid message: Message cannot be blank"
                    }
                }
            }

            context("when console operations fail") {
                it("should handle exception during print") {
                    runTest {
                        // Mock System.out to throw exception
                        val mockPrintStream = mockk<PrintStream>()
                        val originalOut = System.out
                        System.setOut(mockPrintStream)

                        try {
                            // Given
                            val message = "Test message"
                            val exception = RuntimeException("Console error")
                            every { mockPrintStream.println(message) } throws exception

                            // When
                            val result = adapter.send(message)

                            // Then
                            result.isLeft() shouldBe true
                            val error = result.leftOrNull()!!
                            error.shouldBeInstanceOf<ApplicationError.OutputError>()
                            error.message shouldContain "Unexpected error writing to console"
                        } finally {
                            // Restore original console
                            System.setOut(originalOut)
                        }
                    }
                }
            }
        }

        describe("interface implementation") {
            it("should implement OutputPort") {
                adapter.shouldBeInstanceOf<com.abitofhelp.hybrid.application.port.output.OutputPort>()
            }
        }
    }
})
