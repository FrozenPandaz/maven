# Phase Target Refactoring Plan

## Current Implementation Analysis

### Current Structure
The `NxTargetFactory.kt` currently creates:
1. **Individual goal targets**: Each plugin execution goal gets its own target (e.g., `maven-compiler:compile@default-compile`)
2. **Phase targets**: Created as `nx:noop` targets that just depend on goal targets
3. **Test targets**: Atomized test targets for individual test classes

### Key Observations
- Goals are mapped to phases via `execution.phase` or `mojoDescriptor.phase`
- Phase targets currently use `nx:noop` executor and depend on individual goal targets
- Goals within a phase are executed as separate targets
- Build state management (`nx:apply` and `nx:record`) is added to individual goal commands

### Dependency Structure
- Phases depend on:
  - Previous phase in lifecycle (if index > 1)
  - `^phase` (parent project's same phase)
  - `^install` (if install phase exists)
  - All goal targets assigned to that phase
- Goal targets depend on:
  - Same dependencies as their phase (minus the goal dependencies)

## Proposed Refactoring

### New Structure
1. **Remove individual goal targets** - No more `goalPrefix:goal@executionId` targets
2. **Create phase targets with actual commands** - Phase targets will execute all goals in that phase
3. **Skip empty phases** - Don't create targets for phases with no goals
4. **Maintain proper dependencies** - Phase targets still depend on previous phases and parent phases

### Phase Target Command Structure
```
./mvnw nx:apply goal1 goal2 goal3 nx:record -pl groupId:artifactId -N
```

### Implementation Changes

1. **Collect goals by phase**:
   - Map all plugin executions to their phases
   - Group goals by phase
   - Skip phases with no goals

2. **Create phase targets with commands**:
   - Build command with `nx:apply` at start
   - Add all goals for that phase
   - Add `nx:record` at end
   - Include `-pl` and `-N` flags

3. **Maintain dependencies**:
   - Phase depends on previous phase
   - Phase depends on `^phase` (parent)
   - Phase depends on `^install` if applicable
   - NO dependencies on individual goals (since they don't exist)

4. **Remove**:
   - `createGoalTarget()` method
   - Individual goal target creation loop
   - Goal target groups

5. **Modify**:
   - `createPhaseTarget()` to build actual command with goals
   - Phase target to use `nx:run-commands` instead of `nx:noop`

### Dependency Handling
- Keep phase-to-phase dependencies intact
- Remove all goal-level dependencies
- Ensure phases with goals properly depend on their prerequisites

### Edge Cases to Handle
1. Goals with no explicit phase (use mojo descriptor phase)
2. Phase normalization (Maven 3 to Maven 4 compatibility)
3. Special handling for certain plugins (e.g., build-helper:attach-artifact)
4. Test atomization remains separate