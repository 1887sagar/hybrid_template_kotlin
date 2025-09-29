# Presentation Module

**Version:** 1.0.0  
**Date:** September 29, 2025
**License:** BSD-3-Clause  
**Copyright:** © 2025 Michael Gardner, A Bit of Help, Inc.  
**Authors:** Michael Gardner  
**Status:** Released

## What is the Presentation Module?

The presentation module is your application's "face" - it's how users interact with your system. Whether through a command line, web API, or graphical interface, this layer handles all the user-facing concerns while keeping the complexity hidden behind clean interfaces.

## Why the Presentation Layer Matters

Think of the presentation layer as the "waitstaff" at a restaurant:
- Takes orders from customers (handles user input)
- Translates requests to the kitchen (calls use cases)
- Presents dishes nicely (formats responses)
- Handles special requests (error handling)
- Never cooks the food themselves (no business logic)

## Current Implementation: CLI Interface

Our template currently includes a command-line interface that demonstrates key presentation patterns:

### CLI Runner - The Main Entry Point

```kotlin
class AsyncCliRunner(
    private val createGreetingUseCase: CreateGreetingInputPort,
    private val exitHandler: suspend () -> Unit = { exitProcess(0) }
) {
    suspend fun run(args: Array<String>) {
        val config = parseArgs(args)
        
        val command = CreateGreetingCommand(
            name = config.name,
            silent = config.silent
        )
        
        // Call the use case and handle the result
        createGreetingUseCase.execute(command).fold(
            { error -> 
                System.err.println("Error: ${error.message}")
                exitProcess(1)
            },
            { result -> 
                if (!config.silent) {
                    println("Greeting created: ${result.greeting}")
                }
                exitHandler()
            }
        )
    }
}
```

**Key Responsibilities:**
- Parse command-line arguments
- Transform them into application commands  
- Call appropriate use cases
- Handle errors gracefully
- Format output for users

### Pure Async CLI - The Interface Contract

```kotlin
interface PureAsyncCli {
    suspend fun run(args: Array<String>)
}

class PureAsyncCliImpl(
    private val runner: AsyncCliRunner
) : PureAsyncCli {
    override suspend fun run(args: Array<String>) {
        runner.run(args)
    }
}
```

This shows the **Adapter Pattern** - the interface defines what we need, and the implementation provides how it's done.

## Expanding to Other Interfaces

### Web API Example

Here's how you might add a REST API to the same application:

```kotlin
@RestController
@RequestMapping("/api/v1")
class GreetingController(
    private val createGreetingUseCase: CreateGreetingInputPort
) {
    
    @PostMapping("/greetings")
    suspend fun createGreeting(
        @RequestBody request: CreateGreetingRequest
    ): ResponseEntity<GreetingResponse> {
        
        val command = CreateGreetingCommand(
            name = request.name,
            silent = false  // API responses aren't silent
        )
        
        return createGreetingUseCase.execute(command).fold(
            { error -> 
                ResponseEntity.badRequest()
                    .body(ErrorResponse(error.message))
            },
            { result -> 
                ResponseEntity.ok(
                    GreetingResponse(
                        greeting = result.greeting,
                        personName = result.personName
                    )
                )
            }
        )
    }
}
```

**Notice:** Both CLI and REST API use the exact same use case - just different presentation!

### GraphQL Example

```kotlin
@Component
class GreetingResolver(
    private val createGreetingUseCase: CreateGreetingInputPort
) {
    
    @SchemaMapping(typeName = "Mutation", field = "createGreeting")
    suspend fun createGreeting(
        @Argument input: CreateGreetingInput
    ): GreetingPayload {
        
        val command = CreateGreetingCommand(
            name = input.name,
            silent = false
        )
        
        return createGreetingUseCase.execute(command).fold(
            { error -> 
                GreetingPayload(
                    greeting = null,
                    error = error.message
                )
            },
            { result -> 
                GreetingPayload(
                    greeting = Greeting(
                        message = result.greeting,
                        personName = result.personName
                    ),
                    error = null
                )
            }
        )
    }
}
```

## Request/Response Pattern

### Input DTOs (Requests)

