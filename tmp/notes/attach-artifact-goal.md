# Attach Artifact Goal Implementation

## Summary
Implemented a new Maven goal `nx:attach-artifact` that allows attaching artifacts to Maven projects. This goal is not bound to any phase and can be used standalone.

## Key Components

### 1. NxAttachArtifactMojo.kt
- Created a standalone Maven Mojo for attaching artifacts
- Parameters:
  - `artifact` (required): Path to the artifact file to attach
  - `classifier` (optional): Classifier for the artifact
  - `type` (optional): Type of artifact (default: jar)
  - `mainArtifact` (optional): Whether to set as main project artifact (default: false)

### 2. NxTargetFactory.kt Refactoring
- Added `ArtifactAttachmentConfig` data class to configure which goals need artifact attachment
- Created a map `artifactAttachmentConfigs` for dynamic configuration:
  - `install:install` requires main artifact attachment
  - `spring-boot:repackage` requires artifact attachment but not as main
- Extracted `createMavenCommand()` method that handles:
  - Artifact attachment logic
  - Command parameter processing
  - Configuration merging

## Usage Examples

### Basic Usage
```bash
./mvnw nx:attach-artifact -Dartifact=/path/to/artifact.jar -pl com.example:demo -N
```

### Set as Main Artifact (for install goal)
```bash
./mvnw nx:attach-artifact -Dartifact=/path/to/artifact.jar -DmainArtifact=true -pl com.example:demo -N
```

### With Classifier
```bash
./mvnw nx:attach-artifact -Dartifact=/path/to/artifact.jar -Dclassifier=sources -pl com.example:demo -N
```

## Integration with Install Goal
The implementation now automatically prepends `nx:attach-artifact` to the `install:install` goal when needed, creating a command like:
```bash
mvn nx:attach-artifact -Dartifact=/path/to/artifact.jar -DmainArtifact=true install:install@execution-id -pl com.example:demo -N
```

## Benefits
- Clean separation of concerns
- Easy to extend for other goals
- Maintains Maven's standard workflow
- Flexible artifact attachment options