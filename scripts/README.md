# Scripts Directory

## Overview

This directory contains utility scripts for common development and maintenance tasks.

## Available Scripts

### generate-diagrams.sh

Generates SVG diagrams from PlantUML source files.

**Purpose**: Converts all `.puml` files in `docs/diagrams/` to `.svg` format for documentation.

**Usage**:
```bash
./scripts/generate-diagrams.sh
```

**Requirements**:
- PlantUML installed (`brew install plantuml` on macOS)
- Java runtime (for PlantUML execution)

**What it does**:
1. Changes to the `docs/diagrams/` directory
2. Processes all `.puml` files
3. Generates corresponding `.svg` files
4. Lists all generated files

## Future Scripts

As the project grows, this directory might contain:

### Code Generation
- `generate-module.sh` - Create a new module with standard structure
- `generate-use-case.sh` - Scaffold a new use case with tests

### Development Helpers  
- `setup-dev.sh` - Set up development environment
- `run-with-profile.sh` - Run app with different configurations
- `benchmark.sh` - Run performance benchmarks

### Maintenance
- `update-dependencies.sh` - Update all dependencies safely
- `check-licenses.sh` - Verify dependency licenses
- `archive-logs.sh` - Archive old log files

### CI/CD Helpers
- `prepare-release.sh` - Prepare for a new release
- `publish-docs.sh` - Publish documentation
- `docker-build.sh` - Build Docker images

## Script Guidelines

When adding new scripts:

1. **Make it executable**: `chmod +x script-name.sh`
2. **Add shebang**: Start with `#!/bin/bash`
3. **Include help**: Add usage information
4. **Error handling**: Use `set -e` to exit on errors
5. **Document here**: Update this README

## Script Template

```bash
#!/bin/bash
# Script: script-name.sh
# Purpose: Brief description
# Usage: ./scripts/script-name.sh [options]

set -e  # Exit on error

# Help function
show_help() {
    echo "Usage: $0 [options]"
    echo "Options:"
    echo "  -h, --help    Show this help message"
    echo "  -v, --verbose Verbose output"
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Main logic here
echo "Running script..."

# Success
echo "Done!"
```

## Integration with Make

Many scripts are integrated with the Makefile for convenience:

- `make diagrams` - Calls `generate-diagrams.sh`
- `make run` - Could call a run script
- `make release` - Could call release preparation script

This allows both direct script usage and Make target usage.