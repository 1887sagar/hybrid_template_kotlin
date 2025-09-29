# Bootstrap Module Documentation

**Version:** 1.0.0
**Date:** January 2025
**SPDX-License-Identifier:** BSD-3-Clause
**License File:** See LICENSE file in the project root.
**Copyright:** © 2025 Michael Gardner, A Bit of Help, Inc.
**Authors:** Michael Gardner
**Status:** Released

## Overview

The bootstrap module is the composition root of the application - the single place where all dependencies are wired together. It's the only module that has access to all layers and knows about concrete implementations. This module provides both synchronous and asynchronous entry points and manages the complete application lifecycle with structured concurrency.

## Key Responsibilities

1. **Dependency Wiring** - Creates and wires all application components through CompositionRoot
2. **Configuration Loading** - Securely parses command-line arguments with enhanced validation
3. **Application Startup** - Initializes the application with proper async/sync patterns
4. **Lifecycle Management** - Handles startup, shutdown, and signal processing gracefully
5. **Error Handling** - Maps exceptions to standardized Unix exit codes
6. **Logging** - Provides structured bootstrap logging separate from application logging

## Architecture Position

The bootstrap module is unique in the architecture:
- **Can access all layers** - Domain, Application, Infrastructure, and Presentation
- **Contains no business logic** - Only wiring, startup, and lifecycle code
- **Not referenced by any layer** - It's the top of the dependency hierarchy
- **Dual entry points** - Contains both sync and async main functions
- **Signal handling** - Manages OS-level signals through abstraction

## Core Components

### Async Entry Points

The application provides both synchronous and asynchronous entry points:

```kotlin
// Primary async entry point (recommended)
suspend fun main(args: Array<String>) {
    val exitCode = AsyncApp.runAsync(args)
    exitProcess(exitCode)
}

// Traditional sync entry point (compatibility)
fun main(args: Array<String>) {
    exitProcess(AsyncApp.run(args))
}
```

**Design Benefits**:
- **Modern Async** - Uses suspend main for natural coroutine support
- **Structured Concurrency** - Proper coroutine lifecycle management
- **Signal Handling** - Graceful shutdown on SIGTERM/SIGINT
- **Exit Code Mapping** - Standardized Unix exit codes

### AsyncApp Object

Enhanced application lifecycle manager with async support:

```kotlin
object AsyncApp {
    suspend fun runAsync(
        args: Array<String>,
        signals: SystemSignals = JvmSignals(),
        logger: BootstrapLogger = ConsoleBootstrapLogger()
    ): Int {
        // Install signal handlers for graceful shutdown
        signals.install { initiateShutdown(logger) }
        
        // Parse and validate configuration
        val cfg = parseArgs(args)
        
        // Launch main program with supervision
        val result = CompositionRoot.buildAndRunAsync(cfg)
        
        return when (result) {
            0 -> ExitCode.SUCCESS.code
            else -> ExitCode.ERROR.code
        }
    }
}
```

**Enhanced Features**:
- **SystemSignals Abstraction** - Testable signal handling
- **BootstrapLogger** - Structured lifecycle logging
- **ExitCode Mapping** - Centralized exit code management
- **Graceful Shutdown** - 5-second grace period with cleanup
- **Structured Concurrency** - Proper coroutine supervision

### SystemSignals Abstraction

Cross-platform, testable signal handling:

```kotlin
interface SystemSignals {
    fun install(onShutdown: () -> Unit)
}

class JvmSignals(
    private val useInternalSignalApi: Boolean = true
) : SystemSignals {
    override fun install(onShutdown: () -> Unit) {
        // Always install JVM shutdown hook as fallback
        Runtime.getRuntime().addShutdownHook(Thread { onShutdown() })
        
        // Optionally use sun.misc.Signal via reflection (better UX)
        if (useInternalSignalApi) {
            installSignalHandlers(onShutdown) // SIGINT, SIGTERM
        }
    }
}

class NoOpSignals : SystemSignals {
    override fun install(onShutdown: () -> Unit) {
        // For testing - no signal installation
    }
}
```

**Key Benefits**:
- **Testable** - NoOpSignals for unit tests
- **Cross-platform** - Graceful degradation on different JVMs/OS
- **Reflection-based** - Uses sun.misc.Signal without hard dependency
- **Fallback** - JVM shutdown hook always works

### ExitCode Enum

Centralized exit code management following Unix conventions:

```kotlin
enum class ExitCode(val code: Int) {
    SUCCESS(0),
    ERROR(1),
    MISUSE(2),
    EX_USAGE(64),      // Command line usage error
    EX_DATAERR(65),    // Data format error
    EX_NOINPUT(66),    // Cannot open input
    EX_UNAVAILABLE(69), // Service unavailable
    EX_SOFTWARE(70),   // Internal software error
    EX_OSERR(71),      // System error
    // ... more codes
    
    companion object {
        fun fromThrowable(throwable: Throwable): ExitCode = when (throwable) {
            is IllegalArgumentException -> EX_USAGE
            is java.nio.file.AccessDeniedException -> EX_NOPERM
            is java.io.IOException -> EX_IOERR
            is SecurityException -> EX_NOPERM
            else -> ERROR
        }
        
        fun fromErrorType(errorType: String): ExitCode = when (errorType) {
            "ValidationError" -> EX_DATAERR
            "ConfigurationError" -> EX_CONFIG
            "PermissionError" -> EX_NOPERM
            else -> ERROR
        }
    }
}
```

