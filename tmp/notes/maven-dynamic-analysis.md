# Maven Dynamic Analysis - Replacing Hard-coded Logic

## Current Problem
The `MavenInputOutputAnalyzer.kt` class contains extensive hard-coded logic:
- Hard-coded phase mappings (validate, compile, test, package, etc.)
- Manual input/output path definitions 
- Static cacheability rules
- Fixed directory structure assumptions

## Maven APIs We Can Use Instead

### 1. Plugin Management API
- `MavenProject.getBuildPlugins()` - Get all configured plugins
- `Plugin.getExecutions()` - Get plugin executions for each phase
- `PluginExecution.getGoals()` - Get specific goals
- `PluginExecution.getConfiguration()` - Get plugin configuration

### 2. Lifecycle API
- `DefaultLifecycles` - Access to built-in lifecycle definitions
- `Lifecycle.getPhases()` - Get all phases in a lifecycle
- `MavenSession.getCurrentProject()` - Access current project context

### 3. Mojo Descriptor API
- `MojoDescriptor.getParameters()` - Get mojo input parameters
- `MojoDescriptor.getOutputDirectory()` - Get output directories
- `PluginDescriptor.getMojos()` - Get all mojos in a plugin

### 4. Build Context
- `MavenProject.getBuild()` - Get build configuration
- `Build.getDirectory()` - Get target directory
- `Build.getOutputDirectory()` - Get compiled classes directory
- `Build.getTestOutputDirectory()` - Get test classes directory

## Benefits of Dynamic Approach
1. **No maintenance** - Works with any Maven setup automatically
2. **Custom plugins supported** - Handles project-specific plugins
3. **Configuration-aware** - Respects actual Maven configuration
4. **Future-proof** - Adapts to Maven changes automatically