# Architecture Tests Module

**Version:** 1.0.0  
**Date:** January 2025  
**License:** BSD-3-Clause  
**Copyright:** © 2025 Michael Gardner, A Bit of Help, Inc.  
**Authors:** Michael Gardner  
**Status:** Released

## What are Architecture Tests?

Architecture tests are automated guards that enforce your architectural decisions. Think of them as "code police" that continuously verify your architecture hasn't been accidentally violated. They catch architectural drift before it becomes technical debt.

## Why Architecture Tests Matter

Without architecture tests, you'll inevitably see:
- **Dependency violations**: Presentation calling domain directly
- **Circular dependencies**: Modules depending on each other
- **Naming inconsistencies**: Some ports end with "Port", others don't
- **Layering violations**: Infrastructure code in domain layer
- **Gradual architecture erosion**: The "big ball of mud" effect

Architecture tests prevent all of this automatically.

## What We Test

### 1. Dependency Direction Rules

```kotlin
@ArchTest
val domainLayerShouldNotDependOnAnyOtherLayer = 
    classes()
        .that().resideInAPackage("..domain..")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage(
            "..domain..",
            "java..",
            "kotlin..",
            "arrow.core.."
        )
```

**This ensures:**
- Domain layer has zero outward dependencies
- Domain can't accidentally call infrastructure
- Domain remains pure business logic

### 2. Layer Isolation

```kotlin
@ArchTest
val presentationShouldNotDependOnInfrastructure =
    noClasses()
        .that().resideInAPackage("..presentation..")
        .should().dependOnClassesThat()
        .resideInAPackage("..infrastructure..")
```

**This prevents:**
- Presentation layer directly calling database code
- UI components importing HTTP clients
- CLI components using file system adapters

### 3. Naming Conventions

```kotlin
@ArchTest
val portInterfacesShouldEndWithPort =
    classes()
        .that().resideInAPackage("..port..")
        .and().areInterfaces()
        .should().haveSimpleNameEndingWith("Port")

@ArchTest
val useCasesShouldEndWithUseCase =
    classes()
        .that().resideInAPackage("..usecase..")
        .should().haveSimpleNameEndingWith("UseCase")
```

**This enforces:**
- Consistent naming across the codebase
- Clear identification of architectural components
- Better code readability and navigation

### 4. Package Organization

```kotlin
@ArchTest
val valueObjectsShouldBeInValuePackage =
    classes()
        .that().areAnnotatedWith(JvmInline::class.java)
        .should().resideInAPackage("..domain.value..")

@ArchTest
val adaptersShouldBeInAdapterPackage =
    classes()
        .that().haveSimpleNameEndingWith("Adapter")
        .should().resideInAPackage("..infrastructure.adapter..")
```

## Real Test Examples

### Testing Domain Purity

```kotlin
class DomainLayerTest : DescribeSpec({
    
    describe("Domain Layer Purity") {
        it("should not depend on external frameworks") {
            val rule = classes()
                .that().resideInAPackage("..domain..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                    "..domain..",
                    "java..",
                    "kotlin..",
                    "arrow.core..",
                    "kotlinx.datetime.."
                )
            
            rule.check(importedClasses)
        }
        
        it("should not contain any infrastructure dependencies") {
            val rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                    "..infrastructure..",
                    "org.springframework..",
                    "javax.persistence.."
                )
            
            rule.check(importedClasses)
        }
    }
})
```

### Testing Use Case Structure

```kotlin
class ApplicationLayerTest : DescribeSpec({
    
    describe("Use Case Implementation") {
        it("should implement input port interfaces") {
            val rule = classes()
                .that().haveSimpleNameEndingWith("UseCase")
                .should().implement(
                    classesAssignableTo(
                        ClassUtils.forName("..port.input..", null)
                    )
                )
            
            rule.check(importedClasses)
        }
        
        it("should only depend on domain and ports") {
            val rule = classes()
                .that().resideInAPackage("..application.usecase..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                    "..domain..",
                    "..application.port..",
                    "..application.dto..",
                    "..application.error..",
                    "kotlin..",
                    "arrow.core.."
                )
            
            rule.check(importedClasses)
        }
    }
})
```

### Testing Infrastructure Adapters

```kotlin
class InfrastructureLayerTest : DescribeSpec({
    
    describe("Infrastructure Adapters") {
        it("should implement output port interfaces") {
            val rule = classes()
                .that().haveSimpleNameEndingWith("Adapter")
                .and().resideInAPackage("..infrastructure.adapter.output..")
                .should().implement(
                    classesAssignableTo(
                        ClassUtils.forName("..port.output..", null)
                    )
                )
            
            rule.check(importedClasses)
        }
        
        it("should be the only layer accessing external libraries") {
            val rule = classes()
                .that().resideOutsideOfPackage("..infrastructure..")
                .should().notDependOnClassesThat()
                .resideInAnyPackage(
                    "org.springframework..",
                    "com.fasterxml.jackson..",
                    "java.sql.."
                )
            
            rule.check(importedClasses)
        }
    }
})
```

## Running Architecture Tests

### Command Line

```bash
# Run all architecture tests
./gradlew :architecture-tests:test

# Run specific test category
./gradlew :architecture-tests:test --tests "*DomainLayer*"

# Run with detailed output
./gradlew :architecture-tests:test --info

# Generate architecture test report
./gradlew :architecture-tests:test --continue
```

