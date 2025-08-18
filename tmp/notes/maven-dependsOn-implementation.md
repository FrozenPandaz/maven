# Maven dependsOn Implementation - Maven Reactor Execution Model

## Overview
Successfully implemented `dependsOn` relationships for Maven phase targets, mirroring Maven's reactor build execution model where dependent projects must complete phases before their dependents.

## Implementation Details

### Core Logic
- **Two-pass approach**: First create coordinate-to-project-name mapping, then generate targets with dependencies
- **Helper function**: `createDependsOnForPhase(phaseName)` generates dependsOn arrays for each phase
- **Target enhancement**: Each phase target includes dependsOn relationships based on Maven dependencies

### Example Result
The `maven-core:compile` target now has 22 dependsOn relationships:
```json
{
  "executor": "nx:run-commands",
  "options": {
    "command": "mvn compile -pl org.apache.maven:maven-core",
    "cwd": "{workspaceRoot}"
  },
  "dependsOn": [
    "org.apache.maven.maven-api-annotations:compile",
    "org.apache.maven.maven-api-core:compile",
    "org.apache.maven.maven-api-di:compile",
    // ... 19 more dependencies
  ]
}
```

## Maven Execution Model Alignment

### Before (Incorrect)
- All Maven projects could run phases independently
- No coordination between dependent projects
- Could lead to build failures due to missing dependencies

### After (Correct)
- **Compile dependencies**: `maven-core:compile` waits for all dependency compiles
- **Test dependencies**: `maven-core:test` waits for all dependency tests  
- **Package dependencies**: `maven-core:package` waits for all dependency packages
- **All phases supported**: validate, compile, test, package, install, deploy, etc.

## Benefits

### 1. **Correct Build Order**
Nx now respects Maven's reactor build order automatically. When you run:
```bash
nx run org.apache.maven.maven-core:compile
```
Nx will ensure all 22 Maven dependencies compile first.

### 2. **Parallel Execution**
While respecting dependencies, Nx can still run independent phases in parallel:
- `maven-api-annotations:compile` and `maven-api-di:compile` can run simultaneously
- `maven-core:compile` waits for both to complete

### 3. **Incremental Builds**
Combined with Nx caching, only changed projects and their dependents rebuild:
- Change in `maven-api-core` triggers rebuild of `maven-core` 
- Unchanged dependencies use cached results

### 4. **Consistent with Maven**
Mirrors Maven's own reactor execution model:
- Same dependency resolution logic
- Same phase ordering
- Compatible with existing Maven workflows

## Technical Implementation

### Phase Target Generation
```typescript
// Create dependsOn relationships for Maven dependencies
const createDependsOnForPhase = (phaseName: string): string[] => {
  const dependsOn: string[] = [];
  if (projectDeps && Array.isArray(projectDeps)) {
    for (const dep of projectDeps) {
      const depCoordinates = `${dep.groupId}:${dep.artifactId}`;
      const depProjectName = coordinatesToProjectName.get(depCoordinates);
      if (depProjectName && depProjectName !== `${groupId}.${artifactId}`) {
        dependsOn.push(`${depProjectName}:${phaseName}`);
      }
    }
  }
  return dependsOn;
};
```

### Target Creation with Dependencies
```typescript
allPhases.forEach(phase => {
  const phaseName = phase as string;
  const dependsOn = createDependsOnForPhase(phaseName);
  const target: TargetConfiguration = {
    executor: 'nx:run-commands',
    options: { 
      command: `mvn ${phaseName} -pl ${qualifiedName}`,
      cwd: '{workspaceRoot}'
    }
  };
  
  // Add dependsOn only if there are dependencies
  if (dependsOn.length > 0) {
    target.dependsOn = dependsOn;
  }
  
  targets[phaseName] = target;
});
```

## Result
Perfect integration of Maven's reactor execution model with Nx's task scheduling, enabling efficient and correct parallel builds while maintaining Maven compatibility.