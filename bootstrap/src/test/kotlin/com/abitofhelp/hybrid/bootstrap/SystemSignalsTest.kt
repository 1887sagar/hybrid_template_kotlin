// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - SystemSignals Tests
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.bootstrap

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tests for SystemSignals implementations.
 *
 * These tests verify the signal handling abstractions work correctly
 * and provide testable alternatives to direct signal handling.
 */
class SystemSignalsTest : DescribeSpec({

    describe("NoOpSignals") {
        it("should do nothing when install is called") {
            val signals = NoOpSignals()
            val called = AtomicBoolean(false)

            // Should not throw and callback should not be called
            signals.install { called.set(true) }

            called.get() shouldBe false
        }
    }

    describe("JvmSignals") {
        it("should install shutdown hook without internal signals") {
            val signals = JvmSignals(useInternalSignalApi = false)
            val called = AtomicBoolean(false)

            // Should install shutdown hook successfully
            signals.install { called.set(true) }

            // We can't easily test shutdown hooks in unit tests,
            // but we can verify the call doesn't throw
            called.get() shouldBe false // Hook not triggered yet
        }

        it("should handle internal signal API gracefully when enabled") {
            val signals = JvmSignals(useInternalSignalApi = true)
            val called = AtomicBoolean(false)

            // Should not throw even if internal API fails
            signals.install { called.set(true) }

            // The call should succeed regardless of platform
            called.get() shouldBe false // Signals not triggered in test
        }
    }
})
