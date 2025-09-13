 # Maven Caching Research Notes

## Overview
Research conducted to understand how different tools handle Maven caching, what they hash, and how they determine cacheability.

## Key Finding: No @Cacheable Annotation in Maven
- **Maven does NOT have a @Cacheable annotation for mojos/goals**
- The @Mojo annotation lacks any caching-related attributes
- Caching is handled through external tools and configuration, not annotations

## Build Caching Solutions Comparison

### 1. Maven Build Cache Extension (Apache, 2023+)

**Configuration:**
- Uses `maven-build-cache-config.xml` in `.mvn/` directory
- XML-based configuration with sections for:
  - `configuration`: Global settings, hash algorithm, remote cache
  - `input`: File patterns to include in cache key
  - `output`: What outputs to cache
  - `executionControl`: Plugin execution rules

**Cache Key Components:**
```
Cache Key = Hash(
  Goal Implementation + Version +
  Normalized Input Files +
  Runtime Classpath +
  Relevant Configuration +
  JVM Version +
  Platform Properties
)
```

**Adoption Status:**
- Released in Maven 3.9.0 (June 2023)
- Limited production adoption
- Known users: Meltwater
- Considered experimental/opt-in

**Limitations:**
- Requires Maven >= 3.9.0
- Incomplete state restoration
- Complex configuration required
- "Delivered as-is" disclaimer
- Bug with incremental execution (fixed in 1.2.0)

### 2. Gradle Enterprise/Develocity (Commercial, 2019+)

**Features:**
- Enterprise-grade solution
- Remote cache sharing across teams
- Build scans and analytics
- Works with Maven, Gradle, and sbt
- Professional support

**Adoption:**
- Much wider adoption than Maven's native solution
- Used by major enterprises
- Reports 80%+ build time reductions
- Production-ready since 2019

**Configuration:**
- XML configuration in `.mvn/develocity.xml`
- Programmatic configuration support (v1.15+)
- Better tooling and monitoring

### 3. Nx Approach

**Strategy:**
- Configured via `cacheable` property in target configuration
- Analyzes file dependencies automatically
- Supports custom hasher implementations
- Named inputs for reusable configurations

**What Nx Hashes:**
- Source files matching input patterns
- Dependencies' output hashes
- Runtime inputs (environment variables)
- Named inputs (reusable input configurations)

## Parameter Analysis Strategies

### Input Detection Patterns
Common patterns across all tools:
- Source directories: `src/main/java`, `src/test/java`
- Resources: `src/main/resources`
- Classpath elements
- Maven expressions: `${project.build.sourceDirectory}`
- Parameter metadata analysis (name, type, description, expression)

### Output Detection Patterns
- Target directories: `target/`, `build/`
- Generated sources: `target/generated-sources`
- Archive files: JAR, WAR, EAR
- Reports: `target/site`, `target/reports`

### Volatile Data Handling
Files/patterns to exclude from cache keys:
- Version control: `.git/`, `.svn/`, `.hg/`
- IDE files: `.idea/`, `.vscode/`, `*.iml`
- Logs: `*.log`
- Temporary files: `*~`, `*.swp`
- OS files: `.DS_Store`
- Timestamps and build numbers (normalize or exclude)

## Cacheability Detection Methods

### Non-Cacheable Indicators
1. **Goal patterns:**
   - deploy, install, release
   - exec, run, start, stop
   - clean

2. **Network operations:**
   - Parameters with: url, server, host, port, repository
   - Upload/download operations
   - Remote publishing

3. **Time-sensitive operations:**
   - Timestamp generation
   - Build number increments
   - Git commit info

4. **Mojo characteristics:**
   - `aggregator = true` (cross-project effects)
   - `threadSafe = false` (non-deterministic)
   - `requiresOnline = true` (network dependency)

## Best Practices from Research

### 1. Input Normalization
- Make paths relative to project base
- Sort inputs deterministically
- Use canonical paths
- Exclude volatile files

### 2. Classpath Handling
- Critical for compile/test goals
- Differentiate API vs implementation dependencies
- Hash JAR files by size/name or contents
- Include JVM version

### 3. Configuration Hashing
- Plugin configuration from POM
- Relevant Maven properties
- System properties affecting builds
- Environment variables

### 4. Cache Key Generation
- Use non-cryptographic hash (XX) for speed
- Include goal implementation and version
- Hash file contents, not timestamps
- Ensure deterministic ordering

## Recommendations for Nx Maven Plugin

### Primary Strategy
1. **Use Nx's native caching** rather than Maven Build Cache Extension
2. **Continue with parameter analysis approach** for input/output detection
3. **Support both Nx and Maven caching** but default to Nx
4. **Wait for Maven Build Cache Extension to mature** before deep integration

### Implementation Focus
1. **CacheKey Generation:**
   - Implement deterministic hashing
   - Include all relevant inputs
   - Normalize paths and exclude volatile data

2. **Parameter Analysis (Current Approach):**
   - Analyze parameter names, types, expressions
   - Check descriptions for hints
   - Use isEditable, isRequired properties
   - Pattern matching for known plugins

3. **Cacheability Detection:**
   - Check threadSafe property
   - Identify side-effect goals
   - Detect network operations
   - Flag time-sensitive operations

## Why Our Approach is Valid

1. **Industry Standard:** All tools use configuration + runtime analysis
2. **No Native Support:** Maven doesn't provide caching annotations
3. **Proven Patterns:** Similar to Gradle Enterprise and Maven Build Cache Extension
4. **Flexibility:** Can adapt without modifying Maven plugins

## Key Insights

1. **Maven's Late Entry:** Caching only available since 2023 (vs Gradle 2017)
2. **Configuration Over Code:** All solutions use XML/config, not annotations
3. **Limited Adoption:** Even Apache's solution has limited production use
4. **Complex Challenge:** Maven's architecture makes caching difficult
5. **Nx Advantage:** Can provide better caching than native Maven solutions

## Conclusion

The research validates our current approach of analyzing Maven parameters and expressions to determine inputs/outputs. Since Maven lacks native caching annotations and even Apache's own solution has limited adoption, building intelligent caching into the Nx Maven plugin is the right strategy. Our parameter analysis combined with Nx's proven caching infrastructure will likely provide better results than relying on Maven's experimental caching extension.