// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.bootstrap

import kotlin.system.exitProcess

/**
 * Async main entry point with proper coroutine and signal handling.
 * The application is fully async with signal handling and graceful shutdown.
 */
suspend fun main(args: Array<String>) {
    val exitCode = AsyncApp.runAsync(args)
    exitProcess(exitCode)
}
