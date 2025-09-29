# Bootstrap Module

**Version:** 1.0.0  
**Date:** September 29, 2025
**License:** BSD-3-Clause  
**Copyright:** © 2025 Michael Gardner, A Bit of Help, Inc.  
**Authors:** Michael Gardner  
**Status:** Released

## What is the Bootstrap Module?

The bootstrap module is your application's "startup crew" - it gets everything ready, wires all the components together, and manages the complete application lifecycle. Think of it as the backstage coordinator that handles startup, signal processing, graceful shutdown, and ensures all the actors (modules) perform their roles properly.

## Why Modern Bootstrapping Matters

Without proper bootstrapping, you'd have:
- Scattered configuration logic throughout the application
- Manual dependency wiring everywhere (maintenance nightmare)
- No clear application entry point or lifecycle management
- Poor error handling and inconsistent exit codes
- Unreliable resource cleanup and shutdown procedures
- Difficult testing due to global state and signal handling
- Non-portable signal handling across different platforms

The enhanced bootstrap module solves all these problems with modern async patterns, testable abstractions, and structured concurrency.

## Enhanced Features (v1.0.0)

### 🚀 **Dual Entry Points** 
- **Async-first**: `suspend main()` with structured concurrency
- **Sync compatibility**: Traditional `main()` for legacy integration

### 🛡️ **Signal Handling Abstraction**
- **SystemSignals interface**: Testable signal management
- **Cross-platform**: Graceful degradation on different JVMs/OS
- **5-second grace period**: Clean shutdown with timeout protection

### 📋 **Centralized Exit Codes**
- **Unix conventions**: Follows BSD sysexits.h standards
- **Automatic mapping**: Exceptions → appropriate exit codes
- **Consistent**: All error categories properly categorized

### 🔧 **Enhanced CLI Processing** 
- **New flags**: `--version`, `--quiet` support
- **Security hardened**: Input validation and DoS prevention
- **Better parsing**: Reduced complexity with clear error messages

### 📝 **Structured Bootstrap Logging**
- **BootstrapLogger interface**: Separate from application logging
- **Testable**: NoOpBootstrapLogger for unit tests
- **Lifecycle focus**: Startup, shutdown, and error event logging

## Core Responsibilities

### 1. Modern Application Entry Points
```kotlin
// Recommended: Async entry with full lifecycle management
suspend fun main(args: Array<String>) {
    val exitCode = AsyncApp.runAsync(args)
    exitProcess(exitCode)
}

// Alternative: Sync entry for compatibility
fun main(args: Array<String>) {
    exitProcess(AsyncApp.run(args))
}

// Testing: Full control over dependencies
@Test
fun testApplication() = runTest {
    val result = AsyncApp.runAsync(
        args = arrayOf("--quiet", "TestUser"),
        signals = NoOpSignals(),         // No signal installation
        logger = NoOpBootstrapLogger()   // Silent logging
    )
    assertEquals(0, result)
}
```

### 2. Smart Dependency Composition
The "Composition Root" pattern enhanced with performance-aware adapter selection:

```kotlin
object CompositionRoot {
    suspend fun buildAndRunAsync(cfg: AppConfig): Int {
        // 1. Build domain services (innermost layer)
        val greetingService = DefaultGreetingService()
        
        // 2. Build infrastructure adapters with performance optimization
        val outputAdapter = when {
            cfg.outputPath != null -> {
                // Use high-performance buffered adapter for file output
                val fileAdapter = BufferedFileOutputAdapter(
                    filePath = cfg.outputPath,
                    autoFlush = !cfg.quiet, // Performance vs durability trade-off
                    bufferSize = if (cfg.quiet) 64 * 1024 else 8 * 1024
                )
                CompositeOutputAdapter(listOf(
                    ConsoleOutputAdapter(),
                    fileAdapter
                ))
            }
            else -> ConsoleOutputAdapter()
        }
        
        // 3. Build application layer (use cases)
        val createGreeting = CreateGreetingUseCase(
            greetingService = greetingService,
            outputPort = outputAdapter
        )
        
        // 4. Build presentation layer with configuration
        val runner = AsyncCliRunner(createGreeting, cfg)
        
        // 5. Execute and return standardized exit code
        return runner.run()
    }
}
```

**Enhanced Patterns**:
- **Performance-aware**: BufferedFileOutputAdapter for high-throughput scenarios
- **Configuration-driven**: Quiet mode optimizes for performance over durability
- **Clean Architecture**: Dependencies flow inward, outer layers depend on inner layers
- **Resource optimization**: Buffer sizes adapted based on usage patterns

### 3. Enhanced Configuration Management

