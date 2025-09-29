////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.domain.service

import arrow.core.Either
import com.abitofhelp.hybrid.domain.error.DomainError
import com.abitofhelp.hybrid.domain.value.PersonName

/**
 * Domain service interface for creating personalized greetings.
 *
 * ## What is an Interface?
 * An interface defines a contract - it specifies what methods a class must implement,
 * but not how they're implemented. This allows different implementations to exist.
 *
 * ## Why Use Interfaces in Domain Layer?
 * - **Flexibility**: Different implementations can exist (e.g., different greeting strategies)
 * - **Testing**: Easy to create mock implementations for tests
 * - **Dependency Inversion**: Higher layers depend on this interface, not concrete classes
 *
 * ## Suspend Functions
 * The `suspend` keyword means this function can be paused and resumed - it's asynchronous.
 * This allows for non-blocking operations like database queries or API calls.
 *
 * ## Example Implementation
 * ```kotlin
 * class FriendlyGreetingService : GreetingService {
 *     override suspend fun createGreeting(name: PersonName): Either<DomainError, String> {
 *         // Could check a database for user preferences
 *         val preference = fetchUserPreference(name) // suspend function
 *
 *         return if (preference == "formal") {
 *             "Good day, ${name.value}.".right()
 *         } else {
 *             "Hi ${name.value}!".right()
 *         }
 *     }
 * }
 * ```
 */
interface GreetingService {
    /**
     * Creates a personalized greeting message for the given person.
     *
     * This method:
     * - Takes a validated PersonName (so we know it's already valid)
     * - Returns Either<DomainError, String>:
     *   - Left: Contains error if greeting can't be created
     *   - Right: Contains the greeting string
     *
     * ## Possible Errors
     * - BusinessRuleViolation: If greeting exceeds maximum length
     * - Other domain-specific errors based on implementation
     *
     * ## Usage Example
     * ```kotlin
     * val greetingService: GreetingService = // injected
     * val name = PersonName.create("Alice").getOrNull()!!
     *
     * greetingService.createGreeting(name).fold(
     *     { error ->
     *         when (error) {
     *             is DomainError.BusinessRuleViolation ->
     *                 println("Greeting too long: ${error.reason}")
     *             else -> println("Unexpected error: $error")
     *         }
     *     },
     *     { greeting -> println(greeting) } // "Hey there, Alice! Welcome!"
     * )
     * ```
     *
     * @param name The validated person's name to create a greeting for
     * @return Either a DomainError (Left) or the greeting message (Right)
     */
    suspend fun createGreeting(name: PersonName): Either<DomainError, String>
}
