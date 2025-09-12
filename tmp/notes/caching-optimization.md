# Maven Analyzer Caching Optimization Issue

## Problem Analysis

The `MavenInputOutputAnalyzer` is instantiated once in `NxProjectAnalyzerMojo` and passed to `MavenLifecycleAnalyzer`. However, there are several inefficiencies:

1. **New PluginBasedAnalyzer instances per phase**: Inside `MavenInputOutputAnalyzer.analyzeCacheability()`, a new `PluginBasedAnalyzer` is created for each phase analysis. This means:
   - The plugin descriptor cache in `PluginBasedAnalyzer` is recreated each time
   - The execution plan cache in `PluginExecutionFinder` is also recreated each time
   - Plugin descriptors are being loaded multiple times for the same plugins

2. **Cache locality issues**: Even though `MavenInputOutputAnalyzer` is shared, the actual caching happens in components that are recreated for each call:
   - `PluginBasedAnalyzer` has `pluginDescriptorCache`
   - `PluginExecutionFinder` has `executionPlanCache`
   - These caches are lost between phase analyses

3. **Performance impact**: When analyzing multiple phases (e.g., compile, test, package, verify), the same plugin descriptors and execution plans are recalculated multiple times.

## Call Flow

```
NxProjectAnalyzerMojo.execute()
  -> Creates ONE MavenInputOutputAnalyzer (shared)
  -> Creates ONE MavenLifecycleAnalyzer (shared, uses the shared analyzer)
  
  For each project (in parallel):
    -> NxProjectAnalyzer.analyze()
      -> MavenLifecycleAnalyzer.generatePhaseTargets()
        For each phase:
          -> MavenInputOutputAnalyzer.analyzeCacheability()
            -> Creates NEW PluginBasedAnalyzer (loses cache!)
            -> Creates NEW PluginExecutionFinder (loses cache!)
            -> Reloads plugin descriptors
            -> Recalculates execution plans
```

## Solution

Create the `PluginBasedAnalyzer` and its dependencies once and reuse them across all phase analyses. This will allow the caches to be effective across multiple phases and projects.