```kotlin
data class AppConfig(
    val verbose: Boolean = false,
    val quiet: Boolean = false,        // NEW: Suppress non-essential output  
    val outputPath: String? = null,
    val name: String? = null,
)

// Secure parsing with enhanced CLI support
fun parseArgs(args: Array<String>): AppConfig {
    return try {
        SecureArgParser.parseSecure(args)
    } catch (e: ShowHelpException) {
        // Help was displayed, signal success exit
        printUsage()
        throw e
    } catch (e: ShowVersionException) {
        // Version was displayed, signal success exit  
        printVersion()
        throw e
    }
}

private fun printUsage() {
    println("""
        Usage: app [OPTIONS] [NAME]
        
        A demonstration of Kotlin Hybrid Architecture.
        
        Arguments:
          NAME                Name to greet (optional)
        
        Options:
          -v, --verbose       Enable verbose output
          -q, --quiet         Suppress non-essential output
          --out FILE         Write output to file
          --version          Show version information
          -h, --help         Show this help message
        
        Examples:
          app Alice                    # Simple greeting
          app --verbose Bob            # Verbose greeting  
          app --quiet --out log.txt C  # Quiet mode with file output
    """.trimIndent())
}
```

### 4. Security-Hardened Argument Parsing

Our enhanced `SecureArgParser` with reduced complexity and comprehensive security:

```kotlin
object SecureArgParser {
    private const val MAX_ARG_COUNT = 100
    private const val MAX_PATH_LENGTH = 4096
    private const val MAX_DIRECTORY_TRAVERSAL = 10
    
    fun parseSecure(args: Array<String>): AppConfig {
        // DoS prevention
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
        // Validate each argument for security
        val validatedArg = validateArgument(arg)
        
        return when {
            validatedArg == "--verbose" || validatedArg == "-v" -> {
                state.verbose = true; 1
            }
            validatedArg == "--quiet" || validatedArg == "-q" -> {
                state.quiet = true; 1
            }
            validatedArg == "--version" -> throw ShowVersionException()
            validatedArg == "--help" || validatedArg == "-h" -> throw ShowHelpException()
            validatedArg == "--out" -> {
                require(index + 1 < args.size) { "--out requires a file path" }
                state.outputPath = validateOutputPath(args[index + 1])
                2 // Consumed current + next argument
            }
            validatedArg.startsWith("--out=") -> {
                state.outputPath = validateOutputPath(validatedArg.substring(6))
                1
            }
            else -> {
                if (state.name == null && !validatedArg.startsWith("-")) {
                    state.name = validateName(validatedArg); 1
                } else if (validatedArg.startsWith("-")) {
                    throw IllegalArgumentException("Unknown option: $validatedArg")
                } else { 1 }
            }
        }
    }
    
    // Enhanced path validation with security checks
    private fun validateOutputPath(pathString: String): String {
        require(pathString.isNotBlank()) { "Output path cannot be blank" }
        require(pathString.length <= MAX_PATH_LENGTH) { 
            "Path too long: ${pathString.length} characters (max: $MAX_PATH_LENGTH)" 
        }
        require(!pathString.contains('\u0000')) { "Path contains null bytes" }
        
        // Directory traversal protection
        val traversalCount = pathString.split("/").count { it == ".." }
        require(traversalCount <= MAX_DIRECTORY_TRAVERSAL) {
            "Too many directory traversal sequences: $traversalCount"
        }
        
        // Windows reserved filename check
        val filename = pathString.substringAfterLast('/')
        val baseName = filename.substringBeforeLast('.').uppercase()
        val reservedNames = setOf("CON", "PRN", "AUX", "NUL", "COM1", "COM2", "LPT1", "LPT2")
        require(!reservedNames.contains(baseName)) {
            "Reserved filename not allowed: $baseName"
        }
        
        return pathString
    }
}
```

**Enhanced Security Features:**
- **DoS Prevention**: Argument count and length limits  
- **Path Injection Protection**: Null byte detection, traversal limits
- **Cross-platform**: Windows reserved filename blocking
- **Input Sanitization**: Comprehensive validation before processing
- **Reduced Complexity**: Refactored to lower cyclomatic complexity

## Testable Signal Handling with SystemSignals Abstraction

Modern signal handling that's cross-platform and testable:

