# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

### Changed

### Fixed

### Removed

### Security

## [1.0.0] - September 29, 2025

### Added
- SystemSignals abstraction for testable, cross-platform signal handling
- ExitCode enum following BSD sysexits.h conventions with automatic exception mapping
- BootstrapLogger interface for structured lifecycle logging (ConsoleBootstrapLogger, NoOpBootstrapLogger)
- Enhanced CLI support with `--version` and `--quiet` flags
- ShowVersionException for version display handling
- BufferedFileOutputAdapter with NIO AsynchronousFileChannel for high-performance file writing
- Factory methods for BufferedFileOutputAdapter (highThroughput, lowLatency)
- Comprehensive release automation script (prepare, release, publish)
- Architecture tests Makefile target
- Enhanced security validation in SecureArgParser (path traversal, DoS prevention)
- Testable abstractions for signal handling and bootstrap logging

### Changed
- AsyncApp refactored to use SystemSignals abstraction instead of direct sun.misc.Signal
- All lifecycle logging now routes through BootstrapLogger instead of println
- FileOutputAdapter enhanced with autoFlush parameter and security validation
- SecureArgParser refactored to reduce cyclomatic complexity
- Exit codes now centralized through ExitCode enum
- Enhanced documentation for junior developers without explicit skill level mentions
- Kotlin plugin version pinned at 2.0.21 for reproducible builds
- PERCENTAGE_MULTIPLIER constant extracted in CreateBatchGreetingsUseCase

### Fixed
- AsyncAppTest reliability issues with signal handling
- sun.misc.Signal dependency causing test failures
- Detekt magic number warnings
- SwallowedException warnings with proper exception handling
- PlantUML diagram syntax errors
- generate-diagrams.sh script path navigation
- Bootstrap README trailing content corruption

### Removed
- Direct usage of sun.misc.Signal (replaced with SystemSignals abstraction)
- Direct println usage in bootstrap lifecycle (replaced with BootstrapLogger)

### Security
- Enhanced path validation in FileOutputAdapter to prevent directory traversal attacks
- DoS prevention through argument count limits in SecureArgParser
- Windows reserved filename validation
- Null byte detection in file paths
- Maximum path length enforcement

## [0.1.0] - 2024-12-01

### Added
- Initial release of Kotlin Hybrid Architecture Template
- Domain-Driven Design (DDD) implementation
- Clean Architecture structure
- Hexagonal Architecture (Ports & Adapters)
- Comprehensive test coverage
- Architecture tests with ArchUnit
- PlantUML diagrams for documentation
- Async/Coroutine support with structured concurrency
- Either monad for functional error handling
- Manual dependency injection via CompositionRoot