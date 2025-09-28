// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.bootstrap

object App {
    fun run(args: Array<String>): Int {
        return try {
            val cfg = parseArgs(args)
            val program = CompositionRoot.buildProgram(cfg) // returns () -> Int
            program()
        } catch (e: IllegalArgumentException) {
            // Configuration errors
            System.err.println("Configuration error: ${e.message}")
            ExitCodes.CONFIGURATION_ERROR
        } catch (e: IllegalStateException) {
            // State errors
            System.err.println("State error: ${e.message}")
            ExitCodes.STATE_ERROR
        } catch (e: SecurityException) {
            // Security errors
            System.err.println("Security error: ${e.message}")
            ExitCodes.PERMISSION_DENIED
        } catch (e: OutOfMemoryError) {
            // Memory errors - critical, log basic info to avoid further memory issues
            System.err.println("CRITICAL: Out of memory - ${e.message}")
            ExitCodes.OUT_OF_MEMORY
        } catch (e: Exception) {
            // Unexpected errors
            System.err.println("Fatal error: ${e.message ?: e.javaClass.simpleName}")
            // Log exception details to stderr instead of printStackTrace
            System.err.println("Exception type: ${e.javaClass.name}")
            ExitCodes.GENERAL_ERROR
        }
    }
}

/**
 * Standard exit codes following Unix conventions.
 */
object ExitCodes {
    const val SUCCESS = 0
    const val GENERAL_ERROR = 1
    const val CONFIGURATION_ERROR = 2
    const val STATE_ERROR = 3
    const val PERMISSION_DENIED = 126
    const val COMMAND_NOT_FOUND = 127
    const val INTERRUPTED = 130 // SIGINT (Ctrl+C)
    const val TERMINATED = 137 // SIGTERM
    const val OUT_OF_MEMORY = 137 // SIGKILL (often due to OOM)
}
