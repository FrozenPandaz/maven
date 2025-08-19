# Dependency Resolution Logic Refactoring

## Current State
The dependency resolution logic is currently split between:
1. **Analyzer Mojo (Kotlin)**: Extracts basic project data and dependencies
2. **Plugin TypeScript**: Complex dependency resolution and phase fallback logic

## Logic That Can Be Moved to Analyzer Mojo

### 1. Coordinates to Project Name Mapping
Currently in `plugin.ts:125-134`:
- Creates a map of Maven coordinates (`groupId:artifactId`) to qualified project names
- This is foundational data that could be computed once in the analyzer

### 2. Dependency Resolution with Phase Fallback
Currently in `plugin.ts:150-213`:
- `getBestDependencyPhase()` function with Maven lifecycle phase ordering
- Phase availability checking for dependency projects
- Fallback logic when requested phases aren't available
- Parent POM dependency handling

### 3. Maven Lifecycle Phase Ordering
Currently hardcoded in `plugin.ts:156-163`:
- Complete Maven lifecycle phase sequence
- Used for intelligent phase fallback

## Benefits of Moving to Analyzer Mojo

1. **Performance**: Complex calculations done once during analysis vs every plugin run
2. **Accuracy**: Direct access to Maven's lifecycle resolution
3. **Maintainability**: Logic closer to Maven's native capabilities
4. **Consistency**: Single source of truth for dependency relationships

## What Would Stay in Plugin
- Basic project configuration creation
- Target generation for Nx
- Nx-specific formatting and output

## Implementation Approach
The analyzer mojo could output enhanced dependency information including:
- Resolved dependency relationships with best available phases
- Pre-computed `dependsOn` arrays for each phase
- Coordinates-to-project-name mapping table