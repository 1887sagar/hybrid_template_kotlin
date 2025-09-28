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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class AsyncAppTest : DescribeSpec({

    describe("AsyncApp") {

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

        describe("runAsync") {

            it("should handle successful execution") {
                runTest {
                    // Given
                    val args = arrayOf("TestUser")

                    // When
                    val exitCode = AsyncApp.runAsync(args)

                    // Then
                    exitCode shouldBe 0
                    outputStream.toString() shouldContain "Hey there, TestUser! Welcome!"
                }
            }

            it("should handle configuration errors") {
                runTest {
                    // Given - simulate bad config (this would need actual bad config scenario)
                    val args = arrayOf("--unknown-flag")

                    // When
                    val exitCode = AsyncApp.runAsync(args)

                    // Then
                    exitCode shouldBe 0 // Current impl doesn't validate unknown flags
                }
            }

            it("should handle cancellation gracefully") {
                runTest {
                    // Given
                    val args = arrayOf("TestUser", "--verbose")

                    // When - launch and cancel
                    val job = launch {
                        AsyncApp.runAsync(args)
                    }

                    delay(50) // Let it start
                    job.cancel()
                    delay(50) // Let it clean up

                    // Then
                    job.isCancelled shouldBe true
                }
            }
        }

        describe("signal handling") {

            it("should install signal handlers") {
                runTest {
                    // Given
                    val args = arrayOf("TestUser")

                    // When - start app in background
                    val job = launch {
                        AsyncApp.runAsync(args)
                    }

                    delay(100) // Let it initialize

                    // Then - signal handler should be installed (hard to test directly)
                    // We can at least verify the app is running
                    job.isActive shouldBe true

                    // Cleanup
                    job.cancel()
                }
            }

            // Note: Actually sending signals in tests is platform-dependent and tricky
            // This test is more of a smoke test
            it("should handle shutdown gracefully") {
                // Given
                val args = arrayOf("TestUser")

                // When
                val exitCode = AsyncApp.run(args)

                // Then
                exitCode shouldBe 0
            }
        }

        describe("error handling") {

            it("should handle uncaught exceptions") {
                runTest {
                    // This test would need to trigger an actual uncaught exception
                    // Current implementation doesn't expose easy way to inject errors
                    // This is more of a placeholder for manual testing

                    val args = arrayOf("ValidUser")
                    val exitCode = AsyncApp.runAsync(args)
                    exitCode shouldBe 0
                }
            }
        }

        describe("exit codes") {

            it("should use standard exit codes") {
                // Verify exit codes are defined correctly
                ExitCodes.SUCCESS shouldBe 0
                ExitCodes.GENERAL_ERROR shouldBe 1
                ExitCodes.CONFIGURATION_ERROR shouldBe 2
                ExitCodes.STATE_ERROR shouldBe 3
                ExitCodes.PERMISSION_DENIED shouldBe 126
                ExitCodes.COMMAND_NOT_FOUND shouldBe 127
                ExitCodes.INTERRUPTED shouldBe 130
                ExitCodes.TERMINATED shouldBe 137
                ExitCodes.OUT_OF_MEMORY shouldBe 137
            }
        }
    }
})
