# Maven Reactor Dependency Warnings

## Issue
When running the Nx Maven plugin in Quarkus projects, we see warnings like:
```
[WARNING] The following dependencies could not be resolved at this point of the build but seem to be part of the reactor:
```

## Explanation
- The "reactor" is Maven's term for all modules being built together in a multi-module project
- This warning appears when Maven encounters dependencies between modules during the build
- At the current build phase, the dependent modules haven't been compiled yet
- Maven knows these dependencies will be resolved later in the same build session

## Analogy
Like an assembly line where part A needs part B, but part B is still being manufactured elsewhere on the same line.

## Resolution
- These warnings are typically harmless
- Maven will build modules in correct dependency order
- Dependencies get resolved automatically as the build progresses
- No action needed unless build actually fails

## Impact on Nx Plugin
- Should not affect our plugin functionality
- May want to consider filtering these warnings in output
- Could investigate if our dependency analysis affects Maven's reactor ordering

## Solution Implemented
Changed `requiresDependencyResolution` from `ResolutionScope.COMPILE_PLUS_RUNTIME` to `ResolutionScope.NONE` in both analyzer mojos:
- `NxProjectAnalyzerMojo.kt` (line 17)
- `NxProjectAnalyzerSingleMojo.kt` (line 22)

**Why this works:**
- Plugin only needs dependency metadata (groupId, artifactId, version) from `project.dependencies`
- Doesn't need resolved artifacts or JARs
- `ResolutionScope.NONE` prevents premature resolution of reactor dependencies
- Dependency metadata remains available through Maven project model

**Result:** Eliminates reactor dependency warnings while preserving all required functionality.

## Issue Found with createDependencies

After fixing the reactor warnings, discovered that `createDependencies` in the Nx plugin is not working correctly:

1. **Problem**: All dependency arrays in project-graph.json are empty (`[]`) despite having proper `dependsOn` in targets
2. **Root Cause**: The dependency plugin can't find the cached Maven analysis data 
3. **Cache Path Issue**: Fixed cache lookup to use `workspaceRoot/.nx/workspace-data/` instead of global cache
4. **Still Not Working**: Dependencies plugin not extracting relationships from the `dependsOn` arrays properly

**Next Steps:**
- Debug why `createDependencies` isn't parsing the Maven analysis data structure correctly
- The reactor warnings fix is working ✅
- Dependencies are properly listed in target `dependsOn` arrays ✅  
- But project graph dependencies section remains empty ❌