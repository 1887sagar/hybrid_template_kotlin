# Hybrid Architecture Template Makefile
# Common tasks for building, testing, and maintaining the project
#
# Dependencies:
# - JDK 21 (for Gradle/Kotlin)
# - PlantUML (optional, for diagrams)

# Default target
.DEFAULT_GOAL := help

# Gradle wrapper command
GRADLE := ./gradlew

# Build output directories
BUILD_DIR := build
DIST_DIR := $(BUILD_DIR)/distributions

# Helpers
ZAP := python3 scripts/zap_gradle.py
DRY_RUN := true
AGGRESSIVE := true
INCLUDE_KOTLIN_NATIVE := false

.PHONY: help
help: ## Display this help message
	@echo "Hybrid Architecture Template - Available targets:"
	@echo ""
	@awk 'BEGIN {FS = ":.*##"; printf "\033[36m%-20s\033[0m %s\n", "Target", "Description"} /^[a-zA-Z_-]+:.*?##/ { printf "\033[36m%-20s\033[0m %s\n", $$1, $$2 }' $(MAKEFILE_LIST)

# ===== Build Targets =====

.PHONY: all
all: clean build test ## Clean, build, and test everything

.PHONY: build
build: ## Build the project
	$(GRADLE) build

.PHONY: clean
clean: clean-diagrams ## Clean all build artifacts
	$(GRADLE) clean
	rm -rf $(BUILD_DIR)
	rm -rf */$(BUILD_DIR)
	rm -rf .gradle
	find . -name "*.class" -delete
	find . -name ".DS_Store" -delete

.PHONY: compile
compile: ## Compile the main source code
	$(GRADLE) compileKotlin

# ===== Testing Targets =====

.PHONY: test
test: ## Run all tests
	$(GRADLE) test
	@echo "All tests completed!"

.PHONY: test-unit
test-unit: ## Run unit tests only
	$(GRADLE) test --tests "*Test"

.PHONY: test-integration
test-integration: ## Run integration tests
	$(GRADLE) test --tests "*IntegrationTest"

.PHONY: test-coverage
test-coverage: ## Run tests with coverage report
	$(GRADLE) test jacocoTestReport
	@echo "Module coverage reports available at: */build/reports/jacoco/test/html/index.html"
	$(GRADLE) jacocoRootReport
	@echo "Aggregate coverage report available at: build/reports/jacoco/aggregate/index.html"

# ===== Code Quality Targets =====

.PHONY: format
format: ## Format code with ktlint
	$(GRADLE) ktlintFormat

.PHONY: lint
lint: ## Run ktlint checks
	$(GRADLE) ktlintCheck

.PHONY: detekt
detekt: ## Run detekt static analysis
	$(GRADLE) detekt
	@echo "Detekt report available at: build/reports/detekt/detekt.html"

.PHONY: security
security: ## Check dependencies for known vulnerabilities
	$(GRADLE) dependencyCheckAggregate
	@echo "Security reports available in each module's build/reports/ directory"

.PHONY: check
check: lint detekt security ## Run all code quality checks

# ===== Documentation Targets =====

.PHONY: docs
docs: diagrams ## Generate diagrams and update documentation
	@echo "Documentation is in docs/index.md"
	@echo "Diagrams generated in docs/diagrams/"

# ===== Diagram Targets =====