```kotlin
// For CLI
data class CliConfig(
    val name: String?,
    val silent: Boolean = false
)

// For REST API
data class CreateGreetingRequest(
    @field:Size(max = 100, message = "Name cannot exceed 100 characters")
    val name: String?
)

// For GraphQL
data class CreateGreetingInput(
    val name: String?
)
```

### Output DTOs (Responses)

```kotlin
// For REST API
data class GreetingResponse(
    val greeting: String,
    val personName: String,
    val timestamp: Instant = Instant.now()
)

// For GraphQL
data class GreetingPayload(
    val greeting: Greeting?,
    val error: String?
)

data class Greeting(
    val message: String,
    val personName: String
)
```

## Error Handling Patterns

### HTTP Error Mapping

```kotlin
@ControllerAdvice
class GlobalExceptionHandler {
    
    @ExceptionHandler(ApplicationError::class)
    fun handleApplicationError(
        error: ApplicationError
    ): ResponseEntity<ErrorResponse> {
        
        val (status, message) = when (error) {
            is ApplicationError.ValidationError -> 
                HttpStatus.BAD_REQUEST to "Invalid input: ${error.message}"
            is ApplicationError.BusinessRuleError -> 
                HttpStatus.UNPROCESSABLE_ENTITY to error.message
            is ApplicationError.NotFoundError -> 
                HttpStatus.NOT_FOUND to "Resource not found"
            else -> 
                HttpStatus.INTERNAL_SERVER_ERROR to "An unexpected error occurred"
        }
        
        return ResponseEntity.status(status)
            .body(ErrorResponse(
                code = error.code,
                message = message,
                timestamp = Instant.now()
            ))
    }
}
```

### CLI Error Handling

```kotlin
fun handleCliError(error: ApplicationError): Int {
    val (exitCode, message) = when (error) {
        is ApplicationError.ValidationError -> 2 to "Invalid input: ${error.message}"
        is ApplicationError.BusinessRuleError -> 3 to "Error: ${error.message}"
        else -> 1 to "Unexpected error: ${error.message}"
    }
    
    System.err.println(message)
    return exitCode
}
```

## Input Validation

### Bean Validation (REST/GraphQL)

```kotlin
data class CreateUserRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Must be a valid email")
    val email: String,
    
    @field:NotBlank(message = "Name is required")
    @field:Size(min = 2, max = 50, message = "Name must be 2-50 characters")
    val name: String,
    
    @field:Min(value = 18, message = "Must be at least 18 years old")
    val age: Int
)
```

### CLI Validation

```kotlin
fun parseAndValidateArgs(args: Array<String>): Either<ValidationError, CliConfig> {
    if (args.isEmpty()) {
        return CliConfig(name = null).right()
    }
    
    val name = args[0]
    if (name.length > 100) {
        return ValidationError("Name cannot exceed 100 characters").left()
    }
    
    val silent = args.contains("--silent")
    
    return CliConfig(name = name, silent = silent).right()
}
```

## Security Considerations

### Authentication (REST)

```kotlin
@RestController
class SecureGreetingController(
    private val createGreetingUseCase: CreateGreetingInputPort
) {
    
    @PostMapping("/greetings")
    @PreAuthorize("hasRole('USER')")
    suspend fun createGreeting(
        authentication: Authentication,
        @RequestBody request: CreateGreetingRequest
    ): ResponseEntity<GreetingResponse> {
        
        val userId = authentication.name
        val command = CreateGreetingCommand(
            name = request.name ?: userId,
            silent = false
        )
        
        // Rest of the implementation...
    }
}
```

### Rate Limiting

```kotlin
@Component
class RateLimitingInterceptor : HandlerInterceptor {
    
    private val rateLimiter = RateLimiter.create(10.0) // 10 requests per second
    
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        if (!rateLimiter.tryAcquire()) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.writer.write("Rate limit exceeded")
            return false
        }
        return true
    }
}
```

## Testing Strategies

### Controller Tests

