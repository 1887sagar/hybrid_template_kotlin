# Bootstrap Module

## Overview

The Bootstrap module is the entry point and composition root of the application. It handles:
- Application startup and shutdown
- Dependency injection (manual DI)
- Configuration parsing
- Signal handling for graceful shutdown
- Security validation of inputs

## Core Responsibilities

1. **Entry Point**: Main function and application lifecycle
2. **Composition Root**: Wires all dependencies together
3. **Configuration**: Parses and validates command-line arguments
4. **Security**: Validates inputs to prevent injection attacks
5. **Graceful Shutdown**: Handles system signals properly

## Structure

```
bootstrap/
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── com/
│   │           └── abitofhelp/
│   │               └── hybrid/
│   │                   └── bootstrap/
│   │                       ├── App.kt              # Legacy blocking app
│   │                       ├── AsyncEntryPoint.kt  # Async main with signals
│   │                       ├── AppConfig.kt        # Configuration data
│   │                       ├── CompositionRoot.kt  # Dependency wiring
│   │                       ├── EntryPoint.kt       # Simple main entry
│   │                       └── SecureArgParser.kt  # Secure argument parsing
│   └── test/
│       └── [...test files...]
└── build.gradle.kts
```

## Key Components

### AsyncEntryPoint
The main application entry with signal handling:

```kotlin
object AsyncApp {
    suspend fun runAsync(args: Array<String>): Int {
        // 1. Install signal handlers (SIGTERM, SIGINT)
        // 2. Parse and validate arguments
        // 3. Build and run application
        // 4. Handle graceful shutdown
        // 5. Return exit code
    }
}
```

### CompositionRoot
Where all dependencies are wired together:

```kotlin
object CompositionRoot {
    suspend fun buildAndRunAsync(cfg: AppConfig): Int {
        // Infrastructure layer
        val greetingService: GreetingService = DefaultGreetingService()
        val outputPort: OutputPort = CompositeOutputAdapter.createDefault(cfg.outputPath)
        
        // Application layer
        val createGreetingUseCase: CreateGreetingInputPort = 
            CreateGreetingUseCase(greetingService, outputPort)
        
        // Presentation layer
        val presentationConfig = PresentationConfig(...)
        
        // Run
        return createAndRunCli(presentationConfig, createGreetingUseCase)
    }
}
```

### SecureArgParser
Security-focused command-line argument parser:

```kotlin
object SecureArgParser {
    fun parseSecure(args: Array<String>): AppConfig {
        // Validates against:
        // - Command injection (;, |, &, etc.)
        // - Path traversal (.., ~)
        // - Buffer overflow (length limits)
        // - Null bytes and control characters
        // - System directory writes
    }
}
```

## Security Features

### Input Validation
- **Length Limits**: Prevents DoS via huge inputs
- **Character Blacklist**: Rejects dangerous shell characters
- **Path Sanitization**: Normalizes paths, prevents traversal
- **System Protection**: Blocks writes to OS directories

### Dangerous Patterns Blocked
```kotlin
private val DANGEROUS_PATTERNS = listOf(
    "..",      // Path traversal
    "~",       // Home directory expansion  
    "$",       // Variable expansion
    "`",       // Command substitution
    ";",       // Command separator
    "&",       // Background execution
    "|",       // Pipe
    ">", "<",  // Redirects
    "\n", "\r" // Line injection
)
```

## Signal Handling

### Supported Signals
- **SIGTERM (15)**: Graceful shutdown request
- **SIGINT (2)**: User pressed Ctrl+C
- **SIGHUP (1)**: Terminal disconnected (Unix only)

### Graceful Shutdown Process
1. Signal received
2. 5-second grace period starts
3. Running operations allowed to complete
4. Resources cleaned up
5. Exit with appropriate code

## Exit Codes

Following Unix conventions:
- `0`: Success
- `1`: General error
- `2`: Uncaught exception
- `3`: Invalid state
- `4`: Invalid arguments
- `126`: Permission denied
- `130`: Interrupted (SIGINT)
- `134`: Stack overflow
- `137`: Out of memory

## Configuration

### Command-Line Arguments
```bash
# Basic usage
app [OPTIONS] [NAME]

# Options
-v, --verbose     Enable verbose output
--out PATH        Write output to file
-h, --help        Show help message

# Examples
app John
app --verbose Alice
app Bob --out greetings.txt
app --out=output.txt --verbose Charlie
```

### Environment Variables
- `HYBRID_ASYNC_MODE`: Controls async behavior (default: true)

## Usage Examples

### Basic Async Entry
```kotlin
suspend fun main(args: Array<String>) {
    val exitCode = AsyncApp.runAsync(args)
    exitProcess(exitCode)
}
```

### With Custom Configuration
```kotlin
val config = AppConfig(
    verbose = true,
    outputPath = "/tmp/output.txt",
    name = "Alice"
)

val exitCode = CompositionRoot.buildAndRunAsync(config)
```

## Testing

### Unit Tests
- Argument parsing validation
- Security validation rules
- Configuration building

### Integration Tests
- Full application startup
- Signal handling
- Graceful shutdown

## Best Practices

1. **Security First**: Always validate external inputs
2. **Fail Fast**: Reject invalid input immediately
3. **Clear Errors**: Provide helpful error messages
4. **Resource Cleanup**: Always clean up on shutdown
5. **Exit Codes**: Use standard codes for scripts

## Common Issues

### Signal Handling on Different Platforms
- **Unix/Linux/Mac**: Full signal support
- **Windows**: Limited to SIGINT and SIGTERM
- **Containers**: May need special handling for SIGTERM

### Debugging Tips
- Use `--verbose` for detailed logging
- Check exit codes for error types
- Monitor signal handling with system tools

## Future Enhancements

- Configuration file support
- Environment variable mapping
- Plugin system initialization
- Health check endpoints
- Metrics collection startup