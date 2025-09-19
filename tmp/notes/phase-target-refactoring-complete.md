# Phase Target Refactoring - Complete

## Summary
Successfully refactored `NxTargetFactory.kt` to create phase targets that bundle goals together instead of creating individual goal targets.

## Key Changes Made

### 1. Goal Collection by Phase
- Modified `createNxTargets()` to collect all goals by phase first
- Added `phaseGoals` map to track which goals belong to each phase
- Goals are properly normalized for Maven 3→4 compatibility

### 2. Phase Target Creation
- Updated `createPhaseTarget()` to accept a list of goals
- Phase targets now build actual Maven commands with bundled goals
- Command format: `./mvnw nx:apply goal1 goal2 goal3 nx:record -pl groupId:artifactId -N --batch-mode`

### 3. Empty Phase Handling
- Added logic to skip phases with no goals assigned
- Only creates targets for phases that actually have goals to execute
- Prevents creation of unnecessary empty targets

### 4. Dependency Management
- Maintained correct phase-to-phase dependencies
- Updated dependency logic to only depend on phases that have targets
- Removed all goal-level dependencies since goals are now bundled

### 5. Code Cleanup
- Removed `createGoalTarget()` method
- Removed `createMavenCommand()` method
- Removed `cleanPluginName()` method
- Eliminated individual goal target creation loops

## Results

### Before Refactoring
- Created individual targets for each goal: `maven-compiler:compile@default-compile`
- Many targets per project (potentially dozens)
- Phase targets were `nx:noop` that just depended on goal targets

### After Refactoring
- Creates phase targets that bundle goals: `clean`, `validate`, `compile`, etc.
- Fewer, more meaningful targets per project
- Phase targets execute actual Maven commands with multiple goals
- Command format matches exactly what was requested

### Example Generated Command
```bash
./mvnw nx:apply clean:clean@default-clean nx:record -pl org.apache.maven:maven-support -N --batch-mode
```

## Testing Results
- ✅ Plugin compiles successfully
- ✅ Plugin execution works correctly
- ✅ Goals are properly collected by phase
- ✅ Phase targets are created with bundled commands
- ✅ Empty phases are correctly skipped
- ✅ Dependencies between phases are maintained
- ✅ Build state management (`nx:apply`/`nx:record`) is preserved

## Impact
- Significantly reduced number of targets created
- Phase targets now execute multiple goals efficiently
- Maintains proper dependency relationships
- Command format is exactly as requested by user