**Benefits**:
- **Standardized** - Follows BSD sysexits.h conventions
- **Consistent** - All exit codes centrally managed
- **Mapping** - Automatic exception-to-code mapping
- **Documentation** - Self-documenting error categories

### BootstrapLogger Interface

Structured logging for bootstrap lifecycle events:

```kotlin
interface BootstrapLogger {
    fun info(message: String)
    fun error(message: String)
    fun error(message: String, throwable: Throwable)
}

class ConsoleBootstrapLogger : BootstrapLogger {
    override fun info(message: String) {
        println("[BOOTSTRAP] $message")
    }
    
    override fun error(message: String) {
        System.err.println("[BOOTSTRAP] ERROR: $message")
    }
    
    override fun error(message: String, throwable: Throwable) {
        System.err.println("[BOOTSTRAP] ERROR: $message")
        throwable.printStackTrace()
    }
}

class NoOpBootstrapLogger : BootstrapLogger {
    override fun info(message: String) {} // For testing
    override fun error(message: String) {}
    override fun error(message: String, throwable: Throwable) {}
}
```

**Design Benefits**:
- **Separation** - Bootstrap logging separate from application logging
- **Testable** - NoOpBootstrapLogger for tests
- **Structured** - Consistent message formatting
- **Lifecycle Focus** - Logs startup, shutdown, and error events

### Enhanced AppConfig

Configuration with new CLI features:

```kotlin
data class AppConfig(
    val verbose: Boolean = false,
    val quiet: Boolean = false,    // NEW: Suppress non-essential output
    val outputPath: String? = null,
    val name: String? = null,
)
```

**New CLI Flags**:
- `--version` - Shows application version and exits
- `--quiet` or `-q` - Suppresses verbose output
- Enhanced `--help` - More comprehensive usage information

### SecureArgParser

Enhanced argument parsing with security and reduced complexity:

```kotlin
object SecureArgParser {
    fun parseSecure(args: Array<String>): AppConfig {
        // Validate array size (DoS prevention)
        require(args.size <= MAX_ARG_COUNT) { "Too many arguments: ${args.size}" }
        
        val parseState = ParseState()
        processArguments(args, parseState)
        
        return AppConfig(
            verbose = parseState.verbose,
            quiet = parseState.quiet,
            outputPath = parseState.outputPath,
            name = parseState.name,
        )
    }
    
    private fun processArgument(arg: String, args: Array<String>, index: Int, state: ParseState): Int {
        return when {
            arg == "--verbose" || arg == "-v" -> {
                state.verbose = true
                1 // Consumed 1 argument
            }
            arg == "--quiet" || arg == "-q" -> {
                state.quiet = true
                1
            }
            arg == "--version" -> throw ShowVersionException()
            arg == "--help" || arg == "-h" -> throw ShowHelpException()
            // ... other cases
        }
    }
}
```

**Security Enhancements**:
- **Input Validation** - Length limits and null byte detection
- **Path Validation** - Enhanced path security with traversal protection
- **DoS Prevention** - Argument count limits
- **Reduced Complexity** - Refactored to reduce cyclomatic complexity

### CompositionRoot

The dependency injection and wiring component:

```kotlin
object CompositionRoot {
    suspend fun buildAndRunAsync(cfg: AppConfig): Int {
        // Create domain services
        val greetingService = DefaultGreetingService()
        
        // Create infrastructure adapters
        val outputAdapter = when {
            cfg.outputPath != null -> {
                val fileAdapter = BufferedFileOutputAdapter(
                    filePath = cfg.outputPath,
                    autoFlush = !cfg.quiet // Performance vs durability trade-off
                )
                CompositeOutputAdapter(listOf(
                    ConsoleOutputAdapter(),
                    fileAdapter
                ))
            }
            else -> ConsoleOutputAdapter()
        }
        
        // Create use case
        val createGreeting = CreateGreetingUseCase(
            greetingService = greetingService,
            outputPort = outputAdapter
        )
        
        // Create and run presentation layer
        val runner = AsyncCliRunner(createGreeting, cfg)
        return runner.run()
    }
}
```

**Design Pattern**:
- **Single Place** - All wiring happens here
- **Manual DI** - No framework dependencies
- **Configuration-driven** - Uses AppConfig for adapter selection
- **Performance Aware** - Uses BufferedFileOutputAdapter for better throughput

## Signal Handling

The application handles Unix signals gracefully:

```kotlin
// Install signal handlers
signals.install { initiateShutdown(logger) }

private fun initiateShutdown(logger: BootstrapLogger) {
    if (isShuttingDown.compareAndSet(false, true)) {
        logger.info("Initiating graceful shutdown...")
        
        // Give 5-second grace period for cleanup
        cleanupJob = appScope.launch {
            delay(SHUTDOWN_GRACE_PERIOD_MILLIS)
            if (shutdownLatch.count > 0) {
                logger.error("Forceful shutdown after timeout")
                shutdownLatch.countDown()
            }
        }
        
        shutdownLatch.countDown() // Signal shutdown
    }
}
```

