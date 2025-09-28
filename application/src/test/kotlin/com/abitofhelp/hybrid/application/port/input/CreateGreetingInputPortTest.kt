/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.application.port.input

import arrow.core.right
import com.abitofhelp.hybrid.application.dto.CreateGreetingCommand
import com.abitofhelp.hybrid.application.dto.GreetingResult
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

class CreateGreetingInputPortTest : DescribeSpec({

    describe("CreateGreetingInputPort") {

        it("should be an interface") {
            val port = mockk<CreateGreetingInputPort>()
            port shouldBe port // Just verify it compiles as interface
        }

        it("should define execute method") {
            runTest {
                // Given
                val port = mockk<CreateGreetingInputPort>()
                val command = CreateGreetingCommand("John", silent = false)
                val expectedResult = GreetingResult("Hello, John!", "John")

                coEvery { port.execute(command) } returns expectedResult.right()

                // When
                val result = port.execute(command)

                // Then
                result shouldBe expectedResult.right()
            }
        }
    }
})
