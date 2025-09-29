# Bootstrap Module Documentation

**Version:** 1.0.0
**Date:** January 2025
**SPDX-License-Identifier:** BSD-3-Clause
**License File:** See LICENSE file in the project root.
**Copyright:** © 2025 Michael Gardner, A Bit of Help, Inc.
**Authors:** Michael Gardner
**Status:** Released

## Overview

The bootstrap module is the composition root of the application - the single place where all dependencies are wired together. It's the only module that has access to all layers and knows about concrete implementations. This module starts the application and manages its lifecycle.

## Key Responsibilities

1. **Dependency Wiring** - Creates and wires all application components
2. **Configuration Loading** - Parses command-line arguments and loads configuration
3. **Application Startup** - Initializes the application in the correct order
4. **Lifecycle Management** - Handles startup and shutdown gracefully

## Architecture Position

The bootstrap module is unique in the architecture:
- **Can access all layers** - Domain, Application, Infrastructure, and Presentation
- **Contains no business logic** - Only wiring and startup code
- **Not referenced by any layer** - It's the top of the dependency hierarchy
- **Single entry point** - Contains the main function

## Components

### EntryPoint

The application's main function - kept minimal by design:

```kotlin
package com.abitofhelp.hybrid.bootstrap

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    exitProcess(App.run(args))
}
```

**Design Principles**:
- **Single Line** - Delegates immediately to testable code
- **Exit Code Handling** - Uses `exitProcess` to ensure proper termination
- **No Logic** - All logic is in testable components

### App Object

Handles the application lifecycle and error handling:

```kotlin
object App {
    fun run(args: Array<String>): Int {
        return try {
            val cfg = parseArgs(args)
            val program = CompositionRoot.buildProgram(cfg)
            program()
        } catch (e: IllegalArgumentException) {
            // Configuration errors
            System.err.println("Configuration error: ${e.message}")
            1
        } catch (e: Exception) {
            // Unexpected errors
            System.err.println("Fatal error: ${e.message ?: e.javaClass.simpleName}")
            2
        }
    }
}
```

**Key Features**:
- **Error Handling** - Catches and categorizes errors
- **Exit Codes** - Returns appropriate codes (0=success, 1=config error, 2=fatal error)
- **Configuration Parsing** - Delegates to parseArgs function
- **Program Building** - Delegates to composition root

### AppConfig

Data class for application configuration:

```kotlin
data class AppConfig(
    val verbose: Boolean = false,
    val outputPath: String? = null,
    val name: String? = null,
)

fun parseArgs(args: Array<String>): AppConfig {
    val verbose = args.contains("--verbose") || args.contains("-v")
    val out = args.indexOf("--out").takeIf { it >= 0 }
        ?.let { idx -> args.getOrNull(idx + 1) }
    val name = args.firstOrNull { !it.startsWith("--") && !it.startsWith("-") }
    
    return AppConfig(verbose = verbose, outputPath = out, name = name)
}
```

**Configuration Options**:
- **verbose** - Enable detailed logging
- **outputPath** - Where to send output (future feature)
- **name** - The name for greeting

**Parsing Logic**:
- Supports both long (`--verbose`) and short (`-v`) flags
- Handles flag arguments (`--out filename`)
- First non-flag argument is treated as name

### CompositionRoot

The heart of dependency injection - wires all components manually:

```kotlin
object CompositionRoot {
    fun buildProgram(cfg: AppConfig): () -> Int {
        // ---- Infrastructure (implements inner ports)
        val greetingService: GreetingService = DefaultGreetingService()
        val outputPort: OutputPort = ConsoleOutputAdapter()
        
        // ---- Application (use case depends on ports)
        val createGreetingUseCase: CreateGreetingInputPort =
            CreateGreetingUseCase(greetingService, outputPort)
        
        // ---- Presentation (factory produces a Runnable)
        val presentationConfig = PresentationConfig(
            verbose = cfg.verbose,
            outputPath = cfg.outputPath,
            name = cfg.name,
        )
        val runnable = cli(presentationConfig, createGreetingUseCase)
        
        // Return zero-arg function
        return {
            runnable.run()
            0
        }
    }
}
```

**Wiring Order**:
1. **Infrastructure** - Create concrete implementations
2. **Application** - Wire use cases with dependencies
3. **Presentation** - Create UI components with use cases
4. **Return Function** - Zero-arg function for clean execution

## Dependency Injection Approach

### Manual DI vs Framework

This project uses manual dependency injection instead of a framework. Here's why:

