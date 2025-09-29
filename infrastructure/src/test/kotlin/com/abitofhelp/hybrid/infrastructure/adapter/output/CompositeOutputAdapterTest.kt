/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.infrastructure.adapter.output

import arrow.core.left
import arrow.core.right
import com.abitofhelp.hybrid.application.error.ApplicationError
import com.abitofhelp.hybrid.application.port.output.OutputPort
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

class CompositeOutputAdapterTest : DescribeSpec({

    describe("CompositeOutputAdapter") {

        describe("send") {

            it("should send to all outputs concurrently") {
                runTest {
                    // Given
                    val output1 = mockk<OutputPort>()
                    val output2 = mockk<OutputPort>()
                    val output3 = mockk<OutputPort>()
                    val message = "Test message"

                    coEvery { output1.send(message) } returns Unit.right()
                    coEvery { output2.send(message) } returns Unit.right()
                    coEvery { output3.send(message) } returns Unit.right()

                    val adapter = CompositeOutputAdapter(listOf(output1, output2, output3))

                    // When
                    val result = adapter.send(message)

                    // Then
                    result.isRight() shouldBe true
                    coVerify(exactly = 1) {
                        output1.send(message)
                        output2.send(message)
                        output3.send(message)
                    }
                }
            }

            it("should handle partial failures") {
                runTest {
                    // Given
                    val output1 = mockk<OutputPort>()
                    val output2 = mockk<OutputPort>()
                    val output3 = mockk<OutputPort>()
                    val message = "Test message"

                    coEvery { output1.send(message) } returns Unit.right()
                    coEvery { output2.send(message) } returns ApplicationError.OutputError("Failed").left()
                    coEvery { output3.send(message) } returns Unit.right()

                    val adapter = CompositeOutputAdapter(listOf(output1, output2, output3))

                    // When
                    val result = adapter.send(message)

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            (error is ApplicationError.OutputError) shouldBe true
                            if (error is ApplicationError.OutputError) {
                                error.message shouldContain "Failed to send to 1 of 3 outputs"
                                error.message shouldContain "Failed"
                            }
                        },
                        { },
                    )

                    // All outputs should still be attempted
                    coVerify(exactly = 1) {
                        output1.send(message)
                        output2.send(message)
                        output3.send(message)
                    }
                }
            }

            it("should handle all failures") {
                runTest {
                    // Given
                    val output1 = mockk<OutputPort>()
                    val output2 = mockk<OutputPort>()
                    val message = "Test message"

                    coEvery { output1.send(message) } returns ApplicationError.OutputError("Error 1").left()
                    coEvery { output2.send(message) } returns ApplicationError.OutputError("Error 2").left()

                    val adapter = CompositeOutputAdapter(listOf(output1, output2))

                    // When
                    val result = adapter.send(message)

                    // Then
                    result.isLeft() shouldBe true
                    result.fold(
                        { error ->
                            (error as ApplicationError.OutputError).message shouldContain
                                "Failed to send to 2 of 2 outputs"
                            error.message shouldContain "Error 1"
                            error.message shouldContain "Error 2"
                        },
                        { },
                    )
                }
            }
        }

        describe("createDefault") {

            it("should create console-only adapter when no file path") {
                // When
                val adapter = CompositeOutputAdapter.createDefault(null)

                // Then
                adapter::class.simpleName shouldBe "ConsoleOutputAdapter"
            }

            it("should create composite adapter when file path provided") {
                // When
                val adapter = CompositeOutputAdapter.createDefault("/tmp/test.txt")

                // Then
                adapter::class.simpleName shouldBe "CompositeOutputAdapter"
            }
        }
    }
})