# Diagram source and output directories
DIAGRAM_DIR := docs/diagrams
PUML_FILES := $(wildcard $(DIAGRAM_DIR)/*.puml)
SVG_FILES := $(PUML_FILES:.puml=.svg)
PLANTUML_JAR := tools/plantuml.jar
DIAGRAM_SCRIPT := scripts/generate-diagrams.sh

.PHONY: diagrams
diagrams: ## Generate all architecture diagrams
	@if [ -f $(DIAGRAM_SCRIPT) ]; then \
		$(DIAGRAM_SCRIPT); \
	else \
		$(MAKE) $(SVG_FILES); \
	fi

$(DIAGRAM_DIR)/%.svg: $(DIAGRAM_DIR)/%.puml
	@echo "Generating $@ from $<"
	@if command -v plantuml >/dev/null 2>&1; then \
		plantuml -tsvg $<; \
	elif [ -f $(PLANTUML_JAR) ]; then \
		java -jar $(PLANTUML_JAR) -tsvg $<; \
	else \
		echo "Error: PlantUML not found. Install with 'brew install plantuml' or download to $(PLANTUML_JAR)"; \
		exit 1; \
	fi

.PHONY: clean-diagrams
clean-diagrams: ## Remove generated diagram files
	@echo "Cleaning generated diagrams..."
	rm -f $(DIAGRAM_DIR)/*.svg
	rm -f $(DIAGRAM_DIR)/*.png

.PHONY: watch-diagrams
watch-diagrams: ## Watch and regenerate diagrams on changes
	@echo "Watching for diagram changes (requires fswatch)..."
	@which fswatch > /dev/null || (echo "Error: fswatch not installed. Install with: brew install fswatch" && exit 1)
	fswatch -o $(DIAGRAM_DIR)/*.puml | xargs -n1 -I{} make diagrams

# ===== Utility Targets =====

.PHONY: deps
deps: ## Display project dependencies
	$(GRADLE) dependencies

.PHONY: tasks
tasks: ## List all available Gradle tasks
	$(GRADLE) tasks

.PHONY: refresh
refresh: ## Refresh Gradle dependencies
	$(GRADLE) --refresh-dependencies dependencies

# ===== Release Targets =====

.PHONY: version
version: ## Display project version
	$(GRADLE) properties | grep "^version:" | cut -d' ' -f2

.PHONY: release-dry
release-dry: clean build test check ## Dry run of release process
	@echo "Release dry run completed successfully!"

# ===== Development Helpers =====

.PHONY: watch
watch: ## Watch for changes and rebuild
	$(GRADLE) build --continuous

.PHONY: run
run: build ## Run the application
	@echo "Running the application..."
	@java -jar application/build/libs/application-*.jar

.PHONY: clean-cache
clean-cache: ## Clean Gradle caches
	rm -rf ~/.gradle/caches/
	$(GRADLE) clean build --no-build-cache



# ---- Gradle "panic button" ---------------------------------------------------
#SHELL := /bin/bash

# Usage
# Normal deep clean:
#    make zap
# Preview what would be deleted (no changes):
#    make zap DRY_RUN=1
# “Nuke from orbit” (forces full re-download of dependencies and wrapper):
#    make zap AGGRESSIVE=1
# Also purge Kotlin/Native toolchains (ONLY if you actually use K/N):
#    make zap INCLUDE_KOTLIN_NATIVE=1

GRADLEW            ?= ./gradlew
GRADLE_USER_HOME   ?= $(HOME)/.gradle
XDG_CACHE_HOME     ?= $(HOME)/.cache

# Flags you can pass: DRY_RUN=1 to preview, AGGRESSIVE=1 to nuke deps/wrapper,
# INCLUDE_KOTLIN_NATIVE=1 to also remove ~/.konan (Kotlin/Native toolchains).

# runs your normal clean
# stops Gradle daemons
# clears the project build cache
# removes project build/ and .gradle/ folders (including in included/composite builds)
# prunes user-level Gradle caches and daemon data
# has an AGGRESSIVE=1 mode to nuke dependency caches and wrapper dists
# supports a DRY_RUN=1 preview

DRY_RUN            ?= 0
AGGRESSIVE         ?= 0
INCLUDE_KOTLIN_NATIVE ?= 0

.PHONY: zap
zap: clean
	@echo ">> Stopping Gradle daemons"
	-$(GRADLEW) --stop || true

	@echo ">> Clearing project build cache (if enabled)"
	-$(GRADLEW) -q cleanBuildCache || true

	@echo ">> Removing project-local .gradle and build/ directories"
	@rm_cmd="rm -rf"; [ "$(DRY_RUN)" = "1" ] && rm_cmd="echo rm -rf"; \
	$$rm_cmd "$(CURDIR)/.gradle"; \
	find . -type d -name ".gradle" -prune -exec $$rm_cmd {} +; \
	find . -type d -name "build"   -prune -exec $$rm_cmd {} +;

	@echo ">> Pruning user-level Gradle caches (safe subset)"
	@rm_cmd="rm -rf"; [ "$(DRY_RUN)" = "1" ] && rm_cmd="echo rm -rf"; \
	case "$(GRADLE_USER_HOME)" in "$(HOME)"/*) ;; *) \
	  echo "Safety: GRADLE_USER_HOME ($(GRADLE_USER_HOME)) not under HOME ($(HOME)); aborting."; exit 1; \
	esac; \
	$$rm_cmd "$(GRADLE_USER_HOME)/daemon"; \
	$$rm_cmd "$(GRADLE_USER_HOME)/notifications"; \
	$$rm_cmd "$(GRADLE_USER_HOME)/caches/build-cache-"* 2>/dev/null || true; \
	$$rm_cmd "$(GRADLE_USER_HOME)/vcs-"*              2>/dev/null || true; \
	# Kotlin compiler/plugin shards under Gradle caches:
	$$rm_cmd "$(GRADLE_USER_HOME)/caches/"*/kotlin 2>/dev/null || true; \
	# XDG cache (some environments route Gradle here):
	[ -d "$(XDG_CACHE_HOME)/gradle" ] && $$rm_cmd "$(XDG_CACHE_HOME)/gradle" || true

	@if [ "$(AGGRESSIVE)" = "1" ]; then \
	  echo ">> AGGRESSIVE=1: nuking dependency caches & wrapper dists (will force full re-downloads)"; \
	  rm_cmd="rm -rf"; [ "$(DRY_RUN)" = "1" ] && rm_cmd="echo rm -rf"; \
	  $$rm_cmd "$(GRADLE_USER_HOME)/caches/modules-2"; \
	  $$rm_cmd "$(GRADLE_USER_HOME)/caches/jars-"*        2>/dev/null || true; \
	  $$rm_cmd "$(GRADLE_USER_HOME)/caches/transforms-"*  2>/dev/null || true; \
	  $$rm_cmd "$(GRADLE_USER_HOME)/wrapper/dists"; \
	fi

	@if [ "$(INCLUDE_KOTLIN_NATIVE)" = "1" ]; then \
	  echo ">> INCLUDE_KOTLIN_NATIVE=1: removing ~/.konan (Kotlin/Native toolchains & caches)"; \
	  rm_cmd="rm -rf"; [ "$(DRY_RUN)" = "1" ] && rm_cmd="echo rm -rf"; \
	  $$rm_cmd "$(HOME)/.konan"; \
	fi

	@echo ">> Gradle zap complete."

# ===== Quick Test Shortcuts =====

.PHONY: quick
quick: compile test-unit ## Quick build and unit test

.PHONY: full
full: clean build test check ## Full build, test, and validation

# ===== Run Targets =====

.PHONY: run-async
run-async: build ## Run the application in async mode
	@echo "Running in async mode..."
	@cd bootstrap && java -cp build/libs/bootstrap-*.jar com.abitofhelp.hybrid.bootstrap.AsyncEntryPointKt

.PHONY: run-help
run-help: build ## Show application help
	@cd bootstrap && java -cp build/libs/bootstrap-*.jar com.abitofhelp.hybrid.bootstrap.EntryPointKt --help

# Example usage:
# make build              # Build the project
# make test               # Run all tests
# make test-coverage      # Run tests with coverage reports
# make format             # Format code
# make check              # Run all code quality checks
# make diagrams           # Generate architecture diagrams
# make run                # Run the application
# make run-async          # Run in async mode