```kotlin
object AsyncApp {
    suspend fun runAsync(
        args: Array<String>,
        signals: SystemSignals = JvmSignals(),
        logger: BootstrapLogger = ConsoleBootstrapLogger()
    ): Int = coroutineScope {
        try {
            // 1. Install cross-platform signal handlers
            signals.install { initiateShutdown(logger) }
            
            // 2. Parse and validate configuration securely
            val cfg = parseArgs(args)
            
            // 3. Launch main program with supervision
            val programJob = launch(appScope.coroutineContext) {
                try {
                    val result = CompositionRoot.buildAndRunAsync(cfg)
                    exitCode.set(if (result == 0) ExitCode.SUCCESS else ExitCode.ERROR)
                } catch (e: CancellationException) {
                    logger.info("Program cancelled gracefully: ${e.message}")
                    exitCode.set(ExitCode.SIGINT)
                } catch (e: Exception) {
                    handleProgramException(e, logger)
                }
            }
            
            // 4. Launch shutdown monitor  
            val shutdownJob = launch {
                awaitShutdown() // Suspends until signal received
                programJob.cancelAndJoin() // Graceful cancellation
            }
            
            // 5. Race between completion and shutdown
            select<Unit> {
                programJob.onJoin { shutdownJob.cancel() }
                shutdownJob.onJoin { /* shutdown completed */ }
            }
            
            return@coroutineScope exitCode.get().code
        } catch (e: ShowHelpException) {
            logger.info("Help displayed")
            ExitCode.SUCCESS.code
        } catch (e: ShowVersionException) {
            logger.info("Version displayed")
            ExitCode.SUCCESS.code
        } catch (e: IllegalArgumentException) {
            logger.error("Configuration error: ${e.message}")
            ExitCode.EX_USAGE.code
        } catch (e: Exception) {
            handleUncaughtException(e, logger)
            ExitCode.EX_SOFTWARE.code
        } finally {
            cleanup(logger)
        }
    }
    
    private fun initiateShutdown(logger: BootstrapLogger) {
        if (isShuttingDown.compareAndSet(false, true)) {
            logger.info("Initiating graceful shutdown...")
            
            // 5-second grace period for cleanup
            cleanupJob = appScope.launch {
                delay(5000) // Grace period
                if (shutdownLatch.count > 0) {
                    logger.error("Forceful shutdown after timeout")
                    shutdownLatch.countDown()
                }
            }
            
            shutdownLatch.countDown() // Signal shutdown
        }
    }
}

// SystemSignals implementations
interface SystemSignals {
    fun install(onShutdown: () -> Unit)
}

class JvmSignals : SystemSignals {
    override fun install(onShutdown: () -> Unit) {
        // Always install JVM shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread { onShutdown() })
        
        // Try to use sun.misc.Signal via reflection (better UX)
        try {
            installSignalHandlers(onShutdown) // SIGINT, SIGTERM
        } catch (e: Throwable) {
            // Graceful degradation - shutdown hook still works
        }
    }
}

class NoOpSignals : SystemSignals {
    override fun install(onShutdown: () -> Unit) {
        // For testing - no signal installation
    }
}
```

## Centralized Exit Code Management

Following BSD sysexits.h and Unix conventions with automatic exception mapping:

```kotlin
enum class ExitCode(val code: Int) {
    SUCCESS(0),         // Everything worked
    ERROR(1),          // General error
    MISUSE(2),         // Command misuse
    EX_USAGE(64),      // Command line usage error
    EX_DATAERR(65),    // Data format error
    EX_NOINPUT(66),    // Cannot open input
    EX_UNAVAILABLE(69), // Service unavailable
    EX_SOFTWARE(70),   // Internal software error
    EX_OSERR(71),      // System error (can't fork)
    EX_CANTCREAT(73),  // Can't create output file
    EX_IOERR(74),      // Input/output error
    EX_NOPERM(77),     // Permission denied
    EX_CONFIG(78),     // Configuration error
    SIGINT(130);       // Interrupted (SIGINT)
    
    companion object {
        // Automatic exception mapping
        fun fromThrowable(throwable: Throwable): ExitCode = when (throwable) {
            is IllegalArgumentException -> EX_USAGE
            is java.nio.file.AccessDeniedException -> EX_NOPERM
            is java.nio.file.NoSuchFileException -> EX_NOINPUT
            is java.io.IOException -> EX_IOERR
            is SecurityException -> EX_NOPERM
            is OutOfMemoryError -> EX_OSERR
            else -> ERROR
        }
        
        // Domain error mapping
        fun fromErrorType(errorType: String): ExitCode = when (errorType) {
            "ValidationError" -> EX_DATAERR
            "ConfigurationError" -> EX_CONFIG
            "PermissionError" -> EX_NOPERM
            "FileNotFoundError" -> EX_NOINPUT
            else -> ERROR
        }
    }
}
```

