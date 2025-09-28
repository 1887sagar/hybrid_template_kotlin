<!--
  Kotlin Hybrid Architecture Template - Documentation
  Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
  SPDX-License-Identifier: BSD-3-Clause
  See LICENSE file in the project root.
-->

# Presentation Layer Documentation

## Overview

The presentation layer handles all user interaction, whether through a command-line interface (CLI), REST API, or graphical user interface. This layer is responsible for accepting user input, invoking application use cases, and presenting results back to the user in an appropriate format.

## Key Principles

1. **No Business Logic** - Only presentation concerns
2. **Application Layer Dependency** - Depends only on application layer, never domain or infrastructure
3. **User Experience Focus** - Handle user input validation, formatting, and error messages
4. **Technology Specific** - Can use framework-specific features (CLI parsers, web frameworks, etc.)

## Components

### CLI Factory

The CLI factory creates runnable command-line programs by wiring together application use cases with presentation configuration.

```kotlin
fun cli(
    cfg: PresentationConfig, 
    createGreeting: CreateGreetingInputPort
): Runnable = Runnable {
    try {
        runBlocking {
            either {
                // Create command from config
                val command = CreateGreetingCommand(
                    name = cfg.name,
                    silent = false,
                )
                
                // Show helpful tips
                if (cfg.name == null) {
                    println("Tip: You can provide your name as a command-line argument")
                }
                
                // Execute use case
                createGreeting.execute(command).bind()
                
                // Verbose logging
                if (cfg.verbose) {
                    println("[INFO] Greeting delivered successfully")
                }
            }.fold(
                { error -> /* Handle error */ },
                { /* Success - output already handled */ }
            )
        }
    } catch (e: CancellationException) {
        throw e // Let coroutine cancellation propagate
    } catch (e: Exception) {
        System.err.println("Unexpected error: ${e.message}")
    }
}
```

**Key Features**:
- **Factory Function** - Returns a Runnable for easy composition
- **Dependency Injection** - Accepts use case through interface
- **User-Friendly** - Provides helpful tips and verbose mode
- **Error Handling** - Transforms application errors to user messages
- **Coroutine Support** - Properly handles async operations

### Runnable Interface

A simple functional interface for executable components:

```kotlin
fun interface Runnable {
    fun run()
}
```

**Benefits**:
- **SAM Conversion** - Can use lambda syntax
- **Framework Agnostic** - Not tied to any specific framework
- **Composable** - Easy to wrap with decorators

### Presentation Configuration

Configuration specific to the presentation layer:

```kotlin
data class PresentationConfig(
    val verbose: Boolean,
    val outputPath: String?,
    val name: String?,
)
```

**Purpose**:
- **User Preferences** - Verbose mode, output preferences
- **Input Data** - User-provided values
- **Presentation Options** - How to display results

## Error Handling and User Experience

### Error Message Formatting

Transform technical errors into user-friendly messages:

```kotlin
private fun formatError(error: ApplicationError): String = when (error) {
    is ApplicationError.OutputError -> 
        "Failed to deliver greeting: ${error.message}"
        
    is ApplicationError.DomainErrorWrapper -> 
        "Invalid input: ${error.userMessage}"
        
    is ApplicationError.UseCaseError -> 
        "Processing error in ${error.useCase}: ${error.cause}"
}
```

**Guidelines**:
- Use clear, non-technical language
- Provide actionable error messages
- Include relevant context
- Avoid exposing internal details

### User Input Validation

Validate user input before creating commands:

```kotlin
fun validateUserInput(input: String): Either<String, ValidatedInput> {
    return when {
        input.isBlank() -> "Input cannot be empty".left()
        input.length > 100 -> "Input too long (max 100 characters)".left()
        !input.matches(VALID_PATTERN) -> "Input contains invalid characters".left()
        else -> ValidatedInput(input).right()
    }
}
```

### Helpful User Feedback

Provide guidance and feedback to improve user experience:

```kotlin
// Provide tips for better usage
if (cfg.name == null) {
    println("Tip: You can provide your name as a command-line argument")
    println("Example: myapp --name \"John Doe\"")
}

// Confirm actions in verbose mode
if (cfg.verbose) {
    println("[INFO] Processing request for: ${cfg.name ?: "Anonymous"}")
    println("[INFO] Output will be sent to: ${cfg.outputPath ?: "console"}")
}

// Progress indication for long operations
print("Processing")
repeat(3) {
    delay(500)
    print(".")
}
println(" Done!")
```

## Common Presentation Patterns

### Command-Line Interface (CLI)

Structure for CLI applications:

```kotlin
class CliApp(
    private val useCases: UseCases
) {
    fun run(args: Array<String>) {
        val config = parseArguments(args)
        
        when (config.command) {
            Command.CREATE -> handleCreate(config)
            Command.LIST -> handleList(config)
            Command.DELETE -> handleDelete(config)
            Command.HELP -> showHelp()
        }
    }
    
    private fun parseArguments(args: Array<String>): CliConfig {
        // Use a CLI parsing library like Clikt or kotlinx-cli
    }
}
```

### REST API Controllers (Future)

Example structure for REST endpoints:

```kotlin
class GreetingController(
    private val createGreeting: CreateGreetingInputPort
) {
    suspend fun create(request: CreateRequest): Response {
        val command = CreateGreetingCommand(
            name = request.name,
            silent = false
        )
        
        return createGreeting.execute(command).fold(
            { error -> 
                Response.error(
                    status = errorToHttpStatus(error),
                    message = formatError(error)
                )
            },
            { result -> 
                Response.success(
                    data = GreetingResponse(
                        message = result.message,
                        recipient = result.recipient
                    )
                )
            }
        )
    }
}
```

