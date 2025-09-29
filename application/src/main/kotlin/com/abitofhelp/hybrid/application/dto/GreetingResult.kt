////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.application.dto

/**
 * Result of successfully creating a greeting.
 *
 * ## What is a Result DTO?
 * A result DTO carries data from the application layer back to the presentation layer.
 * It contains not just the main result (the greeting) but also useful metadata.
 *
 * ## Why Include Metadata?
 * Including the recipient name separately allows the presentation layer to:
 * - Display it differently (e.g., in a UI title)
 * - Log it for analytics
 * - Use it for further processing
 *
 * Without having to parse it from the greeting string!
 *
 * ## Example Usage
 * ```kotlin
 * // In your use case
 * val result = GreetingResult(
 *     greeting = "Hey there, Alice! Welcome!",
 *     recipientName = "Alice"
 * )
 *
 * // In your presentation layer
 * greetingUseCase.execute(command).fold(
 *     { error -> showError(error) },
 *     { result ->
 *         // Can use the data separately
 *         titleLabel.text = "Greeting for: ${result.recipientName}"
 *         messageLabel.text = result.greeting
 *
 *         // Or together
 *         console.println(result.greeting)
 *     }
 * )
 * ```
 *
 * ## Design Tip
 * Result DTOs can grow to include more metadata as needed:
 * - Timestamp when created
 * - Which greeting format was used
 * - Language/locale information
 * - Performance metrics
 *
 * Just add new properties without breaking existing code!
 *
 * @property greeting The complete greeting message ready for display
 * @property recipientName The name used in the greeting (might be "Anonymous")
 */
data class GreetingResult(
    /**
     * The formatted greeting message, ready to display.
     * Examples: "Hey there, Alice! Welcome!" or "Hello World from Anonymous!"
     */
    val greeting: String,

    /**
     * The recipient's name as used in the greeting.
     * This will be "Anonymous" if no name was provided.
     * Useful for logging, analytics, or UI display.
     */
    val recipientName: String,
)
