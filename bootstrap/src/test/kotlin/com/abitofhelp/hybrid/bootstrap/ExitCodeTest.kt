// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - ExitCode Tests
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.bootstrap

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.NoSuchFileException

/**
 * Tests for ExitCode enum and its mapping utilities.
 *
 * Verifies that exit codes follow Unix conventions and that
 * exception-to-exit-code mapping works correctly.
 */
class ExitCodeTest : DescribeSpec({

    describe("ExitCode enum") {
        it("should have correct numeric values for standard codes") {
            ExitCode.SUCCESS.code shouldBe 0
            ExitCode.ERROR.code shouldBe 1
            ExitCode.EX_USAGE.code shouldBe 64
            ExitCode.EX_NOPERM.code shouldBe 77
            ExitCode.SIGINT.code shouldBe 130
        }
    }

    describe("fromThrowable mapping") {
        it("should map IllegalArgumentException to EX_USAGE") {
            val exception = IllegalArgumentException("Invalid argument")
            ExitCode.fromThrowable(exception) shouldBe ExitCode.EX_USAGE
        }

        it("should map AccessDeniedException to EX_NOPERM") {
            val exception = AccessDeniedException("Permission denied")
            ExitCode.fromThrowable(exception) shouldBe ExitCode.EX_NOPERM
        }

        it("should map NoSuchFileException to EX_NOINPUT") {
            val exception = NoSuchFileException("File not found")
            ExitCode.fromThrowable(exception) shouldBe ExitCode.EX_NOINPUT
        }

        it("should map IOException to EX_IOERR") {
            val exception = IOException("I/O error")
            ExitCode.fromThrowable(exception) shouldBe ExitCode.EX_IOERR
        }

        it("should map SecurityException to EX_NOPERM") {
            val exception = SecurityException("Security violation")
            ExitCode.fromThrowable(exception) shouldBe ExitCode.EX_NOPERM
        }

        it("should map OutOfMemoryError to EX_OSERR") {
            val exception = OutOfMemoryError("Out of memory")
            ExitCode.fromThrowable(exception) shouldBe ExitCode.EX_OSERR
        }

        it("should map unknown exceptions to ERROR") {
            val exception = RuntimeException("Unknown error")
            ExitCode.fromThrowable(exception) shouldBe ExitCode.ERROR
        }
    }

    describe("fromErrorType mapping") {
        it("should map ValidationError to EX_DATAERR") {
            ExitCode.fromErrorType("ValidationError") shouldBe ExitCode.EX_DATAERR
        }

        it("should map ConfigurationError to EX_CONFIG") {
            ExitCode.fromErrorType("ConfigurationError") shouldBe ExitCode.EX_CONFIG
        }

        it("should map PermissionError to EX_NOPERM") {
            ExitCode.fromErrorType("PermissionError") shouldBe ExitCode.EX_NOPERM
        }

        it("should map FileNotFoundError to EX_NOINPUT") {
            ExitCode.fromErrorType("FileNotFoundError") shouldBe ExitCode.EX_NOINPUT
        }

        it("should map FileCreationError to EX_CANTCREAT") {
            ExitCode.fromErrorType("FileCreationError") shouldBe ExitCode.EX_CANTCREAT
        }

        it("should map unknown error types to ERROR") {
            ExitCode.fromErrorType("UnknownError") shouldBe ExitCode.ERROR
        }
    }
})
