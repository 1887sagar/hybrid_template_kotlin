<!--
  Kotlin Hybrid Architecture Template
  Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
  SPDX-License-Identifier: BSD-3-Clause
  See LICENSE file in the project root.
-->

# Infrastructure Module

## Overview

The Infrastructure module contains all the **technical implementations** and **external system integrations**. It implements the port interfaces defined in the Application layer and provides concrete implementations for databases, external APIs, file systems, messaging systems, and other technical concerns.

## Core Principles

- **Depends on Domain and Application modules**
- **Implements port interfaces** defined in Application layer
- **Contains all framework-specific code**
- **Handles external system integration**
- **No business logic** - only technical implementation
- **Converts exceptions to Either** at boundaries

## Structure

```
infrastructure/
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── com/
│   │           └── abitofhelp/
│   │               └── hybrid/
│   │                   └── infrastructure/
│   │                       ├── adapter/           # Port implementations
│   │                       │   ├── persistence/  # Database adapters
│   │                       │   │   ├── jpa/      # JPA repositories
│   │                       │   │   ├── mongodb/  # MongoDB repositories
│   │                       │   │   └── redis/    # Redis cache
│   │                       │   ├── external/     # External service adapters
│   │                       │   │   ├── payment/  # Payment gateways
│   │                       │   │   ├── email/    # Email services
│   │                       │   │   └── sms/      # SMS providers
│   │                       │   ├── messaging/    # Message queue adapters
│   │                       │   │   ├── kafka/    # Kafka producer/consumer
│   │                       │   │   └── rabbitmq/ # RabbitMQ implementation
│   │                       │   └── filesystem/   # File system operations
│   │                       ├── config/           # Infrastructure configuration
│   │                       ├── error/            # Infrastructure error mapping
│   │                       └── util/             # Technical utilities
│   └── test/
│       └── kotlin/
│           └── com/
│               └── abitofhelp/
│                   └── hybrid/
│                       └── infrastructure/       # Infrastructure tests
└── build.gradle.kts                               # Module build configuration
```

## Key Components

### Database Adapters

#### JPA Repository Implementation
```kotlin
@Repository
class JpaCustomerRepository(
    private val jpaRepository: CustomerJpaRepository,
    private val mapper: CustomerEntityMapper
) : CustomerRepository {
    
    override suspend fun findById(id: CustomerId): Either<DomainError, Customer?> {
        return Either.catch {
            jpaRepository.findById(id.value)
                .map { mapper.toDomain(it) }
                .orElse(null)
        }.mapLeft { throwable ->
            DomainError.NotFound("Customer", id.value)
        }
    }
    
    override suspend fun save(customer: Customer): Either<DomainError, Customer> {
        return Either.catch {
            val entity = mapper.toEntity(customer)
            val saved = jpaRepository.save(entity)
            mapper.toDomain(saved)
        }.mapLeft { throwable ->
            InfrastructureError.PersistenceError(throwable).toDomainError()
        }
    }
}
```

#### MongoDB Repository Implementation
```kotlin
@Component
class MongoOrderRepository(
    private val mongoTemplate: MongoTemplate
) : OrderRepository {
    
    override suspend fun findByCustomerId(
        customerId: CustomerId
    ): Either<DomainError, List<Order>> {
        return Either.catch {
            val query = Query.query(Criteria.where("customerId").`is`(customerId.value))
            mongoTemplate.find(query, OrderDocument::class.java)
                .map { it.toDomain() }
        }.mapLeft { throwable ->
            InfrastructureError.DatabaseError("MongoDB", throwable).toDomainError()
        }
    }
}
```

### External Service Adapters

#### Payment Gateway Adapter
```kotlin
@Component
class StripePaymentAdapter(
    private val stripeClient: StripeClient,
    private val config: PaymentConfig
) : PaymentServicePort {
    
    override suspend fun processPayment(
        amount: Money,
        card: CreditCard
    ): Either<ApplicationError, PaymentResult> {
        return Either.catch {
            val charge = stripeClient.charges.create(
                ChargeCreateParams.builder()
                    .setAmount(amount.toStripeAmount())
                    .setCurrency(amount.currency.code)
                    .setSource(card.token)
                    .build()
            )
            PaymentResult(
                id = PaymentId(charge.id),
                status = PaymentStatus.fromStripe(charge.status),
                amount = amount
            )
        }.mapLeft { throwable ->
            when (throwable) {
                is StripeException -> ApplicationError.ExternalServiceError(
                    "Stripe",
                    throwable.message ?: "Payment failed"
                )
                else -> ApplicationError.ExternalServiceError(
                    "Stripe",
                    "Unexpected error: ${throwable.message}"
                )
            }
        }
    }
}
```