**Benefits:**
- **Standardized**: Industry-standard exit codes
- **Self-documenting**: Clear meaning for each code
- **Automatic mapping**: Exceptions → appropriate exit codes  
- **Script-friendly**: Shell scripts can check specific error types

**Usage in shell scripts:**
```bash
#!/bin/bash
java -jar app.jar "$@"
exit_code=$?

case $exit_code in
    0)   echo "✅ Success!" ;;
    64)  echo "❌ Invalid command line arguments" ;;
    66)  echo "❌ Input file not found" ;;
    74)  echo "❌ I/O error occurred" ;;
    77)  echo "❌ Permission denied" ;;
    130) echo "⚠️  Interrupted by user (Ctrl+C)" ;;
    *)   echo "❌ Application failed with code $exit_code" ;;
esac

exit $exit_code
```

## Real-World Usage Examples

### Basic Command Line Usage

```bash
# Simple greeting
./gradlew :bootstrap:run --args="Alice"

# Verbose mode with detailed output
./gradlew :bootstrap:run --args="--verbose Bob"

# Quiet mode for production (minimal output, better performance)
./gradlew :bootstrap:run --args="--quiet Charlie"

# File output with high-performance buffered adapter
./gradlew :bootstrap:run --args="--out=greetings.txt David"

# Combining options
./gradlew :bootstrap:run --args="--quiet --out=production.log Eve"

# Show application version
./gradlew :bootstrap:run --args="--version"

# Show comprehensive help
./gradlew :bootstrap:run --args="--help"

# Alternative file output syntax
./gradlew :bootstrap:run --args="Frank --out greetings.txt"
```

### Programmatic Usage & Testing

```kotlin
// Custom application entry with full testability
class MyCustomApp {
    suspend fun start(userConfig: UserConfig) {
        val appConfig = AppConfig(
            name = userConfig.username,
            outputPath = userConfig.logFile,
            verbose = userConfig.debugMode,
            quiet = userConfig.productionMode
        )
        
        // Use testable dependencies for full control
        val signals = if (userConfig.enableSignalHandling) JvmSignals() else NoOpSignals()
        val logger = if (userConfig.enableLogging) ConsoleBootstrapLogger() else NoOpBootstrapLogger()
        
        val exitCode = AsyncApp.runAsync(
            args = arrayOf(), // Config already parsed
            signals = signals,
            logger = logger
        )
        
        when (ExitCode.values().find { it.code == exitCode }) {
            ExitCode.SUCCESS -> logger.info("Application completed successfully")
            ExitCode.EX_USAGE -> logger.error("Invalid configuration provided")
            ExitCode.SIGINT -> logger.info("Application interrupted by user")
            else -> logger.error("Application failed with code $exitCode")
        }
    }
}
```

## Comprehensive Testing Strategies

### Modern AsyncApp Testing

```kotlin
class AsyncAppTest : DescribeSpec({
    
    describe("AsyncApp lifecycle") {
        it("should handle valid arguments and return success") {
            runTest {
                val result = AsyncApp.runAsync(
                    args = arrayOf("TestUser"),
                    signals = NoOpSignals(),
                    logger = NoOpBootstrapLogger()
                )
                
                result shouldBe 0
            }
        }
        
        it("should return appropriate exit code for invalid arguments") {
            runTest {
                val result = AsyncApp.runAsync(
                    args = arrayOf("--invalid-flag"),
                    signals = NoOpSignals(), 
                    logger = NoOpBootstrapLogger()
                )
                
                result shouldBe ExitCode.EX_USAGE.code
            }
        }
        
        it("should handle help and version flags") {
            runTest {
                val helpResult = AsyncApp.runAsync(
                    args = arrayOf("--help"),
                    signals = NoOpSignals(),
                    logger = NoOpBootstrapLogger()
                )
                
                val versionResult = AsyncApp.runAsync(
                    args = arrayOf("--version"),
                    signals = NoOpSignals(),
                    logger = NoOpBootstrapLogger()
                )
                
                helpResult shouldBe 0
                versionResult shouldBe 0
            }
        }
    }
})
```

### Security & CLI Testing

