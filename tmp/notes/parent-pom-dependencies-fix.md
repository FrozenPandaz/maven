# Fixed Parent POM Dependencies

## Issue
Maven projects with parent POM relationships were failing to install because the parent POMs weren't being installed first. For example, `maven-api-xml` failed because it couldn't resolve its parent `maven-api` POM.

## Root Cause
The Maven plugin was only capturing `<dependencies>` elements but not `<parent>` relationships. In Maven, parent POMs must be installed before child projects can be built, as Maven needs the parent POM to resolve the project's full dependency chain.

## Solution

### 1. Enhanced Kotlin Analyzer
Added parent POM detection in `NxProjectAnalyzerMojo.kt`:

```kotlin
// Parent POM relationship
val parent = mavenProject.parent
if (parent != null) {
    val parentNode = objectMapper.createObjectNode()
    parentNode.put("groupId", parent.groupId)
    parentNode.put("artifactId", parent.artifactId)
    parentNode.put("version", parent.version)
    projectNode.put("parent", parentNode)
}
```

### 2. Updated TypeScript Plugin
Modified `createDependsOnForPhase()` function in `plugin.ts` to include parent dependencies:

```typescript
// Add parent dependency first (parent POMs must be installed before children)
if (parent) {
  const parentCoordinates = `${parent.groupId}:${parent.artifactId}`;
  const parentProjectName = coordinatesToProjectName.get(parentCoordinates);
  if (parentProjectName && parentProjectName !== `${groupId}.${artifactId}`) {
    dependsOn.push(`${parentProjectName}:${phaseName}`);
  }
}
```

## Results

### Before Fix
```bash
nx run org.apache.maven.maven-api-xml:install
# Only ran: maven-api-annotations:install 
# Failed: Could not find artifact org.apache.maven:maven-api:pom:4.1.0-SNAPSHOT
```

### After Fix
```bash
nx show project org.apache.maven.maven-api-xml --json | jq '.targets.install.dependsOn'
# ["org.apache.maven.maven-api:install", "org.apache.maven.maven-api-annotations:install"]

nx run org.apache.maven.maven-api-xml:install
# Correctly runs: maven-api:install → maven-api-annotations:install → maven-api-xml:install
```

## Parent Dependency Chain
The fix properly handles the complete parent hierarchy:
- `maven-api-xml` → `maven-api` → `maven` (root)
- `maven-api-annotations` → `maven-api` → `maven` (root)
- `maven-builder-support` → `maven-compat` → `maven` (root)

## Verification
```bash
# Check parent relationships are detected
cat nx-maven-projects.json | jq '.projects[] | select(.artifactId == "maven-api-xml") | .parent'

# Verify dependsOn includes both parent and dependencies
nx show project org.apache.maven.maven-api-xml --json | jq '.targets.install.dependsOn'
```

This fix ensures that Maven's parent-child project relationships are properly respected in Nx's task execution order, eliminating dependency resolution failures.