#### Email Service Adapter
```kotlin
@Component
class SendGridEmailAdapter(
    private val sendGrid: SendGrid,
    private val config: EmailConfig
) : NotificationPort {
    
    override suspend fun sendEmail(
        to: Email,
        subject: String,
        body: String
    ): Either<ApplicationError, Unit> {
        return Either.catch {
            val from = com.sendgrid.helpers.mail.objects.Email(config.fromEmail)
            val toEmail = com.sendgrid.helpers.mail.objects.Email(to.value)
            val content = Content("text/html", body)
            val mail = Mail(from, subject, toEmail, content)
            
            val request = Request().apply {
                method = Method.POST
                endpoint = "mail/send"
                this.body = mail.build()
            }
            
            val response = sendGrid.api(request)
            if (response.statusCode !in 200..299) {
                throw EmailSendException("Failed with status: ${response.statusCode}")
            }
        }.mapLeft { throwable ->
            ApplicationError.ExternalServiceError("SendGrid", throwable.message ?: "Send failed")
        }
    }
}
```

### Message Queue Adapters

#### Kafka Event Publisher
```kotlin
@Component
class KafkaEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) : EventPublisherPort {
    
    override suspend fun publish(event: DomainEvent): Either<ApplicationError, Unit> {
        return Either.catch {
            val topic = resolveTopicName(event)
            val key = event.aggregateId
            val payload = objectMapper.writeValueAsString(event)
            
            kafkaTemplate.send(topic, key, payload).get()
            Unit
        }.mapLeft { throwable ->
            ApplicationError.ExternalServiceError("Kafka", "Failed to publish event: ${throwable.message}")
        }
    }
    
    private fun resolveTopicName(event: DomainEvent): String = when (event) {
        is OrderCreatedEvent -> "order.created"
        is PaymentProcessedEvent -> "payment.processed"
        else -> "domain.events"
    }
}
```

### File System Adapter
```kotlin
@Component
class LocalFileSystemAdapter : FileSystemPort {
    
    override suspend fun readFile(path: Path): Either<ApplicationError, String> {
        return Either.catch {
            withContext(Dispatchers.IO) {
                path.readText(Charsets.UTF_8)
            }
        }.mapLeft { throwable ->
            when (throwable) {
                is NoSuchFileException -> ApplicationError.NotFound("File", path.toString())
                is AccessDeniedException -> ApplicationError.UnauthorizedError(path.toString())
                else -> ApplicationError.RepositoryError("FileSystem", throwable.message ?: "Read failed")
            }
        }
    }
    
    override suspend fun writeFile(
        path: Path,
        content: String
    ): Either<ApplicationError, Unit> {
        return Either.catch {
            withContext(Dispatchers.IO) {
                path.parent?.createDirectories()
                path.writeText(content, Charsets.UTF_8)
            }
        }.mapLeft { throwable ->
            ApplicationError.RepositoryError("FileSystem", "Write failed: ${throwable.message}")
        }
    }
}
```

## Configuration

Infrastructure components are configured through:

```kotlin
@Configuration
@ConfigurationProperties(prefix = "infrastructure")
data class InfrastructureConfig(
    val database: DatabaseConfig,
    val external: ExternalServicesConfig,
    val messaging: MessagingConfig
)

data class DatabaseConfig(
    val connectionPool: PoolConfig,
    val timeout: Duration = Duration.ofSeconds(30)
)

data class ExternalServicesConfig(
    val payment: PaymentConfig,
    val email: EmailConfig,
    val retryPolicy: RetryPolicy
)

data class RetryPolicy(
    val maxAttempts: Int = 3,
    val backoffMultiplier: Double = 2.0,
    val initialDelay: Duration = Duration.ofMillis(100)
)
```

## Error Handling

All infrastructure errors are properly mapped:

```kotlin
sealed class InfrastructureError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {
    
    data class DatabaseError(
        val operation: String,
        override val cause: Throwable
    ) : InfrastructureError("Database error during $operation", cause)
    
    data class ExternalServiceError(
        val service: String,
        override val cause: Throwable
    ) : InfrastructureError("External service error: $service", cause)
    
    data class MessageQueueError(
        val queue: String,
        override val cause: Throwable
    ) : InfrastructureError("Message queue error: $queue", cause)
    
    fun toDomainError(): DomainError = when (this) {
        is DatabaseError -> DomainError.Conflict("Database operation failed: $operation")
        is ExternalServiceError -> DomainError.Conflict("External service unavailable: $service")
        is MessageQueueError -> DomainError.Conflict("Message queue error: $queue")
    }
}
```

## Testing Strategy

### Adapter Tests
Test each adapter with mocked external dependencies:
```kotlin
class StripePaymentAdapterTest {
    private val stripeClient = mockk<StripeClient>()
    private val adapter = StripePaymentAdapter(stripeClient, testConfig)
    
    @Test
    fun `should process payment successfully`() {
        // Given
        every { stripeClient.charges.create(any()) } returns mockCharge
        
        // When
        val result = runBlocking { adapter.processPayment(amount, card) }
        
        // Then
        result.shouldBeRight()
    }
}
```

