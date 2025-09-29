// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.bootstrap

import com.abitofhelp.hybrid.application.port.input.CreateGreetingInputPort
import com.abitofhelp.hybrid.application.port.output.OutputPort
import com.abitofhelp.hybrid.application.usecase.CreateGreetingUseCase
import com.abitofhelp.hybrid.domain.service.GreetingService
import com.abitofhelp.hybrid.infrastructure.adapter.output.BufferedFileOutputAdapter
import com.abitofhelp.hybrid.infrastructure.adapter.output.CompositeOutputAdapter
import com.abitofhelp.hybrid.infrastructure.adapter.output.ConsoleOutputAdapter
import com.abitofhelp.hybrid.infrastructure.adapter.output.FileOutputAdapter
import com.abitofhelp.hybrid.infrastructure.service.DefaultGreetingService
import com.abitofhelp.hybrid.presentation.cli.PresentationConfig
import com.abitofhelp.hybrid.presentation.cli.asyncCli
import com.abitofhelp.hybrid.presentation.cli.createAndRunCli

/**
 * The composition root where all dependencies are wired together.
 *
 * ## What is a Composition Root?
 * The composition root is the single place in your application where:
 * - All dependencies are created
 * - The dependency graph is wired together
 * - Concrete implementations are chosen
 *
 * ## Dependency Injection Without a Framework
 * This is "Pure DI" or "Manual DI":
 * - No magic annotations
 * - No reflection
 * - Compile-time safety
 * - Easy to understand and debug
 *
 * ## The Wiring Process
 * 1. **Infrastructure**: Create concrete implementations (adapters)
 * 2. **Application**: Create use cases with their dependencies
 * 3. **Presentation**: Create UI components with use cases
 * 4. **Execute**: Run the assembled application
 *
 * ## Benefits
 * - All dependencies are explicit
 * - Easy to test (just provide different implementations)
 * - No runtime surprises
 * - Fast startup (no reflection)
 *
 * ## Example with DI Framework (for comparison)
 * ```kotlin
 * // With a DI framework like Dagger or Koin
 * @Module
 * class AppModule {
 *     @Provides @Singleton
 *     fun provideGreetingService(): GreetingService = DefaultGreetingService()
 * }
 *
 * // With manual DI (what we do here)
 * val greetingService: GreetingService = DefaultGreetingService()
 * ```
 *
 * The manual approach is simpler for small/medium apps!
 */
object CompositionRoot {

    /**
     * Builds and runs a pure async program.
     *
     * ## The Assembly Process
     *
     * ### 1. Infrastructure Layer
     * ```kotlin
     * val greetingService: GreetingService = DefaultGreetingService()
     * ```
     * - Creates concrete implementations
     * - Could be swapped for DatabaseGreetingService, etc.
     *
     * ### 2. Output Port Selection
     * ```kotlin
     * val outputPort: OutputPort = CompositeOutputAdapter.createDefault(cfg.outputPath)
     * ```
     * - Smart factory chooses appropriate output
     * - Console only if no file path
     * - Composite (console + file) if path provided
     *
     * ### 3. Application Layer
     * ```kotlin
     * val createGreetingUseCase: CreateGreetingInputPort =
     *     CreateGreetingUseCase(greetingService, outputPort)
     * ```
     * - Use case receives its dependencies
     * - Depends on interfaces, not implementations
     *
     * ### 4. Presentation Configuration
     * Transform bootstrap config to presentation config.
     * Keeps layers properly separated.
     *
     * ### 5. Execution
     * Finally runs the assembled application.
     *
     * ## This is the preferred method for modern Kotlin applications.
     *
     * @param cfg Application configuration from command line
     * @return Exit code (0 for success)
     */
    suspend fun buildAndRunAsync(cfg: AppConfig): Int {
        // ---- Infrastructure (implements domain interfaces)
        val greetingService: GreetingService = DefaultGreetingService()

        // Choose output adapter based on configuration
        // For production apps, this could be driven by config or environment
        val outputPort: OutputPort = when {
            // High-volume logging: use buffered output
            cfg.outputPath != null && cfg.verbose ->
                BufferedFileOutputAdapter.highThroughput(cfg.outputPath!!)

            // Normal file output: use standard file adapter
            cfg.outputPath != null ->
                FileOutputAdapter(cfg.outputPath!!)

            // Console only
            else -> ConsoleOutputAdapter(System.out)
        }

        // ---- Application (use case depends on ports)
        val createGreetingUseCase: CreateGreetingInputPort =
            CreateGreetingUseCase(greetingService, outputPort)

        // ---- Presentation (transform config)
        val presentationConfig = PresentationConfig(
            verbose = cfg.verbose,
            outputPath = cfg.outputPath,
            name = cfg.name,
        )

        // ---- Execution
        // Run the pure async CLI
        return createAndRunCli(presentationConfig, createGreetingUseCase)
    }

    /**
     * Builds a suspend program for async execution.
     *
     * ## Why Deprecated?
     * This adds an unnecessary layer of indirection.
     * Just call `buildAndRunAsync` directly!
     *
     * ## The ReplaceWith Annotation
     * IDEs like IntelliJ IDEA will:
     * - Show this method as struck through
     * - Offer to automatically replace calls
     * - Update your code with one click
     *
     * @deprecated Use buildAndRunAsync for simpler API
     * @param cfg Application configuration
     * @return A suspend function that runs the program
     */
    @Deprecated("Use buildAndRunAsync for simpler API", ReplaceWith("buildAndRunAsync(cfg)"))
    suspend fun buildSuspendProgram(cfg: AppConfig): suspend () -> Int {
        return suspend {
            buildAndRunAsync(cfg)
        }
    }

    /**
     * Legacy method for backward compatibility.
     *
     * ## Why Keep This?
     * Some environments or frameworks might:
     * - Not support suspend functions
     * - Expect traditional blocking behavior
     * - Need gradual migration to async
     *
     * ## The Problem with This Approach
     * - Blocks threads (wastes resources)
     * - Can't properly handle cancellation
     * - Loses benefits of coroutines
     *
     * ## Migration Path
     * 1. Start here (blocking)
     * 2. Move to buildSuspendProgram (suspend)
     * 3. End with buildAndRunAsync (fully async)
     *
     * @deprecated Use buildSuspendProgram for proper async support
     * @param cfg Application configuration
     * @return A function that runs the program and returns exit code
     */
    @Deprecated("Use buildSuspendProgram for proper async support")
    fun buildProgram(cfg: AppConfig): () -> Int {
        // ---- Infrastructure (implements inner ports)
        val greetingService: GreetingService = DefaultGreetingService()

        // Use composite output adapter if file path is provided
        val outputPort: OutputPort = CompositeOutputAdapter.createDefault(cfg.outputPath)

        // ---- Application (use case depends on ports)
        val createGreetingUseCase: CreateGreetingInputPort =
            CreateGreetingUseCase(greetingService, outputPort)

        // ---- Presentation (factory produces a Runnable from ports + config)
        val presentationConfig = PresentationConfig(
            verbose = cfg.verbose,
            outputPath = cfg.outputPath,
            name = cfg.name,
        )

        // Use async CLI wrapper for Runnable compatibility
        val runnable = asyncCli(presentationConfig, createGreetingUseCase)

        // Return a zero-arg function (thunk) that:
        // 1. Runs the program
        // 2. Always returns 0 (success)
        // This keeps main() simple but loses error handling!
        return {
            runnable.run()
            ExitCodes.SUCCESS // Always returns success - not ideal!
        }
    }
}
