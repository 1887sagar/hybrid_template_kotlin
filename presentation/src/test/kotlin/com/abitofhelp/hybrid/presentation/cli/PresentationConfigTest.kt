/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.presentation.cli

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class PresentationConfigTest : DescribeSpec({

    describe("PresentationConfig") {

        describe("creation") {
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
            it("should have correct toString") {
                val config = PresentationConfig(
                    verbose = true,
                    outputPath = "out.txt",
                    name = "Alice",
                )

                config.toString() shouldBe "PresentationConfig(verbose=true, outputPath=out.txt, name=Alice)"
            }

            it("should have correct equals") {
                val config1 = PresentationConfig(true, "out.txt", "Bob")
                val config2 = PresentationConfig(true, "out.txt", "Bob")
                val config3 = PresentationConfig(false, "out.txt", "Bob")

                config1 shouldBe config2
                config1 shouldNotBe config3
            }

            it("should have correct hashCode") {
                val config1 = PresentationConfig(true, "out.txt", "Charlie")
                val config2 = PresentationConfig(true, "out.txt", "Charlie")

                config1.hashCode() shouldBe config2.hashCode()
            }

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