**Advantages of Manual DI**:
- **Compile-time Safety** - Errors caught at compile time
- **Explicit Dependencies** - Clear what depends on what
- **No Magic** - Easy to understand and debug
- **No Runtime Overhead** - No reflection or code generation
- **Framework Independence** - Not tied to any DI framework

**Trade-offs**:
- More boilerplate for complex applications
- Need to maintain wiring code manually
- No automatic scope management

### Wiring Patterns

#### Simple Wiring
```kotlin
// Direct instantiation
val service = DefaultGreetingService()
val adapter = ConsoleOutputAdapter()
val useCase = CreateGreetingUseCase(service, adapter)
```

#### Factory Pattern
```kotlin
// For complex creation logic
fun createDatabaseAdapter(config: DatabaseConfig): DatabaseAdapter {
    val dataSource = createDataSource(config)
    val migrator = DatabaseMigrator(dataSource)
    migrator.migrate()
    return DatabaseAdapter(dataSource)
}
```

#### Provider Pattern
```kotlin
// For lazy initialization
class DatabaseProvider(private val config: DatabaseConfig) {
    private val instance by lazy {
        createDatabaseAdapter(config)
    }
    
    fun get(): DatabaseAdapter = instance
}
```

## Configuration Management

### Environment-Based Configuration

Support different environments:

```kotlin
enum class Environment {
    DEVELOPMENT,
    TESTING,
    PRODUCTION
}

fun loadConfig(env: Environment): AppConfig {
    return when (env) {
        Environment.DEVELOPMENT -> AppConfig(
            verbose = true,
            outputPath = "dev-output.log"
        )
        Environment.TESTING -> AppConfig(
            verbose = false,
            outputPath = null
        )
        Environment.PRODUCTION -> AppConfig(
            verbose = false,
            outputPath = "production.log"
        )
    }
}
```

### Configuration Sources

Combine multiple configuration sources:

```kotlin
fun loadConfiguration(args: Array<String>): AppConfig {
    // 1. Load defaults
    var config = AppConfig()
    
    // 2. Load from file
    val fileConfig = loadFromFile("app.properties")
    config = config.merge(fileConfig)
    
    // 3. Load from environment
    val envConfig = loadFromEnvironment()
    config = config.merge(envConfig)
    
    // 4. Load from command line (highest priority)
    val cliConfig = parseArgs(args)
    config = config.merge(cliConfig)
    
    return config
}
```

## Lifecycle Management

### Startup Sequence

Proper initialization order:

```kotlin
class Application {
    fun start() {
        // 1. Initialize logging
        initializeLogging()
        
        // 2. Load configuration
        val config = loadConfiguration()
        
        // 3. Initialize infrastructure
        initializeDatabase(config.database)
        initializeCache(config.cache)
        
        // 4. Wire dependencies
        val dependencies = wireDependencies(config)
        
        // 5. Start services
        dependencies.services.forEach { it.start() }
        
        // 6. Register shutdown hook
        registerShutdownHook(dependencies)
    }
}
```

### Graceful Shutdown

Handle shutdown properly:

```kotlin
fun registerShutdownHook(dependencies: Dependencies) {
    Runtime.getRuntime().addShutdownHook(thread(start = false) {
        println("Shutting down gracefully...")
        
        // 1. Stop accepting new requests
        dependencies.server.stop()
        
        // 2. Wait for ongoing requests
        dependencies.requestTracker.awaitCompletion(30.seconds)
        
        // 3. Close connections
        dependencies.database.close()
        dependencies.cache.close()
        
        // 4. Final cleanup
        println("Shutdown complete")
    })
}
```

## Testing the Bootstrap Module

### Testing App Logic

```kotlin
class AppTest {
    @Test
    fun `should return 0 on successful execution`() {
        val result = App.run(arrayOf("TestUser"))
        
        assertEquals(0, result)
    }
    
    @Test
    fun `should return 1 on configuration error`() {
        val result = App.run(arrayOf("--invalid-flag"))
        
        assertEquals(1, result)
    }
}
```

### Testing Argument Parsing

```kotlin
class AppConfigTest {
    @Test
    fun `should parse verbose flag`() {
        val config = parseArgs(arrayOf("-v", "Alice"))
        
        assertTrue(config.verbose)
        assertEquals("Alice", config.name)
    }
    
    @Test
    fun `should parse output path`() {
        val config = parseArgs(arrayOf("--out", "output.txt"))
        
        assertEquals("output.txt", config.outputPath)
    }
}
```

### Testing Composition Root

```kotlin
class CompositionRootTest {
    @Test
    fun `should build program successfully`() {
        val config = AppConfig(name = "Test")
        val program = CompositionRoot.buildProgram(config)
        
        val result = program()
        
        assertEquals(0, result)
    }
}
```

