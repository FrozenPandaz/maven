# Nx Maven Plugin - Design Document

## Overview

The Nx Maven Plugin integrates Maven projects with Nx's task scheduling and caching system. Instead of duplicating Maven's sophisticated build logic, this plugin leverages Maven's native capabilities while providing Nx's orchestration benefits.

## Architecture

### Core Components

#### 1. **MavenBuildCacheIntegration** 
The central component that bridges Maven's Build Cache Extension with Nx target configuration.

**Key Features:**
- **Native Maven Integration**: Uses Maven Build Cache Extension APIs via Plexus/Sisu container lookup
- **Input Analysis**: Extracts Maven's own input calculations for sophisticated cache key generation
- **Cacheability Decisions**: Leverages Maven's logic instead of reimplementing it
- **Graceful Degradation**: Falls back to no caching when Build Cache Extension unavailable

**API Access Pattern:**
```kotlin
// Access Maven Build Cache Extension components
val container = session.container
val cacheController = container.lookup(Class.forName("org.apache.maven.buildcache.CacheController"))
val inputCalculator = container.lookup(Class.forName("org.apache.maven.buildcache.DefaultProjectInputCalculator"))
```

#### 2. **NxProjectAnalyzerMojo**
Maven plugin entry point that analyzes reactor projects and generates Nx configuration.

**Responsibilities:**
- Discovers all Maven projects in the reactor
- Coordinates with lifecycle analyzers and configuration generators
- Outputs Nx-compatible project graph data

#### 3. **NxProjectConfigurationGenerator**
Converts Maven project metadata into Nx target definitions.

**Target Generation:**
- Maps Maven lifecycle phases to Nx targets
- Applies cacheability decisions from MavenBuildCacheIntegration
- Configures parallelism based on Maven phase characteristics
- Handles dependency resolution between projects

#### 4. **MavenLifecycleAnalyzer**
Analyzes Maven project lifecycle configuration and plugin bindings.

#### 5. **MavenDependencyResolver**
Resolves inter-project dependencies within the Maven reactor.

## Design Principles

### 1. **Leverage Maven's Native Logic**
Instead of reimplementing Maven's caching and input analysis:
- Use Maven Build Cache Extension for cacheability decisions
- Extract Maven's input calculations for cache keys
- Respect Maven's plugin configuration and behavior

### 2. **No Fallback to Hardcoded Rules**
- Only use Maven's own logic for cache decisions
- If Maven Build Cache Extension unavailable, disable caching rather than guess
- Avoids duplication and maintenance of Maven's complex logic

### 3. **Nx Orchestration Focus**
- Provide task scheduling and dependency management
- Enable cross-technology coordination (Maven + other tools)
- Leverage Nx's distributed execution capabilities

## Cache Integration Strategy

### Maven Build Cache Extension Integration

The plugin integrates with Maven's official Build Cache Extension (`org.apache.maven.extensions:maven-build-cache-extension:1.2.0`) which provides:

1. **Sophisticated Input Analysis**
   - Source file changes
   - Plugin parameter analysis
   - Dependency and classpath tracking
   - Project model (pom.xml) change detection

2. **Proven Cacheability Logic**
   - Years of development and refinement
   - Handles complex edge cases
   - Plugin-specific configuration support

3. **Remote Caching Support**
   - Compatible with existing Maven cache infrastructure
   - Supports distributed teams and CI/CD

### Integration Flow

```
Maven Execution Request
        ↓
Maven Build Cache Extension Analysis
        ↓
Nx Plugin extracts:
  - Cacheability decision
  - Input file patterns
  - Cache key components
        ↓
Generate Nx Target Configuration:
  - cache: true/false
  - inputs: ["pattern1", "pattern2"]
  - outputs: ["{projectRoot}/target"]
```

### Input Pattern Conversion

Maven paths are converted to Nx input patterns:

```kotlin
// Maven absolute path → Nx pattern
"/project/src/main/java" → "{projectRoot}/src/main/java"

// Maven relative path → Nx pattern  
"src/main/java" → "{projectRoot}/src/main/java"

// POM file → Nx pattern
"pom.xml" → "{projectRoot}/pom.xml"
```

## Implementation Status

### ✅ Completed
- Maven Build Cache Extension dependency integration
- Plexus/Sisu container access for extension components
- Basic input extraction framework
- Cacheability decision integration
- Compilation and basic testing

### 🚧 In Progress
- Full Maven input analysis extraction
- Real-world testing with Build Cache Extension enabled
- Output pattern configuration

### 📋 TODO
- Integration with actual mojo execution analysis (vs. phase-level)
- Support for multi-module project dependencies
- Performance optimization for large reactor builds
- Documentation and examples

## Benefits

### For Developers
- **Familiar Maven Workflow**: No changes to existing Maven usage
- **Improved Build Performance**: Nx caching with Maven's sophisticated input analysis
- **Cross-Technology Support**: Integrate Maven with other Nx-supported tools

### For Organizations
- **Reduced Duplication**: Leverage existing Maven infrastructure
- **Proven Reliability**: Use Maven's battle-tested caching logic
- **Gradual Adoption**: Works alongside existing Maven builds

## Dependencies

### Required
- Maven 3.9+ (for Build Cache Extension support)
- Nx workspace
- org.apache.maven.extensions:maven-build-cache-extension:1.2.0 (optional but recommended)

### Development
- Kotlin 1.9.22
- Jackson (JSON processing)
- Maven Plugin API

## Future Enhancements

1. **Enhanced Target Generation**: Support for custom Maven plugin goals as Nx targets
2. **Build Insights**: Integration with Nx build analytics 
3. **Remote Execution**: Leverage Nx's distributed execution with Maven builds
4. **IDE Integration**: Support for Nx IDE features with Maven projects

This design ensures we're building on proven Maven foundations while providing Nx's orchestration benefits, rather than reinventing Maven's sophisticated build logic.