```kotlin
@WebMvcTest(GreetingController::class)
class GreetingControllerTest {
    
    @MockBean
    lateinit var createGreetingUseCase: CreateGreetingInputPort
    
    @Autowired
    lateinit var mockMvc: MockMvc
    
    @Test
    fun `should create greeting successfully`() {
        // Given
        val request = CreateGreetingRequest("Alice")
        val expectedResult = GreetingResult("Hello, Alice!", "Alice")
        
        every { createGreetingUseCase.execute(any()) } returns expectedResult.right()
        
        // When & Then
        mockMvc.post("/api/v1/greetings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.greeting") { value("Hello, Alice!") }
            jsonPath("$.personName") { value("Alice") }
        }
    }
    
    @Test
    fun `should handle validation errors`() {
        // Given
        val invalidRequest = CreateGreetingRequest("A".repeat(101)) // Too long
        
        // When & Then
        mockMvc.post("/api/v1/greetings") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(invalidRequest)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Name cannot exceed 100 characters") }
        }
    }
}
```

### CLI Tests

```kotlin
class AsyncCliRunnerTest : DescribeSpec({
    
    describe("CLI Runner") {
        val mockUseCase = mockk<CreateGreetingInputPort>()
        var exitCode = 0
        val exitHandler = suspend { exitCode = 0 }
        
        val runner = AsyncCliRunner(mockUseCase, exitHandler)
        
        it("should handle successful greeting creation") {
            // Given
            val result = GreetingResult("Hello, Alice!", "Alice")
            every { mockUseCase.execute(any()) } returns result.right()
            
            // When
            runBlocking { runner.run(arrayOf("Alice")) }
            
            // Then
            exitCode shouldBe 0
            verify { mockUseCase.execute(any()) }
        }
        
        it("should handle use case errors") {
            // Given
            val error = ApplicationError.ValidationError("name", "Invalid name")
            every { mockUseCase.execute(any()) } returns error.left()
            
            // When
            runBlocking { runner.run(arrayOf("")) }
            
            // Then
            exitCode shouldBe 1
        }
    }
})
```

## API Documentation

### OpenAPI/Swagger

```kotlin
@Operation(
    summary = "Create a greeting",
    description = "Creates a personalized greeting message for a user"
)
@ApiResponses(
    value = [
        ApiResponse(
            responseCode = "200",
            description = "Greeting created successfully",
            content = [Content(schema = Schema(implementation = GreetingResponse::class))]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ]
)
@PostMapping("/greetings")
suspend fun createGreeting(
    @Parameter(description = "Greeting request details")
    @RequestBody request: CreateGreetingRequest
): ResponseEntity<GreetingResponse>
```

## Content Negotiation

Support multiple response formats:

```kotlin
@GetMapping("/greetings/{id}", produces = [
    MediaType.APPLICATION_JSON_VALUE,
    MediaType.APPLICATION_XML_VALUE,
    "text/csv"
])
suspend fun getGreeting(
    @PathVariable id: String,
    @RequestHeader("Accept") acceptHeader: String
): ResponseEntity<Any> {
    
    return createGreetingUseCase.execute(command).fold(
        { error -> ResponseEntity.badRequest().body(error) },
        { result ->
            val response = when {
                acceptHeader.contains("xml") -> result.toXml()
                acceptHeader.contains("csv") -> result.toCsv()
                else -> result.toJson()
            }
            ResponseEntity.ok(response)
        }
    )
}
```

## Performance Optimization

### Caching

```kotlin
@RestController
class CachedGreetingController(
    private val createGreetingUseCase: CreateGreetingInputPort
) {
    
    @GetMapping("/greetings/{name}")
    @Cacheable(value = ["greetings"], key = "#name")
    suspend fun getGreeting(@PathVariable name: String): ResponseEntity<GreetingResponse> {
        
        val command = CreateGreetingCommand(name = name)
        
        return createGreetingUseCase.execute(command).fold(
            { error -> ResponseEntity.badRequest().body(error.toResponse()) },
            { result -> ResponseEntity.ok(result.toResponse()) }
        )
    }
}
```

### Async Processing

```kotlin
@PostMapping("/greetings/async")
suspend fun createGreetingAsync(
    @RequestBody request: CreateGreetingRequest
): ResponseEntity<AsyncResponse> {
    
    val taskId = UUID.randomUUID().toString()
    
    // Process asynchronously
    GlobalScope.launch {
        val command = request.toCommand()
        createGreetingUseCase.execute(command)
        // Store result with taskId for later retrieval
    }
    
    return ResponseEntity.accepted()
        .body(AsyncResponse(
            taskId = taskId,
            status = "PROCESSING",
            statusUrl = "/api/v1/tasks/$taskId"
        ))
}
```

## Module Structure

```
presentation/
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── com/abitofhelp/hybrid/presentation/
│   │           ├── cli/                # Command-line interface
│   │           │   ├── AsyncCliRunner.kt
│   │           │   ├── PureAsyncCli.kt
│   │           │   └── CliFactory.kt
│   │           ├── api/                # Web API (when added)
│   │           │   ├── rest/           # REST controllers
│   │           │   ├── graphql/        # GraphQL resolvers
│   │           │   └── websocket/      # WebSocket handlers
│   │           ├── dto/                # Request/Response DTOs
│   │           ├── validation/         # Input validation
│   │           ├── security/           # Security configuration
│   │           └── error/              # Error handlers
│   └── test/
│       └── kotlin/
│           └── com/abitofhelp/hybrid/presentation/
│               ├── cli/                # CLI tests
│               └── api/                # API tests
└── build.gradle.kts
```

## Common Patterns and Solutions

### Pattern: Command Factory

```kotlin
object CommandFactory {
    fun createGreetingCommand(
        request: CreateGreetingRequest,
        userContext: UserContext? = null
    ): CreateGreetingCommand {
        return CreateGreetingCommand(
            name = request.name ?: userContext?.displayName,
            silent = request.silent ?: false
        )
    }
}
```

### Pattern: Response Builder

```kotlin
class GreetingResponseBuilder {
    fun build(
        result: GreetingResult,
        request: HttpServletRequest
    ): GreetingResponse {
        return GreetingResponse(
            greeting = result.greeting,
            personName = result.personName,
            timestamp = Instant.now(),
            metadata = ResponseMetadata(
                requestId = request.getHeader("X-Request-ID"),
                version = "1.0"
            )
        )
    }
}
```

### Pattern: Middleware/Filters

```kotlin
@Component
class LoggingFilter : Filter {
    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain
    ) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse
        
        val startTime = System.currentTimeMillis()
        
        try {
            chain.doFilter(request, response)
        } finally {
            val duration = System.currentTimeMillis() - startTime
            logger.info(
                "Request: {} {} - Status: {} - Duration: {}ms",
                httpRequest.method,
                httpRequest.requestURI,
                httpResponse.status,
                duration
            )
        }
    }
}
```

## Best Practices Checklist

### ✅ DO:
- Keep controllers thin - only handle HTTP concerns
- Validate input at the boundary
- Transform between DTOs and commands/queries
- Handle errors gracefully with proper HTTP status codes
- Use appropriate response formats (JSON, XML, etc.)
- Implement proper logging and monitoring
- Cache responses when appropriate
- Support API versioning

### ❌ DON'T:
- Put business logic in controllers
- Let domain objects leak through DTOs
- Ignore security concerns
- Skip input validation
- Return internal error details to users
- Block threads with synchronous operations
- Couple presentation to specific frameworks more than necessary

## Troubleshooting

### Problem: "Controller is getting complex"
**Solution**: Extract logic into separate service classes. Controllers should only handle HTTP concerns.

### Problem: "Too much duplication between different interfaces"
**Solution**: Create shared DTOs and mapper utilities.

### Problem: "Error handling is inconsistent"
**Solution**: Use global exception handlers and standardized error response formats.

### Problem: "API responses are slow"
**Solution**: Implement caching, async processing, and database query optimization.

## Summary

The presentation layer is your user's window into your application. It should:

- **Handle** all user interactions professionally
- **Transform** between external formats and internal commands
- **Validate** input before it reaches your core logic
- **Present** results in user-friendly formats
- **Remain** agnostic to business rules

Remember: The presentation layer is like a good translator - it makes communication smooth but never changes the meaning!