# Goal Prefix Change from nx-maven to nx

## Summary
Changed the Maven plugin goal prefix from `nx-maven` to `nx` for a cleaner and shorter command syntax.

## Files Modified

### 1. Maven Plugin Configuration
- **File**: `packages/maven/analyzer-plugin/pom.xml`
- **Change**: Updated `<goalPrefix>nx-maven</goalPrefix>` to `<goalPrefix>nx</goalPrefix>`

### 2. Target Factory Commands
- **File**: `packages/maven/analyzer-plugin/src/main/kotlin/dev/nx/maven/NxTargetFactory.kt`
- **Changes**:
  - `nx-maven:apply` → `nx:apply`
  - `nx-maven:attach-artifact` → `nx:attach-artifact`
  - `nx-maven:record` → `nx:record`

### 3. Documentation
- **File**: `tmp/notes/attach-artifact-goal.md`
- **Changes**: Updated all examples to use `nx:` prefix instead of `nx-maven:`

## Impact

### Before
```bash
mvn nx-maven:analyze
mvn nx-maven:attach-artifact -Dartifact=/path/to/file.jar
mvn nx-maven:apply
mvn nx-maven:record
```

### After
```bash
mvn nx:analyze
mvn nx:attach-artifact -Dartifact=/path/to/file.jar
mvn nx:apply
mvn nx:record
```

## Notes
- The full Maven coordinate (`dev.nx.maven:nx-maven-analyzer-plugin:version:goal`) remains unchanged
- Generated files like `nx-maven-projects.json` will be updated automatically when the plugin is rebuilt
- This is a breaking change for users who have existing scripts using the old prefix