## Common Bootstrap Patterns

### Feature Flags

Enable/disable features at runtime:

```kotlin
data class Features(
    val enableCache: Boolean = false,
    val enableMetrics: Boolean = false,
    val enableNewUI: Boolean = false
)

fun buildProgram(config: AppConfig, features: Features): () -> Int {
    val greetingService = if (features.enableCache) {
        CachedGreetingService(DefaultGreetingService())
    } else {
        DefaultGreetingService()
    }
    
    // Continue wiring...
}
```

### Multi-Module Applications

Wire multiple modules:

```kotlin
object CompositionRoot {
    fun buildModules(config: AppConfig): ApplicationModules {
        // Core module
        val coreModule = CoreModule()
        
        // Feature modules
        val userModule = UserModule(coreModule.database)
        val orderModule = OrderModule(coreModule.database, userModule.userService)
        val notificationModule = NotificationModule(coreModule.messaging)
        
        return ApplicationModules(
            core = coreModule,
            user = userModule,
            order = orderModule,
            notification = notificationModule
        )
    }
}
```

### Plugin Architecture

Support dynamic plugin loading:

```kotlin
interface Plugin {
    fun name(): String
    fun initialize(context: PluginContext)
    fun start()
    fun stop()
}

class PluginLoader {
    fun loadPlugins(directory: Path): List<Plugin> {
        return directory.listDirectoryEntries("*.jar")
            .mapNotNull { loadPlugin(it) }
    }
}
```

## Best Practices

### 1. Keep Bootstrap Thin

The bootstrap module should only contain:
- Wiring logic
- Configuration parsing
- Startup/shutdown code

It should NOT contain:
- Business logic
- Complex transformations
- Infrastructure implementations

### 2. Make Dependencies Explicit

```kotlin
// Good: Explicit dependencies
val useCase = CreateGreetingUseCase(
    greetingService = greetingService,
    outputPort = outputPort
)

// Bad: Hidden dependencies
val useCase = CreateGreetingUseCase() // Where do dependencies come from?
```

### 3. Fail Fast

Validate configuration and dependencies early:

```kotlin
fun buildProgram(config: AppConfig): () -> Int {
    // Validate configuration
    require(config.outputPath?.isNotBlank() ?: true) {
        "Output path cannot be blank"
    }
    
    // Test critical dependencies
    try {
        testDatabaseConnection(config.database)
    } catch (e: Exception) {
        throw IllegalStateException("Cannot connect to database", e)
    }
    
    // Continue with wiring...
}
```

### 4. Use Type-Safe Configuration

```kotlin
// Good: Type-safe configuration
data class DatabaseConfig(
    val url: String,
    val username: String,
    val password: String,
    val poolSize: Int = 10
)

// Bad: String-based configuration
val config = mapOf(
    "db.url" to "jdbc:postgresql://...",
    "db.username" to "user"
)
```

## Future Enhancements

### Dependency Graph Visualization

Generate a visual representation of dependencies:

```kotlin
class DependencyGraphBuilder {
    fun buildGraph(root: CompositionRoot): DependencyGraph {
        // Analyze dependencies and generate graph
    }
    
    fun exportToDot(graph: DependencyGraph): String {
        // Export to GraphViz format
    }
}
```

### Health Checks

Implement startup health checks:

```kotlin
interface HealthCheck {
    suspend fun check(): HealthResult
}

class StartupHealthChecker(
    private val checks: List<HealthCheck>
) {
    suspend fun checkAll(): List<HealthResult> {
        return checks.map { check ->
            runCatching { check.check() }
                .getOrElse { HealthResult.Unhealthy(it.message ?: "Unknown error") }
        }
    }
}
```

### Metrics and Monitoring

Add application metrics:

```kotlin
class MetricsCollector {
    fun recordStartupTime(duration: Duration) {
        metrics.record("app.startup.time", duration.inWholeMilliseconds)
    }
    
    fun recordDependencyCreation(name: String, duration: Duration) {
        metrics.record("app.dependency.creation.$name", duration.inWholeMilliseconds)
    }
}
```

## Summary

The bootstrap module is the application's starting point and composition root. It:
- **Wires** all dependencies together in one place
- **Configures** the application based on various sources
- **Manages** the application lifecycle
- **Provides** a clean entry point
- **Enables** testing of startup logic

By keeping all wiring in one place and following these patterns, you create applications that are:
- **Easy to understand** - All wiring in one place
- **Easy to test** - Separated concerns
- **Easy to modify** - Change wiring without touching business logic
- **Easy to configure** - Multiple configuration sources
- **Easy to deploy** - Clear startup and shutdown procedures