# Target Organization and Naming Implementation

## What was implemented

### 1. Target Organization by Plugin Groups
- Modified `NxWorkspaceGraphMojo.kt` to group plugin goals by their plugin name
- Goals are now processed in logical groups (e.g., all Spring Boot goals together)
- Added logging to show which goals belong to each plugin group

### 2. Target Naming Convention
- Changed from custom shortened names to clean `plugin:goal` syntax
- Plugin names are cleaned to remove unnecessary suffixes like "-maven-plugin"
- Targets use clean, intuitive names matching Maven's shortened syntax
- Examples:
  - `spring-boot-maven-plugin:run` becomes target `spring-boot:run`
  - `maven-compiler-plugin:compile` becomes target `compiler:compile`
  - `maven-surefire-plugin:test` becomes target `surefire:test`

### 3. Target Metadata
- Added `metadata` section to each plugin goal target
- Metadata includes:
  - `plugin`: The cleaned plugin name
  - `originalPlugin`: The full original plugin artifact ID
  - `goalName`: The individual goal name
- This allows tooling to understand the plugin structure and maintain mapping to original names

### 4. Plugin Name Cleaning
- Added `cleanPluginName()` function to remove unnecessary suffixes and prefixes
- Removes "-maven-plugin" suffix and "maven-" prefix
- Handles well-known plugins with special cases
- Preserves essential names like "spring-boot", "compiler", "surefire"

### 5. Removed Collision Handling  
- Eliminated complex unique target name generation since we now use clean plugin:goal naming
- This prevents goal collisions (e.g., spring-boot:run vs invoker:run)
- Simplified the code by removing collision detection functions

## Benefits

1. **Clean naming**: Target names use short, intuitive plugin names (`spring-boot:run` vs `spring-boot-maven-plugin:run`)
2. **Maven familiarity**: Names match Maven's shortened plugin syntax that developers already know
3. **Clear organization**: Goals are logically grouped by plugin in logs and processing  
4. **No more collisions**: Clean plugin names prevent different plugins from overriding each other's goals
5. **Better tooling support**: Metadata preserves both clean and original names for tooling
6. **Simpler code**: Removed complex collision detection logic

## Files modified
- `/home/jason/projects/triage/java/maven/packages/maven/analyzer-plugin/src/main/kotlin/dev/nx/maven/NxWorkspaceGraphMojo.kt`

## Key code changes
- Added plugin name cleaning: `val cleanPluginName = cleanPluginName(originalPluginName)`
- Grouped goals by cleaned plugin names: `goalsByPlugin.getOrPut(cleanPluginName) { mutableListOf() }.add("$originalPluginName:$goalName")`
- Added enhanced metadata with both clean and original names: `metadata.put("originalPlugin", originalPluginName)`
- Used clean plugin:goal naming: `val targetName = "$cleanPluginName:$goalName"`