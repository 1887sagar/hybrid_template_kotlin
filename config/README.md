# Configuration Directory

**Version:** 1.0.0
**Date:** January 2025
**SPDX-License-Identifier:** BSD-3-Clause
**License File:** See LICENSE file in the project root.
**Copyright:** © 2025 Michael Gardner, A Bit of Help, Inc.
**Authors:** Michael Gardner
**Status:** Released

## Overview

This directory contains configuration files for various code quality and analysis tools used in the project.

## Structure

```
config/
├── detekt/
│   └── detekt.yml    # Detekt static code analysis configuration
└── owasp/
    └── suppressions.xml  # OWASP dependency check suppressions
```

## Detekt Configuration

### What is Detekt?

Detekt is a static code analysis tool for Kotlin that helps identify code smells, complexity issues, and potential bugs. It's integrated into our build process to maintain code quality.

### Configuration File: `detekt.yml`

The configuration customizes Detekt rules for this project:

```yaml
build:
  maxIssues: 0  # Fail build on any issue

style:
  MaxLineLength:
    maxLineLength: 120  # Increased for better readability
    
complexity:
  TooManyFunctions:
    active: false  # Disabled for test files
    
naming:
  FunctionNaming:
    functionPattern: '[a-zA-Z][a-zA-Z0-9]*'  # Standard naming
    
comments:
  CommentOverPrivateProperty:
    active: false  # We document via KDoc
```

### Running Detekt

```bash
# Run detekt check
./gradlew detekt

# Generate HTML report
./gradlew detektGenerateHtmlReport

# Run with type resolution (slower but more accurate)
./gradlew detektMain
```

### Key Rules Configured

#### Complexity Rules
- **Cognitive Complexity**: Limit complex method logic
- **Cyclomatic Complexity**: Reduce branching
- **Long Method**: Keep methods focused
- **Large Class**: Promote single responsibility

#### Style Rules  
- **Max Line Length**: 120 characters
- **Magic Numbers**: Use named constants
- **Wildcard Imports**: Discouraged
- **Unused Imports**: Automatically flagged

#### Naming Rules
- **Class Naming**: PascalCase required
- **Function Naming**: camelCase required
- **Package Naming**: lowercase only
- **Variable Naming**: camelCase required

### Suppressing Warnings

When necessary, suppress Detekt warnings:

```kotlin
// Suppress for a function
@Suppress("MagicNumber")
fun calculateScore(): Int = 42

// Suppress for a file
@file:Suppress("WildcardImport")

// Suppress for a block
@Suppress("ComplexCondition")
if (complex && condition && here) {
    // code
}
```

### Custom Rules

To add custom rules:

1. Create a custom rule set module
2. Add to `detekt.yml`:
```yaml
plugins:
  - path: "path/to/custom-rules.jar"
```

## OWASP Dependency Check Configuration

### What is OWASP Dependency Check?

OWASP Dependency Check is a security tool that identifies project dependencies with known vulnerabilities. It checks against the National Vulnerability Database (NVD) and other sources.

### Configuration File: `suppressions.xml`

This file manages false positives - vulnerabilities that are flagged but don't actually affect your project.

### Suppression Guidelines

Only add suppressions after careful review:

1. **Verify it's a false positive** - Check if the CVE really doesn't apply
2. **Document thoroughly** - Include review date, reviewer, and explanation
3. **Be specific** - Target the exact dependency and CVE
4. **Review regularly** - Suppressions may become invalid over time

### Example Suppression

```xml
<suppress>
    <notes><![CDATA[
    Reviewed: 2025-01-15 by MG
    CVE-2021-12345 affects Spring Boot, not Kotlin stdlib
    False positive due to similar package naming
    ]]></notes>
    <packageUrl regex="true">^pkg:maven/org\.jetbrains\.kotlin/kotlin\-stdlib.*$</packageUrl>
    <cve>CVE-2021-12345</cve>
</suppress>
```

### Running Security Checks

```bash
# Run vulnerability scan
./gradlew dependencyCheckAggregate

# Or use Make
make security

# View reports
open build/reports/dependency-check-report.html
```

### Security Thresholds

- **Build fails** on CVSS score ≥ 7.0 (High severity)
- **Warning** on CVSS score ≥ 4.0 (Medium severity)
- **Info** on CVSS score < 4.0 (Low severity)

## Other Potential Configurations

As the project grows, this directory might contain:

### ktlint Configuration
```
config/
└── ktlint/
    └── .editorconfig    # Kotlin code style
```

### Code Coverage
```
config/
└── jacoco/
    └── jacoco.gradle    # Coverage thresholds
```

### Git Hooks
```
config/
└── git-hooks/
    ├── pre-commit       # Format and lint
    └── pre-push         # Run tests
```

## Best Practices

1. **Team Agreement**: Discuss and agree on rules
2. **Gradual Adoption**: Enable rules incrementally
3. **Document Suppressions**: Explain why rules are suppressed
4. **Regular Reviews**: Update configuration as team learns
5. **IDE Integration**: Configure IDE to match

## IDE Integration

### IntelliJ IDEA
1. Install Detekt plugin
2. Configure to use project's detekt.yml
3. Enable inspections on save

### VS Code
1. Install Kotlin extension
2. Configure detekt path
3. Enable format on save

## Troubleshooting

### False Positives
- Review rule configuration
- Consider rule parameters
- Suppress with explanation
- Report bugs to Detekt

### Performance Issues
- Disable type resolution for speed
- Run on changed files only
- Use parallel execution
- Increase heap size

### Rule Conflicts
- Some rules may conflict
- Choose team preference
- Document decisions
- Be consistent

## Resources

- [Detekt Documentation](https://detekt.github.io/detekt/)
- [Detekt Rules](https://detekt.github.io/detekt/complexity.html)
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [ktlint Rules](https://pinterest.github.io/ktlint/)
