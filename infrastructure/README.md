# Infrastructure Module

**Version:** 1.0.0  
**Date:** September 29, 2025
**License:** BSD-3-Clause  
**Copyright:** © 2025 Michael Gardner, A Bit of Help, Inc.  
**Authors:** Michael Gardner  
**Status:** Released

## What is the Infrastructure Module?

The infrastructure module is where the "real world" meets your application. It's the layer that handles all the technical details - writing to files, reading from databases, calling external APIs, and handling console output. Think of it as the "plumbing" that connects your business logic to the outside world.

## Why Infrastructure Matters

Your beautiful domain logic is useless if it can't:
- Save data persistently
- Communicate with users
- Integrate with other systems
- Handle real-world I/O operations

The infrastructure layer makes all this possible while keeping technical concerns separate from business logic.

## The Adapter Pattern

This module implements the "Adapter" pattern from Hexagonal Architecture:

```
┌─────────────────┐         ┌──────────────────┐
│  Application    │  Port   │  Infrastructure  │
│    Layer       ├─────────>│     Adapter      │
│  (Interface)   │         │ (Implementation) │
└─────────────────┘         └──────────────────┘
```

**Key Concept**: The application defines what it needs (ports), and infrastructure provides how it's done (adapters).

## Current Adapters

### Output Adapters - Getting Data Out

#### 1. ConsoleOutputAdapter

Writes messages to the terminal with async buffering:

```kotlin
class ConsoleOutputAdapter : OutputPort {
    private val messageQueue = ConcurrentLinkedQueue<String>()
    private val outputThread = thread(start = true, isDaemon = true, name = "ConsoleOutputAdapter") {
        val writer = BufferedWriter(OutputStreamWriter(System.out), BUFFER_SIZE)
        while (!Thread.currentThread().isInterrupted) {
            processMessages(writer)
            Thread.sleep(FLUSH_INTERVAL_MS)
        }
    }
    
    override suspend fun send(message: String): Either<ApplicationError, Unit> {
        return try {
            messageQueue.offer(message)
            Unit.right()
        } catch (e: Exception) {
            ApplicationError.OutputError("Failed to queue message", e).left()
        }
    }
}
```

**Features**:
- Async output on separate thread
- 8KB buffer for efficiency
- Automatic flushing every 100ms
- Graceful shutdown handling

#### 2. FileOutputAdapter

Enhanced file writing with security validation and autoFlush support:

```kotlin
class FileOutputAdapter(
    private val filePath: String,
    private val autoFlush: Boolean = false,
) : OutputPort {
    
    override suspend fun send(message: String): Either<ApplicationError, Unit> {
        return withContext(Dispatchers.IO) {
            try {
                require(message.isNotBlank()) { "Message cannot be blank" }
                
                // Enhanced path validation with security checks
                val path = validateFilePath(filePath)
                
                // Auto-create parent directories
                path.parent?.let { parent ->
                    if (!parent.exists()) {
                        Files.createDirectories(parent)
                    }
                }
                
                // Write with optional immediate flush
                if (autoFlush) {
                    Files.writeString(
                        path,
                        message + System.lineSeparator(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND,
                        StandardOpenOption.SYNC // Force disk sync
                    )
                } else {
                    Files.writeString(
                        path,
                        message + System.lineSeparator(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                    )
                }
                
                Unit.right()
        } catch (e: IOException) {
            ApplicationError.OutputError(
                "Failed to write to file: ${e.message}", 
                e
            ).left()
        }
    }
}
```

**Features**:
- Creates parent directories automatically
- Supports append or overwrite modes
- Proper error handling with Either
- Uses coroutines for async I/O

#### 3. CompositeOutputAdapter

Combines multiple output adapters:

```kotlin
class CompositeOutputAdapter(
    private val adapters: List<OutputPort>
) : OutputPort {
    
    override suspend fun send(message: String): Either<ApplicationError, Unit> {
        val errors = mutableListOf<ApplicationError>()
        
        // Send to all adapters concurrently
        coroutineScope {
            adapters.map { adapter ->
                async {
                    adapter.send(message).fold(
                        { error -> errors.add(error) },
                        { /* Success */ }
                    )
                }
            }.awaitAll()
        }
        
        return if (errors.isEmpty()) {
            Unit.right()
        } else {
            ApplicationError.CompositeError(errors).left()
        }
    }
}
```