### IDE Integration

Most IDEs will run these tests like any other unit tests:
- **IntelliJ IDEA**: Right-click on test class → "Run"
- **VSCode**: Use Kotlin extension test runner
- **Eclipse**: JUnit runner integration

### CI/CD Integration

```yaml
# GitHub Actions example
- name: Run Architecture Tests
  run: ./gradlew :architecture-tests:test
  
- name: Upload Test Reports
  uses: actions/upload-artifact@v3
  if: failure()
  with:
    name: architecture-test-reports
    path: architecture-tests/build/reports/tests/
```

## Understanding Test Failures

### Common Failure: Dependency Violation

```
java.lang.AssertionError: 
Architecture Violation [Priority: MEDIUM] - 
Rule 'classes that reside in a package '..domain..' should only depend on classes that reside in any package ['..domain..', 'java..', 'kotlin..', 'arrow.core..']' was violated (1 times):
Class <com.abitofhelp.hybrid.domain.service.DefaultGreetingService> depends on class <com.abitofhelp.hybrid.infrastructure.adapter.output.ConsoleOutputAdapter> in (/bootstrap/bin/main/com/abitofhelp/hybrid/domain/service/DefaultGreetingService.class:15)
```

**What this means:**
- Domain service is directly importing infrastructure code
- This violates the dependency inversion principle
- Fix: Use dependency injection instead

**How to fix:**
```kotlin
// BAD: Direct dependency
class DefaultGreetingService {
    private val output = ConsoleOutputAdapter() // ❌ Infrastructure import
}

// GOOD: Dependency injection
class DefaultGreetingService(
    private val output: OutputPort // ✅ Interface from application layer
)
```

### Common Failure: Naming Convention

```
java.lang.AssertionError:
Rule 'classes that reside in a package '..port..' and are interfaces should have simple name ending with 'Port'' was violated (1 times):
Class <com.abitofhelp.hybrid.application.port.output.EmailService> does not have simple name ending with 'Port'
```

**How to fix:**
```kotlin
// BAD: Inconsistent naming
interface EmailService { } // ❌

// GOOD: Follows convention
interface EmailServicePort { } // ✅
```

## Custom Architecture Rules

### Adding Business-Specific Rules

```kotlin
@ArchTest
val repositoriesShouldOnlyBeCalledByUseCases =
    classes()
        .that().haveSimpleNameEndingWith("Repository")
        .should().onlyBeAccessed().byClassesThat()
        .haveSimpleNameEndingWith("UseCase")

@ArchTest
val domainEventsShouldBeImmutable =
    classes()
        .that().implement(DomainEvent::class.java)
        .should().beRecords()
        .orShould().haveOnlyFinalFields()
```

### Testing Circular Dependencies

```kotlin
@ArchTest
val noCircularDependencies =
    slices()
        .matching("com.abitofhelp.hybrid.(*)..")
        .should().beFreeOfCycles()
```

## Ignoring Specific Violations

Sometimes you need to temporarily ignore violations:

```kotlin
@ArchTest
val domainLayerRule = classes()
    .that().resideInAPackage("..domain..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("..domain..", "java..", "kotlin..")
    .because("Domain layer must be pure")
    .ignoreDependency(
        "com.abitofhelp.hybrid.domain.service.LegacyService",
        "com.legacy.ExternalLibrary"
    )
```

**Use sparingly!** Document why the violation exists and plan to fix it.

## Architecture Test Best Practices

### ✅ DO:
- Run architecture tests in CI/CD
- Write tests for all major architectural decisions
- Use descriptive test names and error messages
- Keep tests simple and focused
- Update tests when architecture evolves

### ❌ DON'T:
- Ignore architecture test failures
- Write overly complex architecture rules
- Test implementation details instead of architectural principles
- Use too many exceptions to rules
- Skip architecture tests in development

## Integration with Build Process

### Gradle Integration

```kotlin
// build.gradle.kts
tasks.named("check") {
    dependsOn(":architecture-tests:test")
}

// Fail build on architecture violations
tasks.withType<Test> {
    failFast = true
}
```

### Maven Integration

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <includes>
            <include>**/*ArchTest.java</include>
        </includes>
    </configuration>
</plugin>
```

## Metrics and Reporting

### Generate Architecture Reports

```kotlin
// Custom test to generate architectural overview
@Test
fun generateArchitectureReport() {
    val layerDependencies = classes()
        .should(ArchitectureReportGenerator.generateLayerDiagram())
    
    layerDependencies.check(importedClasses)
}
```

### Measuring Architectural Debt

```kotlin
@Test
fun measureArchitecturalComplexity() {
    val complexity = classes()
        .should(ArchMetrics.calculateCyclomaticComplexity())
    
    // Generate metrics report
    complexity.check(importedClasses)
}
```

## Summary

Architecture tests are your safety net against architectural decay. They:

- **Enforce** architectural boundaries automatically
- **Catch** violations early in development
- **Document** architectural decisions in code
- **Enable** safe refactoring with confidence
- **Maintain** code quality over time

Remember: Architecture tests are like unit tests for your architecture - they should be fast, reliable, and run often!