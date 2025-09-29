// //////////////////////////////////////////////////////////////////////////////
// Kotlin Hybrid Architecture Template
// Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
// SPDX-License-Identifier: BSD-3-Clause
// See LICENSE file in the project root.
// //////////////////////////////////////////////////////////////////////////////

package com.abitofhelp.hybrid.bootstrap

import com.abitofhelp.hybrid.application.port.input.CreateGreetingInputPort
import com.abitofhelp.hybrid.application.port.output.ErrorOutputPort
import com.abitofhelp.hybrid.application.port.output.OutputPort
import com.abitofhelp.hybrid.application.usecase.CreateGreetingUseCase
import com.abitofhelp.hybrid.domain.service.GreetingService
import com.abitofhelp.hybrid.infrastructure.adapter.output.BufferedFileOutputAdapter
import com.abitofhelp.hybrid.infrastructure.adapter.output.ConsoleErrorOutputAdapter
import com.abitofhelp.hybrid.infrastructure.adapter.output.ConsoleOutputAdapter
import com.abitofhelp.hybrid.infrastructure.adapter.output.FileOutputAdapter
import com.abitofhelp.hybrid.infrastructure.service.DefaultGreetingService
import com.abitofhelp.hybrid.presentation.cli.PresentationConfig
import com.abitofhelp.hybrid.presentation.cli.createAndRunCli

/**
 * The composition root where all dependencies are wired together.
 *
 * ## What is a Composition Root?
 * The composition root is the single place in your application where:
 * - All dependencies are created and configured
 * - The dependency graph is wired together
 * - Concrete implementations are chosen for interfaces
 * - Application layers are connected
 *
 * This pattern is also known as the "Object Graph" or "Dependency Factory".
 *
 * ## Why Use a Composition Root?
 * Traditional dependency management problems:
 * ```kotlin
 * // BAD: Dependencies scattered throughout code
 * class OrderService {
 *     private val paymentService = PaymentService() // Hard-coded!
 *     private val emailService = EmailService()     // Can't test!
 * }
 * ```
 *
 * Composition root solution:
 * ```kotlin
 * // GOOD: All dependencies created in one place
 * object CompositionRoot {
 *     fun createOrderService(): OrderService {
 *         val paymentService = createPaymentService()
 *         val emailService = createEmailService()
 *         return OrderService(paymentService, emailService)
 *     }
 * }
 * ```
 *
 * ## Dependency Injection Without a Framework
 * This is "Pure DI" or "Manual DI" approach:
 * - **No magic annotations** - everything is explicit
 * - **No reflection** - compile-time safety guaranteed
 * - **Easy to understand** - follow the code path directly
 * - **Fast startup** - no framework initialization overhead
 * - **Debugger friendly** - step through dependency creation
 *
 * ## The Wiring Process (Layer by Layer)
 * ```
 * 1. Infrastructure → Create concrete implementations (adapters)
 *    ├── FileOutputAdapter
 *    ├── ConsoleOutputAdapter
 *    └── DefaultGreetingService
 *
 * 2. Application → Create use cases with dependencies
 *    └── CreateGreetingUseCase(greetingService, outputPort)
 *
 * 3. Presentation → Create UI components with use cases
 *    └── CliRunner(createGreetingUseCase)
 *
 * 4. Execute → Run the assembled application
 *    └── CLI.run()
 * ```
 *
 * ## Benefits Over DI Frameworks
 * - **Explicit dependencies**: See exactly what depends on what
 * - **Easy to test**: Inject test doubles without configuration
 * - **No runtime surprises**: Missing dependencies fail at compile time
 * - **Fast startup**: No reflection or container initialization
 * - **IDE support**: Full auto-completion and refactoring
 *
 * ## Comparison with DI Frameworks
 * ```kotlin
 * // With Dagger/Koin (framework approach)
 * @Module
 * class AppModule {
 *     @Provides @Singleton
 *     fun provideGreetingService(): GreetingService = DefaultGreetingService()
 *
 *     @Provides
 *     fun provideOutputPort(@Named("outputPath") path: String?): OutputPort =
 *         if (path != null) FileOutputAdapter(path)
 *         else ConsoleOutputAdapter()
 * }
 *
 * // Manual DI (our approach)
 * val greetingService: GreetingService = DefaultGreetingService()
 * val outputPort: OutputPort = if (outputPath != null) {
 *     FileOutputAdapter(outputPath)
 * } else {
 *     ConsoleOutputAdapter()
 * }
 * ```
 *
 * The manual approach is often simpler and more maintainable for small to medium apps!
 *
 * ## When to Use DI Frameworks
 * Consider frameworks like Dagger, Koin, or Spring when you have:
 * - Large applications with hundreds of dependencies
 * - Complex scoping requirements (request, session, singleton)
 * - Advanced features like aspect-oriented programming
 * - Team preference for annotation-based configuration
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

        // Error output always goes to stderr (for now - could be configurable)
        val errorOutputPort: ErrorOutputPort = ConsoleErrorOutputAdapter(System.err)

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
        // Run the pure async CLI with all ports
        return createAndRunCli(presentationConfig, createGreetingUseCase, outputPort, errorOutputPort)
    }

    /**
     * Builds and runs async program with custom ports for testing.
     *
     * ## Purpose
     * This method allows tests to inject their own output adapters
     * to capture and assert on output without touching global streams.
     *
     * ## Example Usage in Tests
     * ```kotlin
     * val outputCollector = TestOutputAdapter()
     * val errorCollector = TestErrorOutputAdapter()
     *
     * val exitCode = CompositionRoot.buildAndRunAsyncForTesting(
     *     cfg = AppConfig(name = "TestUser"),
     *     outputPort = outputCollector,
     *     errorOutputPort = errorCollector
     * )
     *
     * outputCollector.messages shouldContain "Hey there, TestUser!"
     * errorCollector.errors shouldBe empty
     * exitCode shouldBe 0
     * ```
     *
     * @param cfg Application configuration
     * @param outputPort Custom output port (e.g., test collector)
     * @param errorOutputPort Custom error output port (e.g., test collector)
     * @return Exit code
     */
    suspend fun buildAndRunAsyncForTesting(
        cfg: AppConfig,
        outputPort: OutputPort,
        errorOutputPort: ErrorOutputPort,
    ): Int {
        // ---- Infrastructure (implements domain interfaces)
        val greetingService: GreetingService = DefaultGreetingService()

        // ---- Application (use case depends on ports)
        val createGreetingUseCase: CreateGreetingInputPort =
            CreateGreetingUseCase(greetingService, outputPort)

        // ---- Presentation (transform config)
        val presentationConfig = PresentationConfig(
            verbose = cfg.verbose,
            outputPath = cfg.outputPath,
            name = cfg.name,
        )

        // ---- Execution with injected ports
        return createAndRunCli(presentationConfig, createGreetingUseCase, outputPort, errorOutputPort)
    }
}
