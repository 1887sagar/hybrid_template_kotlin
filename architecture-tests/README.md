<!--
  Kotlin Hybrid Architecture Template
  Copyright (c) 2025 Michael Gardner, A Bit of Help, Inc.
  SPDX-License-Identifier: BSD-3-Clause
  See LICENSE file in the project root.
-->

# Architecture Tests Module

## Overview

This module contains ArchUnit tests that validate our hybrid DDD/Clean/Hexagonal architecture. These tests ensure that all modules follow the architectural rules defined in CLAUDE-Kotlin.md.

## Architecture Rules Validated

### 1. Layer Dependencies
- **Domain**: NO dependencies on any other layers
- **Application**: Depends only on Domain
- **Infrastructure**: Depends on Domain and Application
- **Presentation**: Depends only on Application (NOT Domain or Infrastructure)

### 2. Package Structure
- Value objects in `domain.value_object`
- Use cases in `application.use_case`
- Port interfaces in `application.port`
- Adapters in `infrastructure.driven.adapter`
- Commands in `presentation.cli.commands`

### 3. Naming Conventions
- Use cases end with `UseCase`
- Port interfaces end with `Port`
- Adapter implementations end with `Adapter`
- Repository interfaces end with `Repository`
- Commands end with `Command`

### 4. Vertical Slices
- Features are organized as independent vertical slices
- Each slice flows: Presentation → Application → Domain ← Infrastructure

## Running Architecture Tests

```bash
# Run all architecture tests
./gradlew :architecture-tests:test

# Run specific test class
./gradlew :architecture-tests:test --tests LayerDependencyTest

# Run with detailed output
./gradlew :architecture-tests:test --info
```

## Test Structure

```
architecture-tests/
└── src/test/kotlin/.../architecture/
    ├── ArchitectureConstants.kt    # Shared constants
    ├── LayerDependencyTest.kt      # Validates layer dependencies
    ├── PackageStructureTest.kt     # Validates package conventions
    ├── NamingConventionTest.kt     # Validates naming standards
    └── VerticalSliceTest.kt        # Validates feature slices
```

## Adding New Architecture Rules

1. Create a new test class in the architecture package
2. Import the classes to test: `ClassFileImporter().importPackages(...)`
3. Define rules using ArchUnit's fluent API
4. Add descriptive error messages with `.because(...)`

## Example Rule

```kotlin
@Test
fun `domain should not depend on frameworks`() {
    val rule = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "jakarta..")
        .because("Domain must be framework-agnostic")
        
    rule.check(classes)
}
```

## Integration with CI/CD

These tests should run in CI/CD pipeline to catch architectural violations early:
- On every pull request
- Before merging to main
- As part of the build verification
