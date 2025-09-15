# Maven 4 Plugin Analysis Fix

## Current Issue
The PhaseAnalyzer is trying to use MavenPluginManager with mixed Maven 3 and Maven 4 APIs:
- `project.build.plugins` returns `org.apache.maven.api.model.Plugin` (Maven 4 API)
- `MavenPluginManager.getPluginDescriptor()` expects `org.apache.maven.model.Plugin` (Maven 3 API)
- `session.remotePluginRepositories` doesn't exist in Maven 4 API Session

## Investigation Results

### Maven 4 API Changes
1. Maven 4.1.0-SNAPSHOT uses a new immutable API model
2. The `org.apache.maven.api.model.Plugin` is the new immutable type
3. The old `org.apache.maven.model.Plugin` is the mutable Maven 3 type
4. Session no longer has `remotePluginRepositories` property

### Possible Solutions

#### Option 1: Remove Phase Analysis (Simplest)
Since the plugin analysis is failing and appears to be optional functionality:
- Comment out or remove the `getMojoDescriptor` calls
- Skip detailed mojo-level analysis
- Focus on project-level inputs/outputs only

#### Option 2: Convert Plugin Types (Current Approach)
Convert between API types, but this has issues:
- Need to map from immutable to mutable model
- Repository information is not readily available
- Complex and fragile

#### Option 3: Use Maven 4 Native APIs (Best Long-term)
Research shows Maven 4 has new plugin descriptor APIs in:
- `org.apache.maven.api.plugin.descriptor` package
- Immutable model with Builder patterns
- But unclear how to get descriptors from plugins

#### Option 4: Skip Plugin Analysis Temporarily
Since this appears to be for optimization/caching:
- Return null from `getMojoDescriptor`
- Log a warning about Maven 4 compatibility
- Implement proper solution later

## Recommended Fix
For immediate resolution, Option 4 is best:
1. Make `getMojoDescriptor` return null with a warning
2. This allows compilation to succeed
3. Document as a known limitation for Maven 4
4. Can be properly implemented when Maven 4 APIs mature