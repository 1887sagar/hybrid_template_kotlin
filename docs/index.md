# Kotlin Hybrid Architecture Template Documentation

**Version:** 1.0.0  
**Date:** September 29, 2025
**License:** BSD-3-Clause  
**Copyright:** © 2025 Michael Gardner, A Bit of Help, Inc.  
**Authors:** Michael Gardner  
**Status:** Released

Welcome to the documentation hub for the Kotlin Hybrid Architecture Template. This template provides a battle-tested foundation for building enterprise-grade Kotlin applications using proven architectural patterns.

## What You'll Learn

This documentation will help you understand:
- How to structure scalable applications
- Why certain architectural decisions matter
- When to use different patterns
- How to avoid common pitfalls

## Documentation Overview

### 🎯 Start Here

If you're new to the template, follow this path:

1. **[Quick Start Guide](../README.md)** - Get up and running in 5 minutes
2. **[Architecture Overview](guides/architecture-overview.md)** - Understand the big picture
3. **[Hybrid Architecture Guide](guides/hybrid_architecture_guide.md)** - Deep dive into architectural patterns

### 📚 Core Concepts

Learn about each architectural layer and its responsibilities:

- **[Domain Layer Guide](guides/domain-layer.md)**  
  The heart of your application - pure business logic with zero dependencies

- **[Application Layer Guide](guides/application-layer.md)**  
  Orchestrates business logic and defines boundaries

- **[Infrastructure Layer Guide](guides/infrastructure-layer.md)**  
  Connects your application to the outside world

- **[Presentation Layer Guide](guides/presentation-layer.md)**  
  How users interact with your application

- **[Bootstrap Module Guide](guides/bootstrap-module.md)**  
  Wires everything together at startup

### 🛠️ Module Documentation

Detailed technical documentation for each module:

- **[Domain Module](../domain/README.md)** - Business entities and rules
- **[Application Module](../application/README.md)** - Use cases and workflows
- **[Infrastructure Module](../infrastructure/README.md)** - Database, file system, APIs
- **[Presentation Module](../presentation/README.md)** - User interfaces
- **[Architecture Tests](../architecture-tests/README.md)** - Automated rule enforcement

### 📊 Visual Learning

Architecture diagrams help visualize the system design:

#### System Overview
- **[Architecture Overview](diagrams/architecture-overview.svg)** - Bird's eye view of the system
- **[Layer Dependencies](diagrams/layer-dependencies.svg)** - How layers connect
- **[Package Structure](diagrams/package-structure.svg)** - Module organization

#### Detailed Class Diagrams
- **[Domain Model](diagrams/domain-model.svg)** - Core business objects
- **[Application Classes](diagrams/application-layer-class.svg)** - Use cases and ports
- **[Infrastructure Classes](diagrams/infrastructure-layer-class.svg)** - Adapter implementations

#### Flow Sequences
- **[Request Flow](diagrams/greeting-flow-sequence.svg)** - Trace a request through layers
- **[Async Flow](diagrams/greeting-flow-sequence-async.svg)** - Concurrent operations
- **[Error Handling](diagrams/error-handling-sequence.svg)** - How errors propagate

## Learning Path

### For Beginners

Start with understanding the basics:

1. **Read**: [Architecture Overview](guides/architecture-overview.md)
2. **Try**: Build and run the sample application
3. **Experiment**: Modify the greeting message
4. **Learn**: Trace the code changes through layers

### For Intermediate Developers

Deepen your understanding:

1. **Study**: [Domain Layer Guide](guides/domain-layer.md)
2. **Practice**: Add a new value object
3. **Implement**: Create a new use case
4. **Test**: Write comprehensive tests

### For Advanced Developers

Master the architecture:

1. **Analyze**: Review architecture tests
2. **Extend**: Add a new infrastructure adapter
3. **Optimize**: Implement caching strategies
4. **Share**: Document your patterns

## Common Questions

### Why This Architecture?

Traditional architectures often force trade-offs. This hybrid approach combines:
- **DDD**: Rich business modeling
- **Clean Architecture**: Clear boundaries
- **Hexagonal**: Flexible adapters

### How Do I Start?

1. Clone the repository
2. Run `./gradlew build`
3. Explore the code structure
4. Make small changes and observe

### Where Can I Get Help?

- **Documentation**: Start with guides in `/docs/guides/`
- **Examples**: Study the existing code
- **Tests**: Learn from test implementations
- **Standards**: Review `/standards/Claude_Kotlin.md`

## Best Practices

### Always Remember

1. **Domain First**: Start with business logic
2. **Test Everything**: Write tests as you go
3. **Keep It Simple**: Don't over-engineer
4. **Document Why**: Explain decisions, not just code

### Common Mistakes to Avoid

1. **Skipping Layers**: Don't call infrastructure from presentation
2. **Anemic Models**: Put behavior in domain objects
3. **Framework Coupling**: Keep frameworks at the edges
4. **Missing Tests**: Test each layer appropriately

## Next Steps

Ready to dive deeper? Here's what to explore:

1. **[Hybrid Architecture Guide](guides/hybrid_architecture_guide.md)** - Complete architectural patterns
2. **[Bootstrap Module](guides/bootstrap-module.md)** - How the application starts
3. **[Architecture Tests](../architecture-tests/README.md)** - Automated rule enforcement

## Contributing

Want to improve the documentation? We welcome contributions:

1. Fork the repository
2. Create a feature branch
3. Make your improvements
4. Submit a pull request

Remember: Good documentation helps everyone learn faster!

---

*This documentation is a living guide. It grows with community contributions and feedback.*