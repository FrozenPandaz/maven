# Phase Dependency Fix - Complete

## Issue
Phase dependencies were not correctly handling lifecycle order when phases were skipped due to having no goals.

## Problem
The original logic only checked the immediately previous phase in the lifecycle:
```kotlin
if (index > 1) {
    val previousPhase = lifecycle.phases[index - 1]
    if (phaseGoals[previousPhase]?.isNotEmpty() == true) {
        target.dependsOn?.add(previousPhase)
    }
}
```

This would break the dependency chain if the immediately previous phase had no goals and was skipped.

## Solution
Modified the dependency logic to look backward through the lifecycle to find the most recent phase with goals:

```kotlin
if (index > 0) {
    // Look backward through the lifecycle to find the most recent phase with goals
    for (prevIndex in (index - 1) downTo 0) {
        val previousPhase = lifecycle.phases[prevIndex]
        if (phaseGoals[previousPhase]?.isNotEmpty() == true) {
            target.dependsOn?.add(previousPhase)
            phaseDependsOn[phase]?.add(previousPhase)
            log.info("Phase '$phase' depends on previous phase with goals: '$previousPhase'")
            break // Only depend on the most recent phase with goals
        }
    }
}
```

## Results

### Correct Dependency Chain Example:
```
validate (5 goals)
  ↓
initialize (1 goal) → depends on 'validate'
  ↓
after:sources (2 goals) → depends on 'initialize' (skipping empty 'sources')
  ↓
resources (1 goal) → depends on 'after:sources'
  ↓
after:resources (1 goal) → depends on 'resources'
  ↓
compile (1 goal) → depends on 'after:resources'
```

### Key Behaviors:
1. **Maintains lifecycle order**: Dependencies follow the correct Maven lifecycle sequence
2. **Bridges empty phases**: Dependencies skip over phases with no goals
3. **Preserves execution order**: Ensures phases execute in the correct sequence
4. **Single dependency chain**: Each phase depends only on the most recent previous phase with goals

## Verification
Testing shows the correct log output:
- `Phase 'initialize' depends on previous phase with goals: 'validate'`
- `Phase 'after:sources' depends on previous phase with goals: 'initialize'`
- `Phase 'resources' depends on previous phase with goals: 'after:sources'`

The dependency chain correctly bridges over empty phases while maintaining Maven lifecycle semantics.