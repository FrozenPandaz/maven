# Maven 4 Plugin Analysis Solution

## Issues to Fix

1. **Plugin Type Mismatch**:
   - `project.build.plugins` returns `org.apache.maven.api.model.Plugin` (Maven 4 immutable API)
   - `MavenPluginManager.getPluginDescriptor()` expects `org.apache.maven.model.Plugin` (Maven 3 mutable API)

2. **Session Repository Access**:
   - Maven 4 Session doesn't have `remotePluginRepositories` property
   - Should use `session.getRemoteRepositories()` instead

3. **InternalSession Usage**:
   - Line has syntax error with incomplete `plugin.` reference
   - Need to properly convert between session types

## Solution Approach

### Step 1: Create Plugin Conversion Function
```kotlin
private fun toMaven3Plugin(plugin: org.apache.maven.api.model.Plugin): org.apache.maven.model.Plugin {
    val maven3Plugin = org.apache.maven.model.Plugin()
    maven3Plugin.groupId = plugin.groupId
    maven3Plugin.artifactId = plugin.artifactId
    maven3Plugin.version = plugin.version
    // Copy other necessary fields
    return maven3Plugin
}
```

### Step 2: Fix Repository Access
Replace:
```kotlin
session.remotePluginRepositories.map { InternalSession.from(session).toRepository(it) }
```

With:
```kotlin
session.getRemoteRepositories().map { InternalSession.from(session).toRepository(it) }
```

### Step 3: Fix InternalSession Usage
The current line has a syntax error. Should be:
```kotlin
val pluginDescriptor = pluginManager.getPluginDescriptor(
    toMaven3Plugin(plugin),
    session.getRemoteRepositories().map { InternalSession.from(session).toRepository(it) },
    InternalSession.from(session).getSession()
)
```

## Implementation Plan

1. Add the plugin conversion function to PhaseAnalyzer
2. Update getMojoDescriptor to use the conversion function
3. Fix the repository access to use getRemoteRepositories()
4. Fix the syntax error with the incomplete plugin reference
5. Test compilation