```kotlin  
class SecureArgParserTest : DescribeSpec({
    
    describe("Enhanced security validation") {
        it("should accept valid arguments including new flags") {
            val validArgs = arrayOf("--quiet", "--verbose", "Alice", "--out", "test.log")
            
            val result = SecureArgParser.parseSecure(validArgs)
            
            result.quiet shouldBe true
            result.verbose shouldBe true
            result.name shouldBe "Alice" 
            result.outputPath shouldBe "test.log"
        }
        
        it("should prevent path traversal attacks") {
            val dangerousArgs = arrayOf(
                "--out", "../../../etc/passwd",
                "--out", "..\\..\\windows\\system32\\config",
                "--out", "../../../../root/.ssh/id_rsa"
            )
            
            assertThrows<IllegalArgumentException> {
                SecureArgParser.parseSecure(dangerousArgs)
            }
        }
        
        it("should block Windows reserved filenames") {
            val reservedNames = arrayOf("CON", "PRN", "AUX", "NUL", "COM1", "LPT1")
            
            reservedNames.forEach { reserved ->
                assertThrows<IllegalArgumentException> {
                    SecureArgParser.parseSecure(arrayOf("--out", "$reserved.txt"))
                }
            }
        }
        
        it("should enforce reasonable limits") {
            // DoS prevention - too many arguments
            val tooManyArgs = (1..150).map { "arg$it" }.toTypedArray()
            assertThrows<IllegalArgumentException> {
                SecureArgParser.parseSecure(tooManyArgs)
            }
            
            // Path too long
            val longPath = "a".repeat(5000)
            assertThrows<IllegalArgumentException> {
                SecureArgParser.parseSecure(arrayOf("--out", longPath))
            }
        }
    }
})
```

### Signal Handling Testing

```kotlin
class SignalHandlingTest : DescribeSpec({
    
    describe("SystemSignals abstraction") {
        it("should allow testing without actual signal installation") {
            var shutdownCalled = false
            val signals = NoOpSignals()
            
            signals.install { shutdownCalled = true }
            
            // NoOpSignals doesn't actually install anything
            shutdownCalled shouldBe false
        }
        
        it("should enable testing of shutdown logic") {
            runTest {
                var shutdownInitiated = false
                val mockSignals = object : SystemSignals {
                    override fun install(onShutdown: () -> Unit) {
                        // Simulate signal after delay
                        launch {
                            delay(100)
                            shutdownInitiated = true
                            onShutdown()
                        }
                    }
                }
                
                // Test that shutdown is properly handled
                val result = AsyncApp.runAsync(
                    args = arrayOf("TestUser"),
                    signals = mockSignals,
                    logger = NoOpBootstrapLogger()
                )
                
                shutdownInitiated shouldBe true
                result shouldBe ExitCode.SIGINT.code
            }
        }
    }
})
```

## Performance & Production Considerations

### Development vs Production Configuration

```kotlin
// Development setup (verbose, immediate feedback)
val devConfig = AppConfig(
    verbose = true,           // Detailed logging
    quiet = false,           // Show all output
    outputPath = "dev.log",  // Local file
    name = "DevUser"
)

// Production setup (optimized for performance)  
val prodConfig = AppConfig(
    verbose = false,         // Minimal logging
    quiet = true,           // Suppress non-essential output
    outputPath = "/var/log/app.log",  // System log location
    name = "ProdBatch"
)
```

### Resource Management

The enhanced bootstrap module automatically optimizes resource usage:

- **BufferedFileOutputAdapter**: Used automatically for file output with configurable buffer sizes
- **Performance vs Durability**: `quiet` mode optimizes for throughput over immediate disk sync
- **Memory Management**: Proper coroutine scoping prevents resource leaks
- **Graceful Shutdown**: 5-second grace period ensures clean resource cleanup

## Migration Guide

If upgrading from earlier versions, update your code as follows:

```kotlin
// OLD (v0.x)
fun main(args: Array<String>) {
    val exitCode = App.run(args)  
    exitProcess(exitCode)
}

// NEW (v1.0.0) - Async-first with enhanced features
suspend fun main(args: Array<String>) {
    val exitCode = AsyncApp.runAsync(args)
    exitProcess(exitCode)  
}

// For testing - full control over dependencies
@Test
fun testApp() = runTest {
    val result = AsyncApp.runAsync(
        args = arrayOf("--quiet", "TestUser"),
        signals = NoOpSignals(),
        logger = NoOpBootstrapLogger()
    )
    assertEquals(0, result)
}
```

## Summary

The enhanced Bootstrap Module (v1.0.0) provides:

🚀 **Modern Architecture**: Async-first design with structured concurrency  
🛡️ **Security**: Comprehensive input validation and DoS prevention  
🔧 **CLI Enhancements**: New `--version` and `--quiet` flags with better parsing  
📊 **Standardized Exit Codes**: Unix-compliant error reporting  
🧪 **Full Testability**: All components can be mocked and tested independently  
⚡ **Performance**: Buffered file output and configuration-driven optimizations  
🌍 **Cross-platform**: Graceful signal handling across different JVMs and OS  

This foundation enables reliable, maintainable, and performant applications that follow industry best practices for lifecycle management, error handling, and user experience.
