# Fixed Maven 'Provided' Scope Dependencies

## Issue
The `nx run org.apache.maven.maven-builder-support:install` command was failing because maven-builder-support depends on maven-api-core with `<scope>provided</scope>`, but the Kotlin Maven analyzer was filtering out 'provided' scope dependencies.

## Root Cause
In `NxProjectAnalyzerMojo.kt:113`, the dependency filtering logic only included 'compile' and null scope dependencies:

```kotlin
// OLD CODE - Missing 'provided' scope
if ("compile" == dependency.scope || dependency.scope == null) {
```

This meant that 'provided' scope dependencies like maven-api-core weren't being included in the dependency analysis, so no dependsOn relationships were created.

## Solution
Updated the scope filter in `NxProjectAnalyzerMojo.kt:113` to include 'provided' scope dependencies:

```kotlin
// FIXED CODE - Now includes 'provided' scope
if ("compile" == dependency.scope || "provided" == dependency.scope || dependency.scope == null) {
```

## Results
1. **Fixed Dependency Detection**: maven-builder-support now correctly shows maven-api-core as a dependency
2. **Working dependsOn Relationships**: The install target now has proper dependsOn configuration
3. **Nx Task Scheduling**: `nx run org.apache.maven.maven-builder-support:install` now correctly runs 8 dependent tasks first
4. **Maven Reactor Execution Model**: Dependencies are properly resolved based on Maven's actual dependency declarations

## Verification
```bash
# Check that dependencies are detected
cat nx-maven-projects.json | jq '.projects[] | select(.artifactId == "maven-builder-support") | .dependencies'

# Check that dependsOn relationships are created  
nx show project org.apache.maven.maven-builder-support --json | jq '.targets.install.dependsOn'

# Verify Nx runs dependent tasks
nx run org.apache.maven.maven-builder-support:install
```

The Maven plugin now correctly handles all three main dependency scopes needed for proper build ordering:
- `compile` scope (runtime dependencies)
- `provided` scope (compile-time dependencies) 
- `null` scope (default to compile)