### Graphical User Interface (Future)

Example structure for GUI components:

```kotlin
class GreetingViewModel(
    private val createGreeting: CreateGreetingInputPort
) : ViewModel() {
    private val _state = MutableStateFlow(GreetingState())
    val state: StateFlow<GreetingState> = _state
    
    fun onNameChanged(name: String) {
        _state.update { it.copy(name = name) }
    }
    
    fun onCreateClicked() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            createGreeting.execute(
                CreateGreetingCommand(name = state.value.name)
            ).fold(
                { error -> 
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = formatError(error)
                        )
                    }
                },
                { result -> 
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            greeting = result.message
                        )
                    }
                }
            )
        }
    }
}
```

## Best Practices

### 1. Keep It Thin

The presentation layer should be a thin adapter:

```kotlin
// Good: Thin presentation logic
fun handleUserInput(input: String, useCase: SomeInputPort) {
    val command = SomeCommand(input)
    useCase.execute(command).fold(
        { error -> showError(error) },
        { result -> showResult(result) }
    )
}

// Bad: Business logic in presentation
fun handleUserInput(input: String) {
    if (input.length > 10) { // Business rule!
        // This belongs in domain layer
    }
}
```

### 2. Use Appropriate Frameworks

Choose frameworks that fit your needs:

- **CLI**: Clikt, kotlinx-cli, picocli
- **Web**: Ktor, Spring Boot, Http4k
- **Desktop**: Compose Desktop, TornadoFX
- **Mobile**: Jetpack Compose, SwiftUI (via Kotlin Multiplatform)

### 3. Handle All Error Cases

Always handle both success and failure:

```kotlin
result.fold(
    { error -> 
        logger.error("Operation failed", error)
        showErrorToUser(formatError(error))
    },
    { success -> 
        logger.info("Operation successful")
        showSuccessToUser(success)
    }
)
```

### 4. Provide Clear Feedback

Users should always know what's happening:

```kotlin
println("Connecting to server...")
val result = connectToServer()

if (result.isRight()) {
    println("✓ Connected successfully")
} else {
    println("✗ Connection failed: ${formatError(result.leftOrNull()!!)}")
    println("  Try: Check your internet connection and try again")
}
```

## Testing Presentation Logic

### Unit Testing

Test presentation logic with mocked use cases:

```kotlin
class CliFactoryTest {
    private val mockUseCase = mockk<CreateGreetingInputPort>()
    
    @Test
    fun `should show tip when name not provided`() {
        val config = PresentationConfig(
            verbose = false,
            outputPath = null,
            name = null
        )
        
        val output = captureOutput {
            val runnable = cli(config, mockUseCase)
            runnable.run()
        }
        
        output shouldContain "Tip: You can provide your name"
    }
}
```

### Integration Testing

Test the full presentation flow:

```kotlin
@IntegrationTest
class CliIntegrationTest {
    @Test
    fun `should handle complete user flow`() {
        val app = TestApplication()
        
        val result = app.run(arrayOf("--name", "Alice", "--verbose"))
        
        result.exitCode shouldBe 0
        result.output shouldContain "Hello World from Alice!"
        result.output shouldContain "[INFO] Greeting delivered successfully"
    }
}
```

## Internationalization (i18n)

Structure for supporting multiple languages:

```kotlin
interface Messages {
    fun greeting(name: String): String
    fun errorInvalidInput(field: String): String
    fun tipProvideNam(): String
}

class EnglishMessages : Messages {
    override fun greeting(name: String) = "Hello World from $name!"
    override fun errorInvalidInput(field: String) = "Invalid $field"
    override fun tipProvideName() = "Tip: You can provide your name"
}

class SpanishMessages : Messages {
    override fun greeting(name: String) = "¡Hola Mundo desde $name!"
    override fun errorInvalidInput(field: String) = "$field inválido"
    override fun tipProvideName() = "Consejo: Puedes proporcionar tu nombre"
}
```

## Accessibility Considerations

Make your presentation layer accessible:

1. **Clear Error Messages** - Explain what went wrong and how to fix it
2. **Keyboard Navigation** - Ensure all features are keyboard accessible
3. **Screen Reader Support** - Use semantic markup and ARIA labels
4. **Color Contrast** - Ensure sufficient contrast for readability
5. **Alternative Formats** - Provide multiple output formats

## Performance Considerations

### Async UI Updates

Keep the UI responsive:

```kotlin
// Show immediate feedback
setState { it.copy(isLoading = true) }

// Perform async operation
launch {
    val result = useCase.execute(command)
    
    // Update UI on completion
    setState { 
        it.copy(
            isLoading = false,
            result = result
        )
    }
}
```

### Progress Reporting

For long operations:

```kotlin
useCase.executeWithProgress(command) { progress ->
    updateProgress(progress.percentage, progress.message)
}.fold(
    { error -> showError(error) },
    { result -> showResult(result) }
)
```

## Summary

The presentation layer is the face of your application. It:
- **Accepts** user input in various forms
- **Invokes** application use cases through ports
- **Formats** results for user consumption
- **Handles** user experience concerns
- **Remains** independent of business logic

By keeping this layer focused on presentation concerns, you create applications that are:
- **User-friendly** - Clear feedback and helpful messages
- **Flexible** - Easy to add new UI types
- **Testable** - Presentation logic separate from business logic
- **Maintainable** - Clear separation of concerns