**Use Case**: Write to both console and file simultaneously

```kotlin
val composite = CompositeOutputAdapter(
    listOf(
        ConsoleOutputAdapter(),
        FileOutputAdapter("app.log")
    )
)
```

#### 4. BufferedFileOutputAdapter 🚀

High-performance file writing with NIO channels and background processing:

```kotlin
class BufferedFileOutputAdapter(
    private val filePath: String,
    private val bufferSize: Int = 8192,
    private val flushIntervalMs: Long = 1000,
    private val maxQueueSize: Int = 10000,
    private val autoFlush: Boolean = false,
) : OutputPort, AutoCloseable {
    
    private val messageQueue = Channel<String>(capacity = maxQueueSize)
    private val writerScope = CoroutineScope(
        SupervisorJob() + 
        Dispatchers.IO.limitedParallelism(1) +
        CoroutineName("BufferedFileWriter-$filePath")
    )
    
    override suspend fun send(message: String): Either<ApplicationError, Unit> {
        return try {
            require(message.isNotBlank()) { "Message cannot be blank" }
            
            // Non-blocking queue operation
            val result = messageQueue.trySend(message)
            if (result.isSuccess) {
                Unit.right()
            } else {
                // Queue full, try with timeout
                withTimeout(100) {
                    messageQueue.send(message)
                    Unit.right()
                }
            }
        } catch (e: Exception) {
            ApplicationError.OutputError(
                "Failed to queue message: ${e.message}"
            ).left()
        }
    }
    
    // Background writer using NIO AsynchronousFileChannel
    private suspend fun runWriter() {
        AsynchronousFileChannel.open(
            Path(filePath),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.APPEND
        ).use { fileChannel ->
            val buffer = StringBuilder(bufferSize)
            
            while (coroutineContext.isActive) {
                val message = withTimeoutOrNull(flushIntervalMs) {
                    messageQueue.receive()
                }
                
                if (message != null) {
                    buffer.append(message).append(System.lineSeparator())
                    
                    val shouldFlush = autoFlush || 
                        buffer.length >= bufferSize
                    
                    if (shouldFlush) {
                        flushBuffer(fileChannel, buffer)
                    }
                } else if (buffer.isNotEmpty()) {
                    // Timeout - flush existing content
                    flushBuffer(fileChannel, buffer)
                }
            }
        }
    }
    
    companion object {
        // Factory methods for common configurations
        fun highThroughput(filePath: String) = BufferedFileOutputAdapter(
            filePath = filePath,
            bufferSize = 65536, // 64KB
            flushIntervalMs = 5000, // 5 seconds
            maxQueueSize = 50000,
            autoFlush = false
        )
        
        fun lowLatency(filePath: String) = BufferedFileOutputAdapter(
            filePath = filePath,
            bufferSize = 1024, // 1KB
            flushIntervalMs = 100, // 100ms
            maxQueueSize = 1000,
            autoFlush = true
        )
    }
}
```

**Enhanced Features**:
- **NIO Channels**: True async I/O with AsynchronousFileChannel
- **Background Processing**: Dedicated writer coroutine with message queuing
- **Configurable Buffering**: Size, flush interval, and queue depth
- **Performance Metrics**: Track bytes written and queue utilization
- **AutoFlush Support**: Optional immediate persistence vs performance
- **Factory Methods**: Pre-configured for high-throughput or low-latency scenarios
- **Graceful Shutdown**: Proper resource cleanup with timeout
- Manual flush capability

## Common Patterns and Solutions

### Pattern: Repository Implementation

When you need to store and retrieve domain entities:

