# Maven Analyzer N×N Performance Issue Analysis

## Problem Identified: N×N Heavy Operations

The Maven analyzer has a severe N×N performance issue where with 39 projects, it performs 1,521+ expensive operations instead of 39.

## Root Cause: Per-Project Heavy Component Instantiation

### Current Architecture Flow
```kotlin
NxProjectAnalyzerMojo.execute() {
    for each of 39 projects {  // N iterations
        singleAnalyzer.execute() {
            // Creates NEW instances each time:
            val inputOutputAnalyzer = MavenInputOutputAnalyzer(...)  // Heavy
            val lifecycleAnalyzer = MavenLifecycleAnalyzer(...)      // Heavy  
            val pluginAnalyzer = PluginBasedAnalyzer(...)            // Heavy
            val testClassDiscovery = TestClassDiscovery()            // Light
            
            for each phase (6-8 phases) {  // M iterations per project
                inputOutputAnalyzer.analyzeCacheability(phase, project) {
                    // Creates MORE new instances:
                    val pathResolver = PathResolver(...)             // Light
                    val pluginAnalyzer = PluginBasedAnalyzer(...)    // Heavy - AGAIN!
                    val pluginExecutionFinder = PluginExecutionFinder(...) // Heavy
                    val mojoParameterAnalyzer = MojoParameterAnalyzer(...) // Heavy
                    
                    pluginExecutionFinder.findExecutionsForPhase() {
                        lifecycleExecutor.calculateExecutionPlan()  // VERY HEAVY Maven API call
                    }
                    
                    for each plugin execution found {
                        pluginManager.getPluginDescriptor()         // HEAVY Maven API call
                    }
                }
            }
        }
    }
}
```

### The N×N Problem Breakdown
- **N = 39 projects**
- **M = 6-8 phases per project** 
- **P = ~5-10 plugin executions per phase**

**Heavy Operations Per Project:**
- 1× MavenInputOutputAnalyzer creation
- 1× MavenLifecycleAnalyzer creation  
- 1× PluginBasedAnalyzer creation (outer)
- 6-8× PluginBasedAnalyzer creation (inner - per phase!)
- 6-8× PluginExecutionFinder creation
- 6-8× MojoParameterAnalyzer creation
- 48-64× lifecycleExecutor.calculateExecutionPlan() calls
- 240-640× pluginManager.getPluginDescriptor() calls

**Total Heavy Operations:**
- Instead of ~100 operations: **39 × 100 = 3,900 operations**
- Plus expensive Maven API calls scale exponentially

## Why the Previous Optimization Failed

The reverted optimization attempted to eliminate all file I/O and do everything in memory, but it:
1. **Changed the working architecture** - broke the two-tier approach
2. **Merged complex logic** - combined per-project and workspace analysis  
3. **Lost modularity** - harder to debug individual components
4. **Broke dependency resolution** - coordinatesToProjectName logic was fragile
5. **Changed output format** - disrupted consumers expecting two-tier files

## Safe Optimization Strategy

### Phase 1: Shared Component Instances (90% improvement)
```kotlin
// Create expensive components ONCE outside the project loop
val sharedInputOutputAnalyzer = MavenInputOutputAnalyzer(...)
val sharedLifecycleAnalyzer = MavenLifecycleAnalyzer(...)
val sharedPluginAnalyzer = PluginBasedAnalyzer(...)

for each project {
    // Pass project as parameter instead of creating new instances
    sharedInputOutputAnalyzer.analyzeCacheability(phase, project)
    sharedLifecycleAnalyzer.extractLifecycleData(project)  
    sharedPluginAnalyzer.analyzePhaseInputsOutputs(phase, project)
}
```

### Phase 2: Maven API Result Caching (Additional 50% improvement)
```kotlin
// Cache expensive Maven API results across projects
val executionPlanCache = mutableMapOf<String, ExecutionPlan>()
val pluginDescriptorCache = mutableMapOf<String, PluginDescriptor>()

// Cache key: "phase:packaging:groupId" (same results for similar projects)
val cacheKey = "$phase:${project.packaging}:${project.groupId}"
val executionPlan = executionPlanCache.getOrPut(cacheKey) {
    lifecycleExecutor.calculateExecutionPlan(session, phase) // Only call once per unique combo
}
```

### Phase 3: Memory-Only Analysis (Eliminate 78 file operations)
```kotlin
// Keep analysis results in memory, write only once at end
val projectAnalyses = mutableMapOf<String, ObjectNode>()
// No intermediate file writes during analysis loop
```

## Implementation Plan

1. **Preserve working two-tier architecture** - keep modularity
2. **Move expensive component creation outside loops** - reuse instances
3. **Add caching for Maven API calls** - avoid redundant expensive operations  
4. **Keep analysis in memory** - eliminate unnecessary I/O
5. **Comprehensive testing** - ensure no functionality regression

## Expected Performance Gains

- **Before**: 39 projects × ~100 heavy operations = 3,900 operations (2+ minutes)
- **After**: ~100 shared operations + cached API calls = ~150 operations (10 seconds)
- **Improvement**: ~95% reduction in analysis time

This approach maintains the working functionality while achieving the performance benefits that were attempted in the reverted optimization.