////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.infrastructure.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.abitofhelp.hybrid.domain.error.DomainError
import com.abitofhelp.hybrid.domain.service.GreetingFormat
import com.abitofhelp.hybrid.domain.service.GreetingPolicy
import com.abitofhelp.hybrid.domain.service.GreetingService
import com.abitofhelp.hybrid.domain.value.PersonName

/**
 * Default implementation of GreetingService.
 *
 * ## Why is This in Infrastructure?
 * You might wonder why a "service" is in the infrastructure layer.
 * The key insight: this implementation could be replaced with:
 * - DatabaseGreetingService: Loads templates from database
 * - ApiGreetingService: Fetches greetings from a web service
 * - LocalizedGreetingService: Uses i18n resources
 * - AIGreetingService: Generates greetings using ML models
 *
 * The domain layer defines the interface (what we need),
 * infrastructure provides implementations (how it's done).
 *
 * ## Current Implementation
 * This simple implementation:
 * - Uses the domain's GreetingPolicy for format decisions
 * - Has hardcoded greeting templates
 * - Validates greeting length against business rules
 *
 * ## Future Enhancements
 * ```kotlin
 * class DatabaseGreetingService(
 *     private val repository: GreetingTemplateRepository
 * ) : GreetingService {
 *     override suspend fun createGreeting(name: PersonName): Either<DomainError, String> {
 *         val format = GreetingPolicy.determineGreetingFormat(name)
 *         val template = repository.findByFormat(format)
 *         return template.fillWith(name)
 *     }
 * }
 * ```
 *
 * ## Thread Safety
 * This implementation is stateless and thread-safe.
 * Multiple coroutines can call createGreeting concurrently.
 */
class DefaultGreetingService : GreetingService {

    /**
     * Creates a personalized greeting based on the person's name.
     *
     * ## Implementation Flow
     *
     * ### 1. Format Determination
     * Uses the domain's GreetingPolicy to decide the format.
     * This separates the "what format" decision (domain) from
     * the "what text" implementation (infrastructure).
     *
     * ### 2. Template Selection
     * The `when` expression maps formats to greeting templates:
     * - **DEFAULT**: Simple, universal greeting
     * - **FRIENDLY**: Warm and welcoming
     * - **FORMAL**: Professional and respectful
     *
     * ### 3. Business Rule Validation
     * Even though we generate the greeting, we still validate it!
     * This guards against:
     * - Template changes that violate rules
     * - Very long names creating oversized greetings
     * - Future dynamic content that might exceed limits
     *
     * ### 4. Exception Safety
     * The try-catch ensures that ANY error becomes a proper domain error.
     * This prevents:
     * - Null pointer exceptions from corrupted data
     * - String formatting errors
     * - Future implementation bugs from crashing the app
     *
     * ## Why Suspend?
     * Even though this implementation doesn't do I/O, it's marked `suspend` because:
     * - The interface requires it
     * - Other implementations might need async operations
     * - Consistency across all implementations
     *
     * @param name The validated PersonName to greet
     * @return Either a greeting string (Right) or a DomainError (Left)
     */
    override suspend fun createGreeting(name: PersonName): Either<DomainError, String> {
        return try {
            // Step 1: Determine appropriate greeting format based on business rules
            val format = GreetingPolicy.determineGreetingFormat(name)

            // Step 2: Select greeting template based on format
            val greeting = when (format) {
                GreetingFormat.DEFAULT -> "Hello World from ${name.value}!"
                GreetingFormat.FRIENDLY -> "Hey there, ${name.value}! Welcome!"
                GreetingFormat.FORMAL -> "Greetings, ${name.value}. Welcome to the application."
            }

            // Step 3: Validate greeting meets business constraints
            if (greeting.length > GreetingPolicy.MAX_GREETING_LENGTH) {
                DomainError.BusinessRuleViolation(
                    rule = "MaxGreetingLength",
                    reason = "Greeting exceeds maximum length of ${GreetingPolicy.MAX_GREETING_LENGTH} characters",
                ).left()
            } else {
                greeting.right()
            }
        } catch (e: Exception) {
            // Step 4: Transform any unexpected exceptions to domain errors
            // This ensures the domain layer's error handling isn't bypassed
            DomainError.BusinessRuleViolation(
                rule = "GreetingCreation",
                reason = "Failed to create greeting: ${e.message ?: "Unknown error"}",
            ).left()
        }
    }
}
