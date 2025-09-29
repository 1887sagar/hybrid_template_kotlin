// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - ExitCode
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.bootstrap

/**
 * Standard exit codes following Unix conventions and BSD sysexits.h.
 *
 * Centralizes all application exit codes to improve readability,
 * maintainability, and consistency across the application.
 *
 * @param code The numeric exit code
 * @since 1.0.0
 */
@Suppress("MagicNumber") // Exit codes are well-known Unix constants
enum class ExitCode(val code: Int) {
    /** Successful execution */
    SUCCESS(0),

    /** General error */
    ERROR(1),

    /** Misuse of shell builtins (according to Bash) */
    MISUSE(2),

    /** Command invoked cannot execute */
    CANNOT_EXECUTE(126),

    /** Command not found */
    COMMAND_NOT_FOUND(127),

    /** Invalid exit argument */
    INVALID_EXIT_ARG(128),

    /** Fatal error signal "n" (130 = 128 + SIGINT) */
    SIGINT(130),

    /** Command line usage error */
    EX_USAGE(64),

    /** Data format error */
    EX_DATAERR(65),

    /** Cannot open input */
    EX_NOINPUT(66),

    /** Service unavailable */
    EX_UNAVAILABLE(69),

    /** Internal software error */
    EX_SOFTWARE(70),

    /** System error (e.g., can't fork) */
    EX_OSERR(71),

    /** Critical OS file missing */
    EX_OSFILE(72),

    /** Can't create (user) output file */
    EX_CANTCREAT(73),

    /** Input/output error */
    EX_IOERR(74),

    /** Permission denied */
    EX_NOPERM(77),

    /** Configuration error */
    EX_CONFIG(78),
    ;

    companion object {
        /**
         * Maps common application errors to appropriate exit codes.
         *
         * @param throwable The exception to map
         * @return Appropriate exit code for the error
         */
        fun fromThrowable(throwable: Throwable): ExitCode = when (throwable) {
            is IllegalArgumentException -> EX_USAGE
            is java.nio.file.AccessDeniedException -> EX_NOPERM
            is java.nio.file.NoSuchFileException -> EX_NOINPUT
            is java.io.IOException -> EX_IOERR
            is SecurityException -> EX_NOPERM
            is OutOfMemoryError -> EX_OSERR
            else -> ERROR
        }

        /**
         * Maps domain/application error types to exit codes.
         * Extend this as needed for your specific error types.
         */
        fun fromErrorType(errorType: String): ExitCode = when (errorType) {
            "ValidationError" -> EX_DATAERR
            "ConfigurationError" -> EX_CONFIG
            "PermissionError" -> EX_NOPERM
            "FileNotFoundError" -> EX_NOINPUT
            "FileCreationError" -> EX_CANTCREAT
            else -> ERROR
        }
    }
}
