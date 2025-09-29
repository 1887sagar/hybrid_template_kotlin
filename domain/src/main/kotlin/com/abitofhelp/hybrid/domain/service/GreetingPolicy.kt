////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.domain.service

import com.abitofhelp.hybrid.domain.value.PersonName

/**
 * Domain service that encapsulates greeting business rules and policies.
 *
 * ## What is a Domain Service?
 * A domain service contains business logic that doesn't naturally fit inside a single entity
 * or value object. It represents operations or business rules that involve multiple domain
 * objects or complex calculations.
 *
 * ## Why Use Object Instead of Class?
 * `object` in Kotlin creates a singleton - a single instance that's shared everywhere.
 * This is perfect for stateless services that only contain business logic.
 *
 * ## What This Service Does
 * This service decides how to greet users based on their name characteristics:
 * - Long names get formal greetings
 * - Anonymous users get default greetings
 * - Regular names get friendly greetings
 *
 * ## Example Usage
 * ```kotlin
 * val name = PersonName.create("John").getOrNull()!!
 * val format = GreetingPolicy.determineGreetingFormat(name)
 *
 * val greeting = when (format) {
 *     GreetingFormat.FRIENDLY -> "Hey there, ${name.value}!"
 *     GreetingFormat.FORMAL -> "Greetings, ${name.value}."
 *     GreetingFormat.DEFAULT -> "Hello, ${name.value}!"
 * }
 * ```
 */
object GreetingPolicy {
    /**
     * Names longer than this are considered "long" and receive formal treatment.
     * This is a business decision that could change based on requirements.
     */
    private const val LONG_NAME_THRESHOLD = 20

    /**
     * Determines the appropriate greeting format based on the person's name.
     *
     * This method contains the core business logic for greeting selection.
     * The rules are:
     * 1. Names longer than 20 characters → FORMAL (shows respect for complex names)
     * 2. "Anonymous" (case-insensitive) → DEFAULT (neutral greeting)
     * 3. All other names → FRIENDLY (warm and welcoming)
     *
     * ## Design Decision
     * These rules are centralized here so they can be easily modified
     * when business requirements change, without touching other code.
     *
     * @param name The person's validated name
     * @return The appropriate greeting format to use
     */
    fun determineGreetingFormat(name: PersonName): GreetingFormat {
        return when {
            name.value.length > LONG_NAME_THRESHOLD -> GreetingFormat.FORMAL
            name.value.equals("Anonymous", ignoreCase = true) -> GreetingFormat.DEFAULT
            else -> GreetingFormat.FRIENDLY
        }
    }

    /**
     * Business rule: Maximum allowed length for any greeting message.
     *
     * This prevents issues with:
     * - Database field limits
     * - UI display constraints
     * - Memory usage for very long names
     *
     * If a greeting would exceed this length, it should be rejected
     * or truncated based on your error handling strategy.
     */
    const val MAX_GREETING_LENGTH = 200
}

/**
 * Represents different greeting styles based on business rules.
 *
 * ## What is an Enum?
 * An enum (enumeration) defines a fixed set of constants. It's perfect when
 * you have a known, limited set of options that won't change at runtime.
 *
 * ## Why Use Enums?
 * - **Type Safety**: Can't accidentally use an invalid format
 * - **Readability**: `GreetingFormat.FRIENDLY` is clearer than `"friendly"`
 * - **Refactoring**: Changing a format name updates everywhere automatically
 *
 * ## The Formats
 * Each format represents a different tone for greetings:
 *
 * @property DEFAULT Standard, neutral greeting (e.g., "Hello World from X!")
 * @property FRIENDLY Warm, casual greeting (e.g., "Hey there, X! Welcome!")
 * @property FORMAL Professional, respectful greeting (e.g., "Greetings, X. Welcome to the application.")
 */
enum class GreetingFormat {
    /** Standard greeting - used for anonymous users or as fallback */
    DEFAULT,

    /** Casual, warm greeting - used for most regular names */
    FRIENDLY,

    /** Professional greeting - used for long or distinguished names */
    FORMAL,
}
