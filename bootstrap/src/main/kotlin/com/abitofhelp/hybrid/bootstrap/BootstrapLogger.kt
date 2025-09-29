// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template - Bootstrap Logger
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.bootstrap

/**
 * Logger interface for bootstrap-level lifecycle messages.
 *
 * This interface provides a clean abstraction for logging during application
 * startup, shutdown, and lifecycle events. Unlike application-level ports,
 * this is specifically for infrastructure concerns during bootstrap.
 *
 * @since 1.0.0
 */
interface BootstrapLogger {
    /**
     * Logs an informational message.
     *
     * @param message The message to log
     */
    fun info(message: String)

    /**
     * Logs an error message.
     *
     * @param message The error message to log
     */
    fun error(message: String)

    /**
     * Logs an error with exception details.
     *
     * @param message The error message
     * @param throwable The exception that occurred
     */
    fun error(message: String, throwable: Throwable)
}

/**
 * Console implementation of BootstrapLogger.
 *
 * Routes info messages to stdout and error messages to stderr,
 * following standard Unix conventions.
 *
 * @since 1.0.0
 */
class ConsoleBootstrapLogger : BootstrapLogger {
    override fun info(message: String) {
        println(message)
    }

    override fun error(message: String) {
        System.err.println(message)
    }

    override fun error(message: String, throwable: Throwable) {
        System.err.println(message)
        throwable.printStackTrace()
    }
}

/**
 * No-operation implementation for testing.
 *
 * @since 1.0.0
 */
class NoOpBootstrapLogger : BootstrapLogger {
    override fun info(message: String) {
        // Intentionally do nothing
    }

    override fun error(message: String) {
        // Intentionally do nothing
    }

    override fun error(message: String, throwable: Throwable) {
        // Intentionally do nothing
    }
}
