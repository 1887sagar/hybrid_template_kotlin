////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.bootstrap

import kotlin.system.exitProcess

/**
 * Main entry point for the hybrid architecture application.
 *
 * ## What is This Entry Point?
 * This is the primary entry point that demonstrates modern async Kotlin patterns:
 * - Uses suspend main function for natural async/await support
 * - Integrates with proper signal handling and graceful shutdown
 * - Returns meaningful exit codes to the operating system
 * - Supports both blocking and non-blocking execution modes
 *
 * ## Why Suspend Main?
 * Modern Kotlin applications benefit from suspend main because:
 * ```kotlin
 * // Traditional approach (still works)
 * fun main(args: Array<String>) {
 *     runBlocking {
 *         // All async work must be inside runBlocking
 *         val result = someAsyncOperation()
 *         println(result)
 *     }
 * }
 *
 * // Modern suspend main approach
 * suspend fun main(args: Array<String>) {
 *     // Direct async calls without wrapper
 *     val result = someAsyncOperation()
 *     println(result)
 * }
 * ```
 *
 * ## Application Lifecycle
 * 1. **Startup**: Parse command-line arguments securely
 * 2. **Initialization**: Build dependency graph via CompositionRoot
 * 3. **Execution**: Run the application with proper error handling
 * 4. **Shutdown**: Handle signals gracefully and cleanup resources
 * 5. **Exit**: Return appropriate exit code to OS
 *
 * ## Signal Handling Integration
 * This entry point automatically handles Unix signals:
 * - **SIGTERM**: Graceful shutdown request
 * - **SIGINT**: User interrupt (Ctrl+C)
 * - **SIGHUP**: Terminal disconnected
 *
 * ## Exit Code Meanings
 * The application returns standard Unix exit codes:
 * ```
 * 0   → Success (everything worked perfectly)
 * 1   → General error (configuration or runtime issue)
 * 2   → Uncaught exception (unexpected error)
 * 130 → Interrupted by user (Ctrl+C)
 * 137 → Killed by system (out of memory)
 * ```
 *
 * ## Usage Examples
 * ```bash
 * # Basic greeting
 * java -jar hybrid-app.jar "Alice"
 *
 * # Verbose output to console
 * java -jar hybrid-app.jar --verbose "Bob"
 *
 * # Save output to file
 * java -jar hybrid-app.jar --out=greetings.txt "Charlie"
 *
 * # Combination of options
 * java -jar hybrid-app.jar --verbose --out=output.txt "Diana"
 *
 * # Show help
 * java -jar hybrid-app.jar --help
 * ```
 *
 * ## Integration with Build Tools
 * ```bash
 * # Gradle execution
 * ./gradlew run --args="--verbose Alice"
 *
 * # Direct Java execution
 * java -cp build/libs/hybrid-app.jar com.abitofhelp.hybrid.bootstrap.EntryPointKt Alice
 * ```
 *
 * ## Testing and CI/CD
 * Exit codes are crucial for automated environments:
 * ```yaml
 * # GitHub Actions example
 * - name: Test Application
 *   run: java -jar app.jar "TestUser"
 *   # Step fails if exit code != 0
 *
 * # Docker health check
 * HEALTHCHECK CMD java -jar app.jar --health || exit 1
 * ```
 *
 * ## Comparison with Other Entry Points
 * This application provides multiple entry points for different use cases:
 * - **EntryPoint.kt**: Modern async with suspend main (recommended)
 * - **AsyncApp.run()**: Direct async execution for embedding
 * - **AsyncApp.runAsync()**: Suspend function for coroutine contexts
 *
 * @param args Command-line arguments from the OS
 */
suspend fun main(args: Array<String>) {
    val exitCode = AsyncApp.runAsync(args)
    exitProcess(exitCode)
}