**Supported Signals**:
- **SIGTERM (15)** - Graceful termination request
- **SIGINT (2)** - Interrupt signal (Ctrl+C)
- **Shutdown Hook** - JVM shutdown (fallback)

**Shutdown Process**:
1. Signal received → Handler invoked
2. 5-second grace period starts
3. Application components shut down gracefully  
4. Resources cleaned up
5. Process exits with appropriate code

## Error Handling Strategy

The bootstrap module maps all errors to standardized exit codes:

```kotlin
private fun handleUncaughtException(exception: Throwable, logger: BootstrapLogger) {
    val exitCodeToUse = ExitCode.fromThrowable(exception)
    
    when (exception) {
        is OutOfMemoryError -> logger.error("CRITICAL: Out of memory")
        is SecurityException -> logger.error("SECURITY: ${exception.message}")
        else -> logger.error("FATAL: Unhandled exception", exception)
    }
    
    exitCode.set(exitCodeToUse)
    initiateShutdown(logger)
}
```

**Error Categories**:
- **Configuration Errors** → EX_USAGE (64)
- **Security Errors** → EX_NOPERM (77) 
- **I/O Errors** → EX_IOERR (74)
- **System Errors** → EX_OSERR (71)
- **Unknown Errors** → ERROR (1)

## Usage Examples

### Basic Usage
```bash
# Simple execution
java -jar app.jar Alice

# Verbose output  
java -jar app.jar --verbose Bob

# Quiet mode (minimal output)
java -jar app.jar --quiet Charlie

# File output
java -jar app.jar --out results.txt David

# Show version
java -jar app.jar --version

# Show help
java -jar app.jar --help
```

### Testing with Different Signals
```kotlin
@Test
fun `should handle shutdown gracefully`() = runTest {
    val signals = NoOpSignals() // Test-friendly
    val logger = NoOpBootstrapLogger()
    
    val exitCode = AsyncApp.runAsync(
        arrayOf("TestUser"), 
        signals, 
        logger
    )
    
    assertEquals(0, exitCode)
}
```

### Custom Configuration
```kotlin
// High-performance setup
val config = AppConfig(
    verbose = false,
    quiet = true,  // Minimal output
    outputPath = "/var/log/high-volume.log",
    name = "BatchProcess"
)

val result = CompositionRoot.buildAndRunAsync(config)
```

## Performance Considerations

The bootstrap module is optimized for both development and production:

**Development Mode** (verbose=true):
- Console output for debugging
- Detailed error messages
- Signal handling diagnostics

**Production Mode** (quiet=true):
- BufferedFileOutputAdapter for performance
- Minimal console output
- Structured logging only for critical events
- Optimized resource usage

**Resource Management**:
- Automatic cleanup on shutdown
- Proper coroutine cancellation
- File handle management
- Memory-conscious buffering

## Testing

The bootstrap module is designed for comprehensive testing:

```kotlin
class AsyncAppTest {
    @Test
    fun `should return success exit code for valid input`() = runTest {
        val result = AsyncApp.runAsync(
            args = arrayOf("Alice"),
            signals = NoOpSignals(),
            logger = NoOpBootstrapLogger()
        )
        
        assertEquals(0, result)
    }
    
    @Test  
    fun `should handle invalid arguments gracefully`() = runTest {
        val result = AsyncApp.runAsync(
            args = arrayOf("--invalid-flag"),
            signals = NoOpSignals(),
            logger = NoOpBootstrapLogger()
        )
        
        assertEquals(64, result) // EX_USAGE
    }
}
```

**Testability Features**:
- **NoOpSignals** - Prevents signal installation in tests
- **NoOpBootstrapLogger** - Silent logging for tests
- **Dependency injection** - All dependencies can be mocked
- **Pure functions** - Most logic is in testable pure functions

## Migration from Previous Version

If upgrading from earlier versions:

1. **Signal Handling**: Replace direct `sun.misc.Signal` usage with `SystemSignals`
2. **Exit Codes**: Replace magic numbers with `ExitCode` enum
3. **Logging**: Replace raw `println` with `BootstrapLogger`
4. **CLI Parsing**: Add support for `--version` and `--quiet` flags
5. **Async Support**: Consider migrating to `suspend main()` for better async support

## Future Enhancements

Planned improvements for future versions:

- **Configuration Files** - YAML/TOML support alongside CLI args
- **Environment Variables** - Support for environment-based config
- **Health Checks** - Built-in health check endpoints
- **Metrics Integration** - Bootstrap performance metrics
- **Docker Integration** - Enhanced container signal handling

## Conclusion

The bootstrap module provides a robust, testable, and performant foundation for application startup and lifecycle management. With enhanced signal handling, structured logging, standardized exit codes, and comprehensive error handling, it ensures reliable application behavior across different environments and deployment scenarios.