### Integration Tests
Test with real external systems using testcontainers:
```kotlin
@Testcontainers
class PostgresRepositoryIntegrationTest {
    
    @Container
    val postgres = PostgreSQLContainer<Nothing>("postgres:15").apply {
        withDatabaseName("test")
        withUsername("test")
        withPassword("test")
    }
    
    @Test
    fun `should persist and retrieve customer`() {
        // Test with real database
    }
}
```

## Best Practices

1. **Keep Adapters Thin**: Minimal logic, just translation between ports and external systems
2. **Handle All Exceptions**: Convert to Either at boundaries, never let exceptions escape
3. **Use Configuration**: Make adapters configurable, avoid hardcoded values
4. **Add Observability**: Include logging, metrics, and tracing
5. **Manage Resources**: Use try-with-resources or Kotlin's use function
6. **Circuit Breakers**: Add resilience patterns for external services
7. **Caching**: Implement caching where appropriate for performance
8. **Idempotency**: Ensure operations can be safely retried

## Output Adapters

Located in `adapter/output/`:

- **ConsoleOutputAdapter** - Writes messages to stdout
- **FileOutputAdapter** - Writes messages to files asynchronously  
- **BufferedFileOutputAdapter** - High-performance buffered file writes
- **CompositeOutputAdapter** - Combines multiple output adapters

### Buffered vs Unbuffered File Output

| Feature | FileOutputAdapter | BufferedFileOutputAdapter |
|---------|------------------|---------------------------|
| **Write Latency** | Higher (immediate write) | Lower (queued) |
| **Throughput** | Lower | Higher (batched writes) |
| **Memory Usage** | Minimal | Configurable buffer |
| **Data Durability** | Immediate | Delayed until flush |
| **Use Case** | Critical logs, audit trails | High-volume logging |
| **Complexity** | Simple | More complex |

### BufferedFileOutputAdapter Configuration

```kotlin
// Default configuration
val adapter = BufferedFileOutputAdapter(
    filePath = "/var/log/app.log",
    bufferSize = 8192,           // 8KB buffer
    flushIntervalMs = 1000,      // Flush every second
    maxQueueSize = 10000         // Max pending messages
)

// High-throughput configuration
val highThroughput = BufferedFileOutputAdapter.highThroughput("/var/log/app.log")
// Uses: 64KB buffer, 5s flush interval, 50K queue size

// Low-latency configuration  
val lowLatency = BufferedFileOutputAdapter.lowLatency("/var/log/app.log")
// Uses: 1KB buffer, 100ms flush interval, 1K queue size
```

### Performance Characteristics

**BufferedFileOutputAdapter Benefits:**
- **Reduced I/O Operations**: Batches multiple writes into single disk operation
- **Non-blocking Sends**: Messages queued immediately, written asynchronously
- **NIO Channels**: Uses Java NIO for true async file operations
- **Configurable Trade-offs**: Balance between latency and throughput

**When to Use Buffered Output:**
- High-volume application logging
- Metrics and telemetry data
- Non-critical debugging information
- Performance-sensitive applications

**When NOT to Use Buffered Output:**
- Financial transaction logs
- Security audit trails  
- Critical error logs
- Applications prone to crashes (may lose buffered data)

### Implementation Details

The `BufferedFileOutputAdapter` uses:
- Coroutine channels for lock-free message queuing
- Single writer coroutine to prevent file corruption
- Configurable buffer with automatic flushing
- Graceful shutdown to ensure all data is written

## Common Patterns

### Retry with Exponential Backoff
```kotlin
suspend fun <T> retryWithBackoff(
    policy: RetryPolicy,
    block: suspend () -> T
): Either<ApplicationError, T> {
    var lastError: Throwable? = null
    var delay = policy.initialDelay.toMillis()
    
    repeat(policy.maxAttempts) { attempt ->
        try {
            return Either.Right(block())
        } catch (e: Exception) {
            lastError = e
            if (attempt < policy.maxAttempts - 1) {
                delay(delay)
                delay = (delay * policy.backoffMultiplier).toLong()
            }
        }
    }
    
    return Either.Left(
        ApplicationError.ExternalServiceError(
            "Retry",
            "Failed after ${policy.maxAttempts} attempts: ${lastError?.message}"
        )
    )
}
```

### Circuit Breaker Pattern
```kotlin
class CircuitBreaker(
    private val failureThreshold: Int = 5,
    private val timeout: Duration = Duration.ofMinutes(1)
) {
    private var failureCount = 0
    private var lastFailureTime: Instant? = null
    private var state = State.CLOSED
    
    suspend fun <T> execute(block: suspend () -> T): T {
        when (state) {
            State.OPEN -> {
                if (Duration.between(lastFailureTime, Instant.now()) > timeout) {
                    state = State.HALF_OPEN
                } else {
                    throw CircuitBreakerOpenException()
                }
            }
            State.HALF_OPEN, State.CLOSED -> {
                try {
                    val result = block()
                    reset()
                    return result
                } catch (e: Exception) {
                    recordFailure()
                    throw e
                }
            }
        }
    }
}
```