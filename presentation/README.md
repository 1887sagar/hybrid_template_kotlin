<!--
  Kotlin Hybrid Architecture Template
  Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
  SPDX-License-Identifier: BSD-3-Clause
  See LICENSE file in the project root.
-->

# Presentation Module

## Overview

The Presentation module is the **outermost layer** that handles all user interactions. It provides REST APIs, GraphQL endpoints, CLI interfaces, and web UI components. This layer formats outputs, handles user input, and coordinates requests by calling the appropriate use cases from the Application layer.

## Core Principles

- **Depends ONLY on Application module** (never on Domain or Infrastructure)
- **No business logic** - only presentation concerns
- **Handles input validation and transformation**
- **Presents results in appropriate formats**
- **Framework-specific code lives here**

## Structure

```
presentation/
├── src/
│   ├── main/
│   │   └── kotlin/
│   │       └── com/
│   │           └── abitofhelp/
│   │               └── hybrid/
│   │                   └── presentation/
│   │                       ├── api/            # REST/GraphQL endpoints
│   │                       │   ├── rest/       # REST controllers
│   │                       │   │   ├── v1/     # API version 1
│   │                       │   │   └── v2/     # API version 2
│   │                       │   ├── graphql/    # GraphQL resolvers
│   │                       │   └── websocket/  # WebSocket handlers
│   │                       ├── cli/            # Command-line interface
│   │                       │   ├── commands/   # CLI command implementations
│   │                       │   ├── options/    # Command options and flags
│   │                       │   └── output/     # Output formatters
│   │                       ├── web/            # Web UI (if applicable)
│   │                       │   ├── controllers/ # MVC controllers
│   │                       │   └── views/      # View templates
│   │                       ├── dto/            # Presentation DTOs
│   │                       ├── mapper/         # DTO mappers
│   │                       ├── validation/     # Input validation
│   │                       ├── security/       # Authentication/authorization
│   │                       └── error/          # Error handlers
│   └── test/
│       └── kotlin/
│           └── com/
│               └── abitofhelp/
│                   └── hybrid/
│                       └── presentation/       # Presentation tests
└── build.gradle.kts                           # Module build configuration
```

## Key Components

### REST Controllers

```kotlin
@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val createOrderUseCase: CreateOrderUseCase,
    private val getOrderUseCase: GetOrderDetailsUseCase,
    private val listOrdersUseCase: ListOrdersUseCase
) {
    
    @PostMapping
    suspend fun createOrder(
        @Valid @RequestBody request: CreateOrderRequest
    ): ResponseEntity<OrderResponse> {
        val command = request.toCommand()
        
        return createOrderUseCase(command).fold(
            { error -> ResponseEntity.status(error.toHttpStatus()).body(error.toResponse()) },
            { order -> ResponseEntity.ok(OrderResponse.from(order)) }
        )
    }
    
    @GetMapping("/{id}")
    suspend fun getOrder(
        @PathVariable id: String
    ): ResponseEntity<OrderResponse> {
        return getOrderUseCase(id).fold(
            { error -> ResponseEntity.status(error.toHttpStatus()).body(error.toResponse()) },
            { order -> ResponseEntity.ok(OrderResponse.from(order)) }
        )
    }
    
    @GetMapping
    suspend fun listOrders(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) customerId: String?
    ): ResponseEntity<PagedResponse<OrderResponse>> {
        val query = ListOrdersQuery(page, size, customerId)
        
        return listOrdersUseCase(query).fold(
            { error -> ResponseEntity.status(error.toHttpStatus()).body(error.toResponse()) },
            { orders -> ResponseEntity.ok(PagedResponse.from(orders)) }
        )
    }
}
```

### GraphQL Resolvers

```kotlin
@Component
class OrderResolver(
    private val createOrderUseCase: CreateOrderUseCase,
    private val orderQueryService: OrderQueryService
) : GraphQLMutationResolver, GraphQLQueryResolver {
    
    suspend fun createOrder(input: CreateOrderInput): Order {
        val command = input.toCommand()
        return createOrderUseCase(command).fold(
            { error -> throw GraphQLException(error.message) },
            { order -> order }
        )
    }
    
    suspend fun order(id: String): Order? {
        return orderQueryService.findById(id).fold(
            { null },
            { it }
        )
    }
    
    suspend fun orders(
        first: Int = 20,
        after: String? = null
    ): OrderConnection {
        return orderQueryService.findAll(first, after)
    }
}
```

### CLI Commands

```kotlin
@Component
class OrderCli : CliktCommand(name = "order") {
    
    override fun run() = Unit
}

@Component
class CreateOrderCommand(
    private val createOrderUseCase: CreateOrderUseCase
) : CliktCommand(name = "create", help = "Create a new order") {
    
    private val customer by option("-c", "--customer", help = "Customer ID").required()
    private val items by option("-i", "--items", help = "Items as JSON").required()
    private val format by option("-f", "--format").choice("json", "yaml", "table").default("table")
    
    override fun run() {
        val command = CreateOrderCommand(
            customerId = customer,
            items = Json.decodeFromString(items)
        )
        
        val result = runBlocking {
            createOrderUseCase(command)
        }
        
        result.fold(
            { error -> echo(error.format(), err = true) },
            { order -> echo(formatOutput(order, format)) }
        )
    }
}
```

### Request/Response DTOs

