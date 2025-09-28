/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.application.port.output

import arrow.core.left
import arrow.core.right
import com.abitofhelp.hybrid.application.error.ApplicationError
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

class OutputPortTest : DescribeSpec({

    describe("OutputPort") {

        it("should be an interface") {
            val port = mockk<OutputPort>()
            port shouldBe port // Just verify it compiles as interface
        }

        it("should define send method returning success") {
            runTest {
                // Given
                val port = mockk<OutputPort>()
                val message = "Test message"

                coEvery { port.send(message) } returns Unit.right()

                // When
                val result = port.send(message)

                // Then
                result shouldBe Unit.right()
            }
        }

        it("should define send method returning error") {
            runTest {
                // Given
                val port = mockk<OutputPort>()
                val message = "Test message"
                val error = ApplicationError.OutputError("Failed to send")

                coEvery { port.send(message) } returns error.left()

                // When
                val result = port.send(message)

                // Then
                result shouldBe error.left()
            }
        }
    }
})
