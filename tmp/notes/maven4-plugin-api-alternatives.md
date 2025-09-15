# Maven 4 Plugin API Alternatives

## The Big Change in Maven 4

Maven 4 introduces a completely new **immutable API** for plugins:
- Old Maven 3: `org.apache.maven.model.Plugin` (mutable)
- New Maven 4: `org.apache.maven.api.model.Plugin` (immutable)
- New descriptors: `org.apache.maven.api.plugin.descriptor.PluginDescriptor` (immutable)

## Modern Maven 4 Alternatives

### 1. PluginXmlFactory Service
Maven 4 provides `org.apache.maven.api.services.xml.PluginXmlFactory` to read plugin descriptors:
```kotlin
@Inject
private lateinit var pluginXmlFactory: PluginXmlFactory

// Read plugin descriptor from plugin JAR
val pluginDescriptor = pluginXmlFactory.read(
    XmlReaderRequest.builder()
        .path(pathToPluginXml)
        .build()
)
```

### 2. MojoExecution Interface
Maven 4's `org.apache.maven.api.MojoExecution` provides access to:
- `getDescriptor()` - returns the Maven 4 MojoDescriptor
- `getPlugin()` - returns the Maven 4 Plugin
- `getConfiguration()` - returns the configuration

However, this is only available in MojoExecutionScoped context (during mojo execution).

### 3. The Problem
There's no direct Maven 4 API service to:
- Load a plugin descriptor from a plugin artifact (GAV coordinates)
- Get MojoDescriptors outside of mojo execution context
- Bridge between the API Plugin type and plugin descriptors

## Why MavenPluginManager Still Exists
The old `MavenPluginManager` is part of Maven's internal implementation and expects Maven 3 types. It's not part of the new Maven 4 API, which is why we have these compatibility issues.

## Solutions

### Option A: Use PluginXmlFactory Directly
1. Resolve the plugin artifact using ArtifactResolver
2. Extract the META-INF/maven/plugin.xml from the JAR
3. Use PluginXmlFactory to read the descriptor
4. Access MojoDescriptors from the immutable PluginDescriptor

### Option B: Skip Plugin Descriptor Analysis
Since we're mainly using it for:
- Thread safety checks (can use heuristics)
- Cacheability checks (can use heuristics)
- Parameter analysis (can analyze configuration directly)

We could bypass the need for MojoDescriptor entirely.

### Option C: Create a Conversion Layer
Create utility functions to convert between Maven 3 and Maven 4 types, but this is complex and fragile.

## Recommendation
**Option B** is the most practical for now. Maven 4's API is still evolving, and trying to force compatibility between the old and new systems is problematic. Using heuristics based on plugin/goal names and analyzing configurations directly will be more maintainable.