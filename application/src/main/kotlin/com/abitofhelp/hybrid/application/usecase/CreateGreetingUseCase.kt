////////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
////////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.application.usecase

import arrow.core.Either
import arrow.core.flatMap
import com.abitofhelp.hybrid.application.dto.CreateGreetingCommand
import com.abitofhelp.hybrid.application.dto.GreetingResult
import com.abitofhelp.hybrid.application.error.ApplicationError
import com.abitofhelp.hybrid.application.port.input.CreateGreetingInputPort
import com.abitofhelp.hybrid.application.port.output.OutputPort
import com.abitofhelp.hybrid.domain.service.GreetingService

/**
 * Use case for creating and delivering greetings.
 *
 * ## What is a Use Case?
 * A use case represents a specific business operation or user action. It:
 * - Orchestrates domain services and infrastructure
 * - Handles the flow of data through the system
 * - Manages transactions and error handling
 * - Implements the application's business processes
 *
 * ## The Application Layer's Role
 * This layer acts as a coordinator:
 * 1. Receives commands from presentation layer
 * 2. Validates and transforms input
 * 3. Calls domain services for business logic
 * 4. Interacts with infrastructure through ports
 * 5. Returns results or errors
 *
 * ## This Use Case's Flow
 * ```
 * Presentation → Command → Use Case
 *                              ↓
 *                    Validate & Transform
 *                              ↓
 *                     Domain Service
 *                              ↓
 *                      Output Port
 *                              ↓
 *                     ← Result/Error
 * ```
 *
 * ## Dependency Injection
 * Notice the constructor parameters:
 * - `greetingService`: From domain layer (business logic)
 * - `outputPort`: Interface from application, implemented by infrastructure
 *
 * This creates clean dependencies:
 * - Application depends on Domain (stable)
 * - Application defines OutputPort interface
 * - Infrastructure implements OutputPort
 * - No direct dependency on infrastructure!
 *
 * @param greetingService Domain service that creates greetings
 * @param outputPort Where to send the greeting (console, file, etc.)
 */
class CreateGreetingUseCase(
    private val greetingService: GreetingService,
    private val outputPort: OutputPort,
) : CreateGreetingInputPort {
    /**
     * Executes the create greeting use case.
     *
     * ## The Flow Explained
     *
     * ### 1. Input Validation & Transformation
     * ```kotlin
     * if (command.name.isNullOrBlank()) {
     *     Either.Right(PersonName.anonymous())  // Default for missing names
     * } else {
     *     PersonName.create(command.name)       // Validate & create domain object
     * }
     * ```
     *
     * ### 2. Functional Composition with flatMap
     * `flatMap` chains operations that return Either:
     * - If previous step returns Left (error), stop and return that error
     * - If previous step returns Right (success), continue with next operation
     *
     * ### 3. Error Transformation
     * `mapLeft { ApplicationError.DomainErrorWrapper(it) }`
     * - Transforms domain errors into application errors
     * - Maintains layer boundaries
     *
     * ### 4. Conditional Output
     * The `silent` flag allows creating greetings without output:
     * - Useful for testing
     * - Batch operations
     * - Preview functionality
     *
     * ## Example Call Flow
     * ```kotlin
     * // Happy path
     * execute(CreateGreetingCommand("Alice", false))
     * // → PersonName.create("Alice") → Right(PersonName("Alice"))
     * // → greetingService.createGreeting(...) → Right("Hey there, Alice!")
     * // → outputPort.send(...) → Right(Unit)
     * // → Right(GreetingResult("Hey there, Alice!", "Alice"))
     *
     * // Error path
     * execute(CreateGreetingCommand("A@B", false))
     * // → PersonName.create("A@B") → Left(ValidationError)
     * // → Left(ApplicationError.DomainErrorWrapper(...))
     * // (stops here, error returned to caller)
     * ```
     */
    override suspend fun execute(command: CreateGreetingCommand): Either<ApplicationError, GreetingResult> {
        // Step 1: Validate command structure (if needed)
        return command.validate().flatMap {
            // Step 2: Transform command to domain object
            command.toPersonName()
        }.flatMap { personName ->
            // Step 3: Generate greeting using domain service
            greetingService.createGreeting(personName)
                .mapLeft { ApplicationError.DomainErrorWrapper(it) }
                .flatMap { greeting ->
                    // Step 4: Send to output based on command preferences
                    if (command.shouldSendOutput()) {
                        outputPort.send(greeting)
                            .map { GreetingResult(greeting, personName.value) }
                    } else {
                        // Silent mode - return result without output
                        Either.Right(GreetingResult(greeting, personName.value))
                    }
                }
        }
    }
}
