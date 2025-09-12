# Maven Plugin CI Fix

## Problem
CI was failing when building the Maven analyzer plugin with error: "Could not find the selected project in the reactor: packages/maven/analyzer-plugin"

## Root Cause
The analyzer plugin's pom.xml had incorrect Maven dependency versions:
- Used `${maven.version}` instead of `${project.version}` 
- Used outdated `maven-project` dependency (2.2.1) incompatible with Maven 4

## Solution
Fixed the pom.xml by:
1. Replaced all occurrences of `${maven.version}` with `${project.version}` to reference the parent version (4.1.0-SNAPSHOT)
2. Replaced the deprecated `maven-project` dependency with `maven-embedder` 

## Files Modified
- `packages/maven/analyzer-plugin/pom.xml`

## Testing
- Local build now succeeds with warnings but no errors
- Plugin compiles and packages correctly
- Ready for CI to pass