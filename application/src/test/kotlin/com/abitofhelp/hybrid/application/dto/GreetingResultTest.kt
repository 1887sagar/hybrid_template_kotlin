/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.application.dto

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class GreetingResultTest : DescribeSpec({

    describe("GreetingResult") {

        it("should create result with greeting and recipient name") {
            val result = GreetingResult(greeting = "Hello, John!", recipientName = "John")
            result.greeting shouldBe "Hello, John!"
            result.recipientName shouldBe "John"
        }

        it("should create result with anonymous recipient") {
            val result = GreetingResult(greeting = "Hello, Anonymous!", recipientName = "Anonymous")
            result.greeting shouldBe "Hello, Anonymous!"
            result.recipientName shouldBe "Anonymous"
        }

        it("should have correct toString") {
            val result = GreetingResult(greeting = "Hello, World!", recipientName = "World")
            result.toString() shouldBe "GreetingResult(greeting=Hello, World!, recipientName=World)"
        }

        it("should have correct equals") {
            val result1 = GreetingResult(greeting = "Hello", recipientName = "John")
            val result2 = GreetingResult(greeting = "Hello", recipientName = "John")
            val result3 = GreetingResult(greeting = "Hello", recipientName = "Jane")
            val result4 = GreetingResult(greeting = "Goodbye", recipientName = "John")

            result1 shouldBe result2
            result1 shouldNotBe result3
            result1 shouldNotBe result4
        }

        it("should have correct hashCode") {
            val result1 = GreetingResult(greeting = "Hello", recipientName = "John")
            val result2 = GreetingResult(greeting = "Hello", recipientName = "John")

            result1.hashCode() shouldBe result2.hashCode()
        }

        it("should support copy") {
            val original = GreetingResult(greeting = "Hello", recipientName = "John")
            val copy1 = original.copy(greeting = "Goodbye")
            val copy2 = original.copy(recipientName = "Jane")

            copy1.greeting shouldBe "Goodbye"
            copy1.recipientName shouldBe "John"
            copy2.greeting shouldBe "Hello"
            copy2.recipientName shouldBe "Jane"
            original.greeting shouldBe "Hello"
            original.recipientName shouldBe "John"
        }

        it("should support destructuring") {
            val result = GreetingResult(greeting = "Hello, World!", recipientName = "World")
            val (greeting, recipientName) = result

            greeting shouldBe "Hello, World!"
            recipientName shouldBe "World"
        }
    }
})
