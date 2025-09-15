# Best Way to Get MojoDescriptors in Maven 4

## The Correct Approach

### 1. Use MavenPluginManager with Proper Conversions

The MavenPluginManager is still the correct way to get MojoDescriptors, but you need to handle the type conversions properly:

```kotlin
private fun getMojoDescriptor(plugin: org.apache.maven.api.model.Plugin,
                             goal: String,
                             project: Project): MojoDescriptor? {
    return try {
        // Convert Maven 4 Plugin to Maven 3 Plugin
        val maven3Plugin = toMaven3Plugin(plugin)

        // Get the internal session
        val internalSession = InternalSession.from(session)

        // Convert repositories
        val remoteRepos = session.getRemoteRepositories()
            .map { internalSession.toRepository(it) }

        // Get the repository session
        val repoSession = internalSession.getSession()

        // Get the MojoDescriptor
        pluginManager.getMojoDescriptor(
            maven3Plugin,
            goal,
            remoteRepos,
            repoSession
        )
    } catch (e: Exception) {
        log.warn("Failed to get MojoDescriptor for ${plugin.artifactId}:$goal: ${e.message}")
        null
    }
}

private fun toMaven3Plugin(plugin: org.apache.maven.api.model.Plugin): org.apache.maven.model.Plugin {
    val maven3Plugin = org.apache.maven.model.Plugin()
    maven3Plugin.groupId = plugin.groupId
    maven3Plugin.artifactId = plugin.artifactId
    maven3Plugin.version = plugin.version

    // Convert executions if needed
    plugin.executions?.forEach { execution ->
        val maven3Execution = org.apache.maven.model.PluginExecution()
        maven3Execution.id = execution.id
        maven3Execution.phase = execution.phase
        maven3Execution.goals = execution.goals.toList()
        maven3Execution.configuration = execution.configuration
        maven3Plugin.addExecution(maven3Execution)
    }

    return maven3Plugin
}
```

### 2. Key Points

1. **InternalSession** provides the bridge between Maven 4 and Maven 3:
   - `InternalSession.from(session)` - converts Maven 4 Session
   - `toRepository()` - converts Maven 4 RemoteRepository to Aether type
   - `getSession()` - gets the RepositorySystemSession

2. **Session repositories**:
   - Use `session.getRemoteRepositories()` not `session.remotePluginRepositories`
   - Convert each using `InternalSession.toRepository()`

3. **Plugin conversion**:
   - Must convert from `api.model.Plugin` to `model.Plugin`
   - Copy essential fields (groupId, artifactId, version)
   - Optionally copy executions if needed

## Alternative Approaches

### Option A: Use PluginDescriptor First
```kotlin
// Get the full plugin descriptor
val pluginDescriptor = pluginManager.getPluginDescriptor(
    maven3Plugin,
    remoteRepos,
    repoSession
)

// Then get specific mojo
val mojoDescriptor = pluginDescriptor.getMojo(goal)
```

### Option B: Direct Analysis (Skip MojoDescriptor)
```kotlin
// Analyze plugin configuration directly
plugin.executions.forEach { execution ->
    execution.goals.forEach { goal ->
        // Use heuristics based on plugin/goal
        val threadSafe = isKnownThreadSafeGoal(plugin.artifactId, goal)
        val cacheable = isKnownCacheableGoal(plugin.artifactId, goal)

        // Analyze configuration for inputs/outputs
        analyzeConfiguration(execution.configuration)
    }
}
```

## Recommendation

Use the **proper conversion approach** (Option 1) if you need accurate MojoDescriptor information. The conversions are straightforward and InternalSession provides all the necessary bridging methods.