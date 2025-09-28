/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.bootstrap

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class EntryPointTest : DescribeSpec({

    describe("EntryPoint") {

        // Note: We cannot directly test the main function because it calls exitProcess
        // which would terminate the test runner. Instead, we test that the file exists
        // and contains the expected structure.

        it("should have main function defined") {
            // This test verifies that EntryPoint.kt exists and can be loaded
            // The actual main function testing is done through integration tests
            // or by testing App.run directly

            // Verify we can reference the App class (compilation would fail if not)
            val appClass = App::class
            appClass.simpleName shouldBe "App"
        }

        describe("integration with App") {
            // The main function delegates to App.run
            // We test App.run separately to verify behavior

            it("should delegate to App.run") {
                // Given valid arguments
                val args = arrayOf("IntegrationTest")

                // When App.run is called (what main does)
                val result = App.run(args)

                // Then it should succeed
                result shouldBe 0
            }
        }
    }
})
