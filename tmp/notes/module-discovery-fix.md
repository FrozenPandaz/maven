# Module Discovery Logic Fix

## Problem Solved
Fixed StackOverflowError when processing Apache Maven codebase by replacing `session.allProjects` with Maven's proper module discovery logic.

## Root Cause
- Maven's `session.allProjects` discovered all 1,888 pom.xml files in the codebase
- This included test resource projects in `/src/test/resources/` and `/its/core-it-suite/` 
- Processing this many projects caused StackOverflowError in regex/string processing

## Solution
Implemented `collectProjectsFromModules()` that follows Maven's own module discovery:
- Uses `project.modules` to find declared modules only
- Recursively processes module hierarchy 
- Only includes actual Maven modules, not test resources
- Reduced from 1,888 projects to 39 actual modules

## Implementation Details
- Modified `NxWorkspaceGraphMojo.kt`
- Added `collectProjectsFromModules()` function
- Added `findModuleProject()` helper for module lookup
- Uses Maven's canonical path resolution for accurate matching

## Results
- ✅ No more StackOverflowError
- ✅ Processes 39 projects instead of 1,888
- ✅ Test atomization still works (18 test classes discovered)
- ✅ Follows Maven's standard behavior
- ✅ Automatically excludes test resources (no manual filtering needed)

## Test Results
- Minimal project: Works perfectly
- Apache Maven project: Works perfectly (39/39 modules processed)
- Generated 1.97MB workspace graph file successfully