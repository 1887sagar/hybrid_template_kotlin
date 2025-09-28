# BuildSrc Module

## Overview

The `buildSrc` directory is a special Gradle feature that allows you to write build logic in Kotlin (or Java/Groovy). Code in this directory is automatically compiled and made available to all build scripts in the project.

## Purpose

This module provides:
- **Convention Plugins**: Reusable build configurations
- **Shared Dependencies**: Centralized version management
- **Build Logic**: Custom tasks and configurations
- **Type Safety**: Kotlin-based build scripts with IDE support

## Structure

```
buildSrc/
├── build.gradle.kts                           # BuildSrc's own build configuration
└── src/
    └── main/
        └── kotlin/
            ├── hybrid.kotlin-application.gradle.kts  # Convention for applications
            ├── hybrid.kotlin-common.gradle.kts       # Common Kotlin settings
            ├── hybrid.kotlin-library.gradle.kts      # Convention for libraries
            └── hybrid.settings.gradle.kts            # Repository configuration
```

## Convention Plugins

### hybrid.kotlin-common
Applied to all Kotlin modules:
- Kotlin JVM configuration
- Code quality tools (Detekt, ktlint)
- JaCoCo test coverage
- Common compiler options
- Testing framework setup

```kotlin
plugins {
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
    jacoco
}

// Common configurations for all modules
```

### hybrid.kotlin-library
For library modules (domain, application, etc.):
- Extends kotlin-common
- Library-specific settings
- No main class configuration

```kotlin
plugins {
    id("hybrid.kotlin-common")
}

// Library-specific configurations
```

### hybrid.kotlin-application  
For executable modules:
- Extends kotlin-common
- Application plugin
- Main class configuration
- Distribution settings

```kotlin
plugins {
    id("hybrid.kotlin-common")
    application
}

// Application-specific configurations
```

### hybrid.settings
Repository configuration used in settings.gradle.kts:
- Plugin repository setup
- Consistent repository configuration

## Benefits

### 1. DRY (Don't Repeat Yourself)
Instead of duplicating configuration across modules:
```kotlin
// Without buildSrc - repeated in every module
plugins {
    kotlin("jvm") version "1.9.20"
    id("io.gitlab.arturbosch.detekt") version "1.23.3"
}

// With buildSrc - in each module
plugins {
    id("hybrid.kotlin-library")  // All configuration included!
}
```

### 2. Type Safety
Get IDE support with autocomplete and error checking:
```kotlin
// Type-safe configuration
tasks.test {
    useJUnitPlatform()  // IDE knows about this
}
```

### 3. Centralized Version Management
Update versions in one place:
```kotlin
// In buildSrc
object Versions {
    const val kotlin = "1.9.20"
    const val arrow = "1.2.1"
    const val kotest = "5.8.0"
}
```

### 4. Custom Tasks
Easy to add project-specific tasks:
```kotlin
tasks.register("projectReport") {
    doLast {
        println("Project: ${project.name}")
        println("Version: ${project.version}")
    }
}
```

## Usage

### In Module Build Files
```kotlin
// domain/build.gradle.kts
plugins {
    id("hybrid.kotlin-library")
}

dependencies {
    implementation(libs.arrow.core)
    testImplementation(libs.kotest.runner.junit5)
}
```

### Adding New Convention Plugins
1. Create new `.gradle.kts` file in `buildSrc/src/main/kotlin/`
2. Define the plugin configuration
3. Apply in modules as needed

## Best Practices

1. **Keep It Simple**: Don't over-engineer build logic
2. **Document Conventions**: Explain what each plugin does
3. **Version Catalogs**: Use with `libs.versions.toml`
4. **Incremental**: Make build logic cacheable
5. **Test Build Logic**: Yes, you can test buildSrc code!

## Common Patterns

### Configuring All Projects
```kotlin
// In a convention plugin
allprojects {
    repositories {
        mavenCentral()
    }
}
```

### Conditional Configuration
```kotlin
// Only for modules with tests
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
```

### Custom Extensions
```kotlin
// Define custom DSL
interface HybridExtension {
    val featureFlags: MapProperty<String, Boolean>
}

// Use in build scripts
hybrid {
    featureFlags.put("asyncMode", true)
}
```

## Troubleshooting

### Slow IDE Sync
- Keep buildSrc minimal
- Avoid heavy computations
- Cache expensive operations

### Changes Not Applied
- Clean and rebuild: `./gradlew clean build`
- Invalidate IDE caches
- Check for typos in plugin IDs

### Version Conflicts
- Use platform dependencies
- Enforce versions with constraints
- Check dependency tree: `./gradlew dependencies`

## Further Reading

- [Gradle BuildSrc Documentation](https://docs.gradle.org/current/userguide/organizing_gradle_projects.html#sec:build_sources)
- [Kotlin DSL Primer](https://docs.gradle.org/current/userguide/kotlin_dsl.html)
- [Convention Plugins](https://docs.gradle.org/current/samples/sample_convention_plugins.html)