```kotlin
class FileBasedUserRepository(
    private val dataDir: String
) : UserRepositoryPort {
    
    override suspend fun save(user: User): Either<ApplicationError, User> {
        return try {
            withContext(Dispatchers.IO) {
                val file = File(dataDir, "user_${user.id.value}.json")
                file.parentFile.mkdirs()
                
                val json = Json.encodeToString(toDto(user))
                file.writeText(json)
                
                user.right()
            }
        } catch (e: Exception) {
            ApplicationError.PersistenceError(
                "Failed to save user", 
                e
            ).left()
        }
    }
    
    override suspend fun findById(
        id: UserId
    ): Either<ApplicationError, User?> {
        return try {
            withContext(Dispatchers.IO) {
                val file = File(dataDir, "user_${id.value}.json")
                if (!file.exists()) {
                    null.right()
                } else {
                    val json = file.readText()
                    val dto = Json.decodeFromString<UserDto>(json)
                    toDomain(dto).right()
                }
            }
        } catch (e: Exception) {
            ApplicationError.RetrievalError(
                "Failed to load user", 
                e
            ).left()
        }
    }
    
    // Convert between domain and DTOs
    private fun toDto(user: User): UserDto = UserDto(
        id = user.id.value,
        name = user.name.value,
        email = user.email.value
    )
    
    private fun toDomain(dto: UserDto): User = User(
        id = UserId(dto.id),
        name = PersonName.create(dto.name).getOrThrow(),
        email = Email.create(dto.email).getOrThrow()
    )
}
```

### Pattern: External API Client

```kotlin
class WeatherApiAdapter(
    private val apiKey: String,
    private val baseUrl: String
) : WeatherServicePort {
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
        }
    }
    
    override suspend fun getWeather(
        city: String
    ): Either<ApplicationError, Weather> {
        return try {
            val response: WeatherResponse = client.get("$baseUrl/weather") {
                parameter("q", city)
                parameter("apikey", apiKey)
            }
            
            Weather(
                temperature = Temperature.celsius(response.temp),
                description = response.description,
                humidity = Percentage(response.humidity)
            ).right()
            
        } catch (e: ClientRequestException) {
            ApplicationError.ExternalServiceError(
                "Weather API",
                "Invalid request: ${e.message}"
            ).left()
        } catch (e: Exception) {
            ApplicationError.ExternalServiceError(
                "Weather API",
                "Service unavailable"
            ).left()
        }
    }
}
```

### Pattern: Event Publisher

```kotlin
class KafkaEventPublisher(
    private val producer: KafkaProducer<String, String>
) : EventPublisherPort {
    
    override suspend fun publish(
        event: DomainEvent
    ): Either<ApplicationError, Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val record = ProducerRecord(
                    "domain-events",
                    event.aggregateId,
                    Json.encodeToString(event)
                )
                
                producer.send(record).get(5, TimeUnit.SECONDS)
                Unit.right()
            }
        } catch (e: TimeoutException) {
            ApplicationError.ExternalServiceError(
                "Kafka",
                "Publish timeout"
            ).left()
        } catch (e: Exception) {
            ApplicationError.ExternalServiceError(
                "Kafka",
                "Failed to publish event"
            ).left()
        }
    }
}
```

## Testing Infrastructure Code

### Unit Tests with Test Doubles

```kotlin
class FileOutputAdapterTest : DescribeSpec({
    
    describe("FileOutputAdapter") {
        val testDir = createTempDir()
        val testFile = File(testDir, "test.txt")
        val adapter = FileOutputAdapter(testFile.absolutePath)
        
        afterSpec {
            testDir.deleteRecursively()
        }
        
        it("should write message to file") {
            val result = runBlocking {
                adapter.send("Hello, World!")
            }
            
            result.shouldBeRight()
            testFile.readText() shouldBe "Hello, World!\n"
        }
        
        it("should handle I/O errors gracefully") {
            val readOnlyDir = File("/dev/null")
            val badAdapter = FileOutputAdapter(
                "$readOnlyDir/impossible.txt"
            )
            
            val result = runBlocking {
                badAdapter.send("This will fail")
            }
            
            result.shouldBeLeft()
            val error = result.leftOrNull() as ApplicationError.OutputError
            error.message shouldContain "Failed to write"
        }
    }
})
```

### Integration Tests

