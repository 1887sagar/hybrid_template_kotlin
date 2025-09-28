/*
 * Kotlin Hybrid Architecture Template - Test Suite
 * Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
 * SPDX-License-Identifier: BSD-3-Clause
 * See LICENSE file in the project root.
 */

package com.abitofhelp.hybrid.domain.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GreetingFormatTest : DescribeSpec({

    describe("GreetingFormat") {

        it("should have DEFAULT format") {
            GreetingFormat.DEFAULT shouldBe GreetingFormat.DEFAULT
        }

        it("should have FRIENDLY format") {
            GreetingFormat.FRIENDLY shouldBe GreetingFormat.FRIENDLY
        }

        it("should have FORMAL format") {
            GreetingFormat.FORMAL shouldBe GreetingFormat.FORMAL
        }

        it("should have exactly 3 formats") {
            GreetingFormat.values().size shouldBe 3
        }

        it("should find format by name") {
            GreetingFormat.valueOf("DEFAULT") shouldBe GreetingFormat.DEFAULT
            GreetingFormat.valueOf("FRIENDLY") shouldBe GreetingFormat.FRIENDLY
            GreetingFormat.valueOf("FORMAL") shouldBe GreetingFormat.FORMAL
        }
    }
})