```kotlin
// Request DTO with validation
data class CreateOrderRequest(
    @field:NotBlank(message = "Customer ID is required")
    val customerId: String,
    
    @field:NotEmpty(message = "Order must contain at least one item")
    @field:Valid
    val items: List<OrderItemRequest>
) {
    fun toCommand() = CreateOrderCommand(
        customerId = customerId,
        items = items.map { it.toDomain() }
    )
}

data class OrderItemRequest(
    @field:NotBlank(message = "Product ID is required")
    val productId: String,
    
    @field:Min(1, message = "Quantity must be at least 1")
    val quantity: Int,
    
    @field:DecimalMin("0.01", message = "Price must be positive")
    val price: BigDecimal
)

// Response DTO
data class OrderResponse(
    val id: String,
    val customerId: String,
    val items: List<OrderItemResponse>,
    val total: BigDecimal,
    val status: String,
    val createdAt: Instant
) {
    companion object {
        fun from(order: OrderDto) = OrderResponse(
            id = order.id,
            customerId = order.customerId,
            items = order.items.map { OrderItemResponse.from(it) },
            total = order.total,
            status = order.status,
            createdAt = order.createdAt
        )
    }
}
```

### Error Handling

```kotlin
@ControllerAdvice
class GlobalExceptionHandler {
    
    @ExceptionHandler(ApplicationError::class)
    fun handleApplicationError(error: ApplicationError): ResponseEntity<ErrorResponse> {
        val status = when (error) {
            is ApplicationError.ValidationError -> HttpStatus.BAD_REQUEST
            is ApplicationError.UnauthorizedError -> HttpStatus.UNAUTHORIZED
            is ApplicationError.NotFoundError -> HttpStatus.NOT_FOUND
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }
        
        return ResponseEntity
            .status(status)
            .body(ErrorResponse(
                code = error.code,
                message = error.message,
                timestamp = Instant.now()
            ))
    }
    
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleValidationError(ex: ConstraintViolationException): ResponseEntity<ValidationErrorResponse> {
        val errors = ex.constraintViolations.map { violation ->
            ValidationError(
                field = violation.propertyPath.toString(),
                message = violation.message
            )
        }
        
        return ResponseEntity
            .badRequest()
            .body(ValidationErrorResponse(errors))
    }
}
```

### Security Configuration

```kotlin
@EnableWebSecurity
@Configuration
class SecurityConfig {
    
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/v1/public/**").permitAll()
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt()
            }
            .cors { cors ->
                cors.configurationSource(corsConfigurationSource())
            }
            .build()
    }
    
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf("http://localhost:3000")
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/api/**", configuration)
        return source
    }
}
```

### Application Bootstrap

```kotlin
@SpringBootApplication
@ComponentScan(basePackages = ["com.abitofhelp.hybrid"])
class Application {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(Application::class.java, *args)
        }
    }
}

@Configuration
class ApplicationConfig {
    
    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        registerModule(KotlinModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
    
    @Bean
    fun webMvcConfigurer(): WebMvcConfigurer = object : WebMvcConfigurer {
        override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
            configurer
                .favorParameter(true)
                .parameterName("format")
                .ignoreAcceptHeader(false)
                .defaultContentType(MediaType.APPLICATION_JSON)
                .mediaType("json", MediaType.APPLICATION_JSON)
                .mediaType("xml", MediaType.APPLICATION_XML)
        }
    }
}
```

## API Documentation

### OpenAPI/Swagger Configuration
```kotlin
@Configuration
class OpenApiConfig {
    
    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(Info()
            .title("Order Management API")
            .version("1.0.0")
            .description("API for managing customer orders")
            .contact(Contact()
                .name("API Support")
                .email("api@example.com")))
        .components(Components()
            .addSecuritySchemes("bearer-key",
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")))
}
```

## Dependencies

- **Application module only**: For use cases and DTOs
- **Spring Boot**: For REST/Web framework
- **Spring Security**: For authentication/authorization
- **Clikt**: For CLI implementation
- **Jackson/Kotlinx Serialization**: For JSON handling
- **Bean Validation**: For input validation
- **No direct domain or infrastructure dependencies**

## Testing Strategy

### Controller Tests
```kotlin
@WebMvcTest(OrderController::class)
class OrderControllerTest {
    
    @MockkBean
    lateinit var createOrderUseCase: CreateOrderUseCase
    
    @Autowired
    lateinit var mockMvc: MockMvc
    
    @Test
    fun `should create order successfully`() {
        // Given
        val request = createOrderRequest()
        coEvery { createOrderUseCase(any()) } returns Either.Right(orderDto())
        
        // When & Then
        mockMvc.post("/api/v1/orders") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { exists() }
            jsonPath("$.status") { value("PENDING") }
        }
    }
}
```

### Integration Tests
```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class OrderApiIntegrationTest {
    
    @Autowired
    lateinit var mockMvc: MockMvc
    
    @Test
    fun `should complete order creation flow`() {
        // Test complete API flow
    }
}
```

## Best Practices

1. **Thin Controllers**: Keep controllers focused on HTTP concerns only
2. **DTO Validation**: Validate all input at the boundary
3. **Error Mapping**: Convert application errors to appropriate HTTP responses
4. **API Versioning**: Use URL versioning for REST APIs
5. **Content Negotiation**: Support multiple response formats
6. **HATEOAS**: Include links in responses for discoverability
7. **Rate Limiting**: Implement rate limiting for API endpoints
8. **API Documentation**: Keep OpenAPI specs up-to-date

## Important Notes

1. **No Business Logic**: All business logic stays in domain/application layers
2. **Framework Agnostic Core**: Presentation depends on application abstractions
3. **Security at the Edge**: Handle authentication/authorization here
4. **Observability**: Add logging, metrics, and tracing
5. **Graceful Degradation**: Handle failures with appropriate fallbacks