////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.presentation.cli

/**
 * Configuration for the presentation layer.
 *
 * ## Purpose
 * This data class holds all configuration needed by the presentation layer,
 * keeping it separate from:
 * - Application logic (commands)
 * - Infrastructure details (file handling)
 * - Business rules (validation)
 *
 * ## Why a Data Class?
 * Using a data class provides:
 * - Immutability (all properties are val)
 * - Automatic equals/hashCode for comparisons
 * - toString() for debugging
 * - copy() for creating modified versions
 *
 * ## Configuration Sources
 * In a real application, this could be populated from:
 * - Command-line arguments
 * - Environment variables
 * - Configuration files
 * - Default values
 *
 * ## Example Usage
 * ```kotlin
 * // From command-line args
 * val config = PresentationConfig(
 *     verbose = args.contains("--verbose"),
 *     outputPath = args.find { it.startsWith("--output=") }
 *                     ?.substringAfter("="),
 *     name = args.firstOrNull { !it.startsWith("--") }
 * )
 *
 * // Create modified version
 * val quietConfig = config.copy(verbose = false)
 *
 * // Use in presentation logic
 * if (config.verbose) {
 *     println("[DEBUG] Starting application...")
 * }
 * ```
 *
 * @property verbose Whether to show detailed logging information
 * @property outputPath Optional file path for output (null = console only)
 * @property name Optional name to greet (null = anonymous user)
 */
data class PresentationConfig(
    /**
     * Controls verbose output for debugging.
     * - true: Show detailed logs and progress information
     * - false: Show only essential output
     *
     * Useful for troubleshooting or understanding program flow.
     */
    val verbose: Boolean,

    /**
     * Optional file path where output should be written.
     * - null: Output goes only to console
     * - Path string: Output goes to both console and file
     *
     * The infrastructure layer handles file creation and permissions.
     */
    val outputPath: String?,

    /**
     * The name to use for greeting generation.
     * - null: Will use "Anonymous" as default
     * - Empty string: Will be validated and might cause error
     * - Valid name: Will be used in the greeting
     *
     * Validation happens in the domain layer, not here.
     */
    val name: String?,
)
