# Simplified Plugin Analysis for Maven 4

## Current Problem
The PhaseAnalyzer uses MojoDescriptor to determine:
1. **Thread safety** - whether the mojo can run in parallel
2. **Cacheability** - whether the mojo's outputs can be cached
3. **Parameters** - to identify inputs and outputs

## Why This Is Complex
- MojoDescriptor requires MavenPluginManager which expects Maven 3's mutable Plugin type
- Maven 4 provides immutable api.model.Plugin types
- Converting between these types is complex and fragile

## Simpler Alternative Approach

### Option 1: Use Plugin Configuration Directly
Instead of getting MojoDescriptor, analyze the plugin configuration from the POM:
```kotlin
fun analyzeSimplified(project: Project, phase: String): PhaseInformation {
    val plugins = project.build.plugins
    var isThreadSafe = true
    var isCacheable = isPhaseCacheable(phase)
    val inputs = mutableSetOf<String>()
    val outputs = mutableSetOf<String>()

    plugins.forEach { plugin ->
        plugin.executions
            .filter { execution -> execution.phase == phase }
            .forEach { execution ->
                // Analyze based on plugin artifactId and goals
                execution.goals.forEach { goal ->
                    // Use heuristics based on plugin name and goal
                    if (isNonCacheableGoal(plugin.artifactId, goal)) {
                        isCacheable = false
                    }
                    if (isNonThreadSafeGoal(plugin.artifactId, goal)) {
                        isThreadSafe = false
                    }
                    // Analyze configuration directly
                    analyzeConfiguration(execution.configuration, inputs, outputs)
                }
            }
    }

    return PhaseInformation(isThreadSafe, isCacheable, inputs, outputs)
}
```

### Option 2: Default Conservative Approach
Since most plugin analysis is for optimization:
```kotlin
fun analyzeConservative(project: Project, phase: String): PhaseInformation {
    // Default to conservative values
    var isThreadSafe = false  // Assume not thread-safe unless proven
    var isCacheable = isPhaseCacheable(phase)  // Use phase-level heuristics
    val inputs = mutableSetOf<String>()
    val outputs = mutableSetOf<String>()

    // Add known project paths as inputs/outputs
    inputs.add(project.build.sourceDirectory)
    outputs.add(project.build.outputDirectory)

    return PhaseInformation(isThreadSafe, isCacheable, inputs, outputs)
}
```

### Option 3: Plugin Metadata Cache
Create a hardcoded knowledge base of common plugins:
```kotlin
val PLUGIN_METADATA = mapOf(
    "maven-compiler-plugin" to PluginMetadata(
        threadSafe = true,
        cacheable = true,
        inputPatterns = listOf("src/main/java/**"),
        outputPatterns = listOf("target/classes/**")
    ),
    "maven-surefire-plugin" to PluginMetadata(
        threadSafe = false,  // Tests might have side effects
        cacheable = false,    // Test results can vary
        inputPatterns = listOf("src/test/java/**", "target/classes/**"),
        outputPatterns = listOf("target/surefire-reports/**")
    )
    // ... more plugins
)
```

## Recommended Solution
**Option 1 with fallback to Option 2** - Try to analyze the plugin configuration directly, but fall back to conservative defaults if information is insufficient. This:
- Avoids the Maven 3/4 API conversion issue entirely
- Works with the data we already have
- Can be enhanced over time
- Fails safely with conservative assumptions