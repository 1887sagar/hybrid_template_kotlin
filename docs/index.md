# Kotlin Hybrid Architecture Template Documentation

Welcome to the comprehensive documentation for the Kotlin Hybrid Architecture Template. This template provides a production-ready foundation for building maintainable, testable, and scalable Kotlin applications using a powerful hybrid of Domain-Driven Design (DDD), Clean Architecture, Hexagonal Architecture, and the Dependency Inversion Principle (DIP).

## 🎯 What Makes This Template Special

- **Battle-tested Architecture**: Combines the best principles from DDD, Clean, and Hexagonal architectures
- **Automated Enforcement**: ArchUnit tests ensure architectural boundaries are respected
- **Production Ready**: Includes error handling, testing, and build configurations
- **Clear Examples**: Every pattern is demonstrated with practical code examples
- **Visual Documentation**: Architecture diagrams help understand the system design

## 📚 Documentation Structure

### 🚀 Getting Started

- **[Hybrid Architecture Guide](guides/hybrid_architecture_guide.md)** - 📖 **Start Here!** Comprehensive guide explaining DDD, Clean, Hexagonal, and DIP principles

### 📖 Comprehensive Layer Documentation
In-depth explanations of each architectural layer:

- **[Architecture Overview](guides/architecture-overview.md)** - Complete guide to the hybrid architecture
- **[Domain Layer](guides/domain-layer.md)** - Entities, value objects, services, and policies explained
- **[Application Layer](guides/application-layer.md)** - Use cases, ports, and orchestration patterns
- **[Infrastructure Layer](guides/infrastructure-layer.md)** - Adapters, implementations, and technical concerns
- **[Presentation Layer](guides/presentation-layer.md)** - User interfaces, controllers, and view models
- **[Bootstrap Module](guides/bootstrap-module.md)** - Composition root and application startup

### 📦 Module Documentation
Technical guides and README files for each module:

- **[Domain Module](../domain/README.md)** - Core business logic and domain models
- **[Application Module](../application/README.md)** - Use cases and application orchestration
- **[Infrastructure Module](../infrastructure/README.md)** - Technical adapters and implementations
- **[Presentation Module](../presentation/README.md)** - REST APIs, CLI, and user interfaces
- **[Architecture Tests](../architecture-tests/README.md)** - Automated architecture verification with ArchUnit

### 📊 Architecture Diagrams

Visual representations help understand the architecture better. All diagrams are available in the [`diagrams/`](diagrams/) directory:

#### System Design
- **[Architecture Overview](diagrams/architecture-overview.svg)** - Complete system view showing all layers and their relationships
- **[Layer Dependencies](diagrams/layer-dependencies.svg)** - Clean Architecture dependency rules with forbidden dependencies marked
- **[Hexagonal Ports & Adapters](diagrams/hexagonal-ports-adapters.svg)** - Input/output ports with their adapter implementations

#### Layer Class Diagrams
- **[Domain Layer Classes](diagrams/domain-layer-class.svg)** - Value objects, services, and policies with their relationships
- **[Application Layer Classes](diagrams/application-layer-class.svg)** - Use cases, input/output ports, and DTOs
- **[Infrastructure Layer Classes](diagrams/infrastructure-layer-class.svg)** - Adapter implementations for ports
- **[Presentation Layer Classes](diagrams/presentation-layer-class.svg)** - CLI factory and configuration
- **[Bootstrap Module Classes](diagrams/bootstrap-module-class.svg)** - Composition root and application startup

#### Flow Diagrams
- **[Greeting Flow Sequence](diagrams/greeting-flow-sequence.svg)** - Complete flow from user input to output
- **[Greeting Flow Async Sequence](diagrams/greeting-flow-sequence-async.svg)** - Async/concurrent flow with signal handling
- **[Error Handling Sequence](diagrams/error-handling-sequence.svg)** - Error propagation and transformation across layers
- **[Use Case Flow](diagrams/use-case-flow.svg)** - Sequence diagram showing a complete order creation flow
- **[Error Handling Flow](diagrams/error-handling-flow.svg)** - Error types and transformation between layers

#### Implementation Details
- **[Domain Model](diagrams/domain-model.svg)** - Entities, value objects, and domain services with relationships
- **[Package Structure](diagrams/package-structure.svg)** - Detailed module and package organization

## 🔗 Quick Reference Links

### Essential Files
- **[CLAUDE-Kotlin.md](../CLAUDE-Kotlin.md)** - Comprehensive code generation and maintenance criteria
- **[README.md](../README.md)** - Project overview and quick start instructions
- **[Makefile](../Makefile)** - Build automation and common tasks

### Key Concepts at a Glance

#### 🎯 The Four Pillars

1. **Domain-Driven Design (DDD)**
   - Business logic is the heart of the application
   - Rich domain models with behavior
   - Ubiquitous language shared with domain experts

2. **Clean Architecture**
   - Dependencies point inward toward the domain
   - Business logic is independent of frameworks
   - UI, database, and external services are plugins

3. **Hexagonal Architecture**
   - Ports define what the application needs
   - Adapters implement how it's provided
   - Easy to swap implementations

4. **Dependency Inversion Principle (DIP)**
   - Depend on abstractions, not concretions
   - High-level modules define interfaces
   - Low-level modules implement them

#### 🏗️ Layer Responsibilities

| Layer | Purpose | Dependencies | Examples |
|-------|---------|--------------|----------|
| **Domain** | Core business logic | None | Entities, Value Objects, Domain Services |
| **Application** | Use case orchestration | Domain only | Use Cases, Application Services, DTOs |
| **Infrastructure** | Technical implementations | Domain & Application | Database, APIs, Message Queues |
| **Presentation** | User interfaces | Application only | REST Controllers, CLI, Web UI |

## 🚦 Getting Started Path

1. **First Time?** Start with the [Hybrid Architecture Guide](guides/hybrid_architecture_guide.md)
2. **Ready to Code?** Check out the [README](../README.md) for quick start instructions
3. **Understanding Structure?** Review the module documentation above
4. **Need Visual Help?** Browse the architecture diagrams

## 📝 License

This template is open source and available under the BSD 3-Clause License.

---

*This documentation is continuously improved. If you find any issues or have suggestions, please contribute to the project.*