```kotlin
class DatabaseIntegrationTest : DescribeSpec({
    
    val testDb = TestDatabase.create()
    val repository = PostgresUserRepository(testDb.dataSource)
    
    beforeSpec {
        testDb.migrate()
    }
    
    afterSpec {
        testDb.close()
    }
    
    describe("User persistence") {
        it("should save and retrieve user") {
            val user = User(
                id = UserId.generate(),
                name = PersonName.create("Alice").getOrThrow(),
                email = Email.create("alice@example.com").getOrThrow()
            )
            
            runBlocking {
                repository.save(user).shouldBeRight()
                
                val loaded = repository.findById(user.id)
                loaded.shouldBeRight()
                loaded.value shouldBe user
            }
        }
    }
})
```

### Contract Tests

```kotlin
abstract class OutputPortContract(
    private val createAdapter: () -> OutputPort
) : DescribeSpec({
    
    describe("OutputPort contract") {
        val adapter = createAdapter()
        
        it("should successfully send valid message") {
            val result = runBlocking {
                adapter.send("Valid message")
            }
            result.shouldBeRight()
        }
        
        it("should handle empty message") {
            val result = runBlocking {
                adapter.send("")
            }
            // Depends on implementation - some may accept empty
        }
    }
})

// Run contract test for each implementation
class ConsoleOutputContractTest : OutputPortContract(
    { ConsoleOutputAdapter() }
)

class FileOutputContractTest : OutputPortContract(
    { FileOutputAdapter(createTempFile().absolutePath) }
)
```

## Common Pitfalls and Solutions

### Pitfall 1: Blocking I/O in Coroutines

```kotlin
// BAD: Blocks the coroutine thread
override suspend fun readFile(path: String): String {
    return File(path).readText() // Blocking!
}

// GOOD: Uses proper dispatcher
override suspend fun readFile(path: String): String {
    return withContext(Dispatchers.IO) {
        File(path).readText()
    }
}
```

### Pitfall 2: Not Handling Resources Properly

```kotlin
// BAD: Resource leak
fun writeToFile(data: String) {
    val writer = FileWriter("output.txt")
    writer.write(data)
    // Forgot to close!
}

// GOOD: Use use() for auto-closing
fun writeToFile(data: String) {
    FileWriter("output.txt").use { writer ->
        writer.write(data)
    } // Automatically closed
}

// BETTER: Use Kotlin's file extensions
fun writeToFile(data: String) {
    File("output.txt").writeText(data)
}
```

### Pitfall 3: Exposing Infrastructure Details

```kotlin
// BAD: Leaks SQLException to application
override fun save(user: User): User {
    try {
        // SQL operations
    } catch (e: SQLException) {
        throw e // Application shouldn't know about SQL!
    }
}

// GOOD: Wrap in application error
override fun save(user: User): Either<ApplicationError, User> {
    return try {
        // SQL operations
        user.right()
    } catch (e: SQLException) {
        ApplicationError.PersistenceError(
            "Failed to save user",
            e
        ).left()
    }
}
```

## Adding New Adapters

### Step 1: Define the Port (in Application layer)

```kotlin
// application/src/main/kotlin/.../port/output/CachePort.kt
interface CachePort {
    suspend fun get(key: String): Either<ApplicationError, String?>
    suspend fun set(key: String, value: String, ttl: Duration = Duration.INFINITE): Either<ApplicationError, Unit>
    suspend fun delete(key: String): Either<ApplicationError, Unit>
}
```

### Step 2: Implement the Adapter

```kotlin
// infrastructure/src/main/kotlin/.../adapter/output/RedisCacheAdapter.kt
class RedisCacheAdapter(
    private val redisClient: RedisClient
) : CachePort {
    
    override suspend fun get(key: String): Either<ApplicationError, String?> {
        return try {
            withContext(Dispatchers.IO) {
                redisClient.get(key).right()
            }
        } catch (e: Exception) {
            ApplicationError.ExternalServiceError(
                "Redis",
                "Failed to get value"
            ).left()
        }
    }
    
    override suspend fun set(
        key: String, 
        value: String, 
        ttl: Duration
    ): Either<ApplicationError, Unit> {
        return try {
            withContext(Dispatchers.IO) {
                if (ttl.isInfinite()) {
                    redisClient.set(key, value)
                } else {
                    redisClient.setex(key, ttl.inWholeSeconds.toInt(), value)
                }
                Unit.right()
            }
        } catch (e: Exception) {
            ApplicationError.ExternalServiceError(
                "Redis",
                "Failed to set value"
            ).left()
        }
    }
}
```

### Step 3: Write Tests

```kotlin
class RedisCacheAdapterTest : DescribeSpec({
    
    val testRedis = TestRedisContainer()
    lateinit var adapter: CachePort
    
    beforeSpec {
        testRedis.start()
        adapter = RedisCacheAdapter(testRedis.client)
    }
    
    afterSpec {
        testRedis.stop()
    }
    
    describe("Redis cache operations") {
        it("should store and retrieve values") {
            runBlocking {
                adapter.set("key1", "value1").shouldBeRight()
                
                val result = adapter.get("key1")
                result.shouldBeRight()
                result.value shouldBe "value1"
            }
        }
        
        it("should expire values after TTL") {
            runBlocking {
                adapter.set("key2", "value2", 1.seconds).shouldBeRight()
                
                delay(1500)
                
                val result = adapter.get("key2")
                result.shouldBeRight()
                result.value shouldBe null
            }
        }
    }
})
```

### Step 4: Wire in Bootstrap

```kotlin
// bootstrap/src/main/kotlin/.../CompositionRoot.kt
object CompositionRoot {
    fun createCachePort(config: AppConfig): CachePort {
        return when (config.cacheType) {
            CacheType.REDIS -> RedisCacheAdapter(
                RedisClient.create(config.redisUrl)
            )
            CacheType.IN_MEMORY -> InMemoryCacheAdapter()
            CacheType.NONE -> NoOpCacheAdapter()
        }
    }
}
```

## Performance Considerations

### Connection Pooling

```kotlin
class DatabaseConnectionPool(
    private val config: DatabaseConfig
) {
    private val dataSource = HikariDataSource().apply {
        jdbcUrl = config.url
        username = config.username
        password = config.password
        maximumPoolSize = 10
        minimumIdle = 2
        connectionTimeout = 5000
    }
    
    suspend fun <T> withConnection(
        block: suspend (Connection) -> T
    ): T {
        return withContext(Dispatchers.IO) {
            dataSource.connection.use { conn ->
                block(conn)
            }
        }
    }
}
```

### Caching Strategy

```kotlin
class CachedUserRepository(
    private val delegate: UserRepositoryPort,
    private val cache: CachePort
) : UserRepositoryPort {
    
    override suspend fun findById(
        id: UserId
    ): Either<ApplicationError, User?> {
        // Try cache first
        val cacheKey = "user:${id.value}"
        
        return cache.get(cacheKey).flatMap { cached ->
            if (cached != null) {
                // Parse from cache
                Json.decodeFromString<UserDto>(cached)
                    .let { toDomain(it) }
                    .let { it.right() }
            } else {
                // Load from database
                delegate.findById(id).onRight { user ->
                    // Cache for next time
                    user?.let {
                        val json = Json.encodeToString(toDto(it))
                        cache.set(cacheKey, json, 5.minutes)
                    }
                }
            }
        }
    }
}
```

## Module Structure

```
infrastructure/
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── com/abitofhelp/hybrid/infrastructure/
│   │           ├── adapter/
│   │           │   ├── output/          # Output port implementations
│   │           │   │   ├── ConsoleOutputAdapter.kt
│   │           │   │   ├── FileOutputAdapter.kt
│   │           │   │   └── CompositeOutputAdapter.kt
│   │           │   ├── input/           # Input adapters (REST, etc.)
│   │           │   └── repository/      # Repository implementations
│   │           ├── config/              # Infrastructure configuration
│   │           └── util/                # Infrastructure utilities
│   └── test/
│       └── kotlin/
│           └── com/abitofhelp/hybrid/infrastructure/
│               ├── adapter/             # Adapter tests
│               ├── integration/         # Integration tests
│               └── contract/            # Contract tests
└── build.gradle.kts
```

## Summary

The infrastructure module is where theory meets practice. It:

- **Implements** the ports defined by the application
- **Handles** all technical concerns
- **Isolates** your business logic from external changes
- **Provides** reliable, tested adapters
- **Enables** easy swapping of implementations

Remember: Infrastructure code is allowed to be "messy" - it deals with the real world. The key is keeping this messiness contained and hidden behind clean interfaces!