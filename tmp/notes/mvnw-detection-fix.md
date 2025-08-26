# Fixed Maven Wrapper Detection Issue

## Problem
The Maven plugin was using `mvnd` (Maven Daemon) instead of `mvnw` (Maven Wrapper) when both were available, due to incorrect priority order in the detection logic.

## Root Cause  
The detection function was checking for `mvnd` first, then falling back to `mvnw`. This caused issues because:
1. `mvnd` doesn't work well in parallel with itself
2. Project should prefer its own wrapper (`mvnw`) over system-wide tools

## Original Priority Order (Problematic)
1. `mvnd` (Maven Daemon) - system-wide
2. `mvnw` (Maven Wrapper) - project-specific  
3. `mvn` (Regular Maven) - system-wide

## Fixed Priority Order
1. `mvnw` (Maven Wrapper) - project-specific
2. `mvn` (Regular Maven) - system-wide

**Note:** Removed `mvnd` entirely due to parallelization issues.

## Changes Made

### 1. Updated TypeScript Source
File: `packages/maven/src/plugins/maven-analyzer.ts`
- Renamed function from `detectMavenWrapper()` to `detectMavenExecutable()`
- Changed detection order to check `mvnw` first, then fallback to `mvn`
- Removed `mvnd` detection completely

### 2. Updated Compiled JavaScript
File: `/home/jason/projects/triage/java/quarkus/.nx/installation/node_modules/@nx/maven/dist/plugins/maven-analyzer.js`
- Applied the same logic changes to the compiled JavaScript that was actually being executed

## Verification
Before fix:
```
[Maven Analyzer] Found mvnd system-wide, using Maven Daemon
[Maven Analyzer] Maven command: mvnd dev.nx.maven:nx-maven-analyzer-plugin:0.0.1-SNAPSHOT:analyze
```

After fix:
```
[Maven Analyzer] Found Maven wrapper, using: ./mvnw  
[Maven Analyzer] Maven command: ./mvnw dev.nx.maven:nx-maven-analyzer-plugin:0.0.1-SNAPSHOT:analyze
```

## Impact
- Projects with `mvnw` will now correctly use their wrapper instead of system `mvnd`
- Eliminates parallelization issues caused by multiple `mvnd` processes
- Maintains compatibility with projects without wrappers (fallback to `mvn`)