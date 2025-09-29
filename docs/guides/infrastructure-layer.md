# Infrastructure Layer Documentation

**Version:** 1.0.0
**Date:** September 29, 2025
**SPDX-License-Identifier:** BSD-3-Clause
**License File:** See LICENSE file in the project root.
**Copyright:** © 2025 Michael Gardner, A Bit of Help, Inc.
**Authors:** Michael Gardner
**Status:** Released

## Overview

The infrastructure layer contains all technical implementation details and external system integrations. It implements the ports (interfaces) defined in the domain and application layers, providing concrete implementations for databases, file systems, external APIs, and other technical concerns.

## Key Responsibilities

1. **Implement Port Interfaces** - Provide concrete implementations of domain/application ports
2. **Handle External Systems** - Integrate with databases, APIs, file systems, etc.
3. **Manage Technical Concerns** - Logging, monitoring, caching, etc.
4. **Transform Data** - Convert between external formats and domain objects

## Components

### Adapters

Adapters implement port interfaces, bridging the gap between business logic and technical infrastructure.

#### ConsoleOutputAdapter

```kotlin
class ConsoleOutputAdapter : OutputPort {
    override suspend fun send(message: String): Either<ApplicationError, Unit> {
        return try {
            require(message.isNotBlank()) { "Message cannot be blank" }
            println(message)
            Unit.right()
        } catch (e: IllegalArgumentException) {
            ApplicationError.OutputError(
                "Invalid message: ${e.message ?: "Unknown validation error"}",
            ).left()
        } catch (e: SecurityException) {
            ApplicationError.OutputError(
                "Security error writing to console: ${e.message ?: "Permission denied"}",
            ).left()
        } catch (e: Exception) {
            ApplicationError.OutputError(
                "Unexpected error writing to console: ${e.javaClass.simpleName} - ${e.message}",
            ).left()
        }
    }
}
```

**Key Features**:
- **Implements OutputPort** - Fulfills application layer contract
- **Error Handling** - Catches and transforms exceptions to application errors
- **Specific Exception Types** - Different handling for different failure modes
- **Validation** - Checks preconditions before operation

### Services

Infrastructure services provide technical implementations that can be swapped out based on requirements.

#### DefaultGreetingService

```kotlin
class DefaultGreetingService : GreetingService {
    override suspend fun createGreeting(name: PersonName): Either<DomainError, String> {
        return try {
            val format = GreetingPolicy.determineGreetingFormat(name)
            
            val greeting = when (format) {
                GreetingFormat.DEFAULT -> "Hello World from ${name.value}!"
                GreetingFormat.FRIENDLY -> "Hey there, ${name.value}! Welcome!"
                GreetingFormat.FORMAL -> "Greetings, ${name.value}. Welcome to the application."
            }
            
            if (greeting.length > GreetingPolicy.MAX_GREETING_LENGTH) {
                DomainError.BusinessRuleViolation(
                    rule = "MaxGreetingLength",
                    reason = "Greeting exceeds maximum length",
                ).left()
            } else {
                greeting.right()
            }
        } catch (e: Exception) {
            DomainError.BusinessRuleViolation(
                rule = "GreetingCreation",
                reason = "Failed to create greeting: ${e.message}",
            ).left()
        }
    }
}
```

**Design Rationale**:
- **Infrastructure, Not Domain** - Even though it's pure logic now, it represents a replaceable component
- **Future Flexibility** - Could be swapped for:
  - `DatabaseGreetingService` - Loads templates from database
  - `I18nGreetingService` - Loads from translation files
  - `ApiGreetingService` - Fetches from external service
- **Template Storage** - The greeting templates are "data" that could come from anywhere

## Common Adapter Patterns

### Database Adapter Example

```kotlin
class DatabaseGreetingRepository : GreetingRepositoryPort {
    private val dataSource: DataSource // Injected
    
    override suspend fun save(greeting: Greeting): Either<ApplicationError, Unit> {
        return try {
            withContext(Dispatchers.IO) {
                dataSource.connection.use { conn ->
                    conn.prepareStatement(
                        "INSERT INTO greetings (id, message, recipient) VALUES (?, ?, ?)"
                    ).use { stmt ->
                        stmt.setString(1, greeting.id.value)
                        stmt.setString(2, greeting.message)
                        stmt.setString(3, greeting.recipient.value)
                        stmt.executeUpdate()
                    }
                }
            }
            Unit.right()
        } catch (e: SQLException) {
            ApplicationError.OutputError(
                "Failed to save greeting: ${e.message}"
            ).left()
        }
    }
}
```

### File System Adapter Example

```kotlin
class FileOutputAdapter(
    private val basePath: Path
) : OutputPort {
    override suspend fun send(message: String): Either<ApplicationError, Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val file = basePath.resolve("output-${System.currentTimeMillis()}.txt")
                file.writeText(message)
            }
            Unit.right()
        } catch (e: IOException) {
            ApplicationError.OutputError(
                "Failed to write file: ${e.message}"
            ).left()
        }
    }
}
```

### HTTP Client Adapter Example

```kotlin
class ApiNotificationAdapter(
    private val httpClient: HttpClient
) : NotificationPort {
    override suspend fun notify(
        userId: UserId,
        message: String
    ): Either<ApplicationError, Unit> {
        return try {
            val response = httpClient.post("https://api.example.com/notify") {
                contentType(ContentType.Application.Json)
                setBody(NotificationRequest(userId.value, message))
            }
            
            if (response.status.isSuccess()) {
                Unit.right()
            } else {
                ApplicationError.OutputError(
                    "API returned ${response.status}: ${response.bodyAsText()}"
                ).left()
            }
        } catch (e: Exception) {
            ApplicationError.OutputError(
                "Failed to call notification API: ${e.message}"
            ).left()
        }
    }
}
```

## Error Handling Strategies

### 1. Exception to Either Transformation

Always catch exceptions at infrastructure boundaries and transform them to Either:

```kotlin
return try {
    // Perform operation
    someResult.right()
} catch (e: SpecificException) {
    // Handle specific exception
    ApplicationError.OutputError("Specific error: ${e.message}").left()
} catch (e: Exception) {
    // Catch-all for unexpected exceptions
    ApplicationError.OutputError("Unexpected error: ${e.message}").left()
}
```

### 2. Specific Exception Handling

Handle different exception types appropriately:

```kotlin
catch (e: IOException) {
    ApplicationError.OutputError("I/O error: ${e.message}").left()
} catch (e: SecurityException) {
    ApplicationError.OutputError("Permission denied: ${e.message}").left()
} catch (e: IllegalArgumentException) {
    ApplicationError.OutputError("Invalid input: ${e.message}").left()
}
```

### 3. Resource Management

Use Kotlin's `use` extension for automatic resource cleanup:

```kotlin
connection.use { conn ->
    conn.prepareStatement(sql).use { stmt ->
        stmt.executeQuery().use { rs ->
            // Process results
        }
    }
}
```

## Best Practices

### 1. Keep Adapters Thin

Adapters should only handle:
- Technical integration
- Error transformation
- Data format conversion

They should NOT contain business logic.

### 2. Use Dependency Injection

Inject technical dependencies through constructors:

```kotlin
class EmailAdapter(
    private val smtpClient: SmtpClient,
    private val templateEngine: TemplateEngine,
    private val config: EmailConfig
) : NotificationPort
```

### 3. Handle Async Operations Properly

Use appropriate coroutine contexts:

```kotlin
override suspend fun performIoOperation(): Either<ApplicationError, Result> {
    return withContext(Dispatchers.IO) {
        // I/O operations here
    }
}
```

### 4. Configuration Management

Keep configuration separate from implementation:

```kotlin
data class DatabaseConfig(
    val url: String,
    val username: String,
    val password: String,
    val poolSize: Int = 10
)

class DatabaseAdapter(
    private val config: DatabaseConfig
) : RepositoryPort
```

## Testing Infrastructure

### Unit Testing Adapters

Test adapters with mocked dependencies:

```kotlin
class ConsoleOutputAdapterTest {
    private val adapter = ConsoleOutputAdapter()
    
    @Test
    fun `should successfully send valid message`() = runTest {
        val result = adapter.send("Hello, World!")
        
        result.shouldBeRight()
    }
    
    @Test
    fun `should reject blank message`() = runTest {
        val result = adapter.send("   ")
        
        result.shouldBeLeft()
        val error = result.leftOrNull() as ApplicationError.OutputError
        error.message shouldContain "blank"
    }
}
```

### Integration Testing

Test with real external systems (in test environment):

```kotlin
@IntegrationTest
class DatabaseAdapterIntegrationTest {
    private val testDb = TestDatabase()
    private val adapter = DatabaseAdapter(testDb.config)
    
    @BeforeEach
    fun setup() {
        testDb.clear()
    }
    
    @Test
    fun `should persist and retrieve data`() = runTest {
        // Test with real database
    }
}
```

## Common Infrastructure Patterns

### Caching

```kotlin
class CachedGreetingService(
    private val delegate: GreetingService,
    private val cache: Cache<PersonName, String>
) : GreetingService {
    override suspend fun createGreeting(name: PersonName): Either<DomainError, String> {
        return cache.get(name)?.right() 
            ?: delegate.createGreeting(name).onRight { greeting ->
                cache.put(name, greeting)
            }
    }
}
```

### Retry Logic

```kotlin
suspend fun <T> retryWithBackoff(
    times: Int = 3,
    initialDelay: Long = 100,
    block: suspend () -> Either<ApplicationError, T>
): Either<ApplicationError, T> {
    repeat(times - 1) { attempt ->
        val result = block()
        if (result.isRight()) return result
        
        delay(initialDelay * (attempt + 1))
    }
    return block() // Last attempt
}
```

### Circuit Breaker

```kotlin
class CircuitBreaker<T>(
    private val failureThreshold: Int = 5,
    private val resetTimeout: Duration = 60.seconds
) {
    private var failures = 0
    private var lastFailureTime: Instant? = null
    private var state = State.CLOSED
    
    suspend fun call(block: suspend () -> T): Either<ApplicationError, T> {
        return when (state) {
            State.OPEN -> {
                if (shouldReset()) {
                    state = State.HALF_OPEN
                    tryCall(block)
                } else {
                    ApplicationError.OutputError("Circuit breaker is open").left()
                }
            }
            State.CLOSED, State.HALF_OPEN -> tryCall(block)
        }
    }
}
```

## Guidelines for Adding New Infrastructure

1. **Identify the Port** - What interface are you implementing?
2. **Choose Technology** - What technical solution fits best?
3. **Handle Errors** - Transform all exceptions to Either
4. **Manage Resources** - Ensure proper cleanup
5. **Add Configuration** - Make it configurable
6. **Write Tests** - Both unit and integration tests
7. **Document Decisions** - Why this technology?

## Summary

The infrastructure layer is where technical reality meets business abstraction. It:
- **Implements** the contracts defined by inner layers
- **Handles** all technical complexities
- **Transforms** exceptions into functional errors
- **Manages** external resources and integrations
- **Remains** replaceable without affecting business logic

By keeping infrastructure concerns isolated in this layer, you ensure that business logic remains pure and technical decisions remain flexible.