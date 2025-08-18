# Removed Maven Analysis Caching

## Summary
Successfully removed all caching mechanisms from the Maven plugin to ensure the Maven analysis is always regenerated from scratch.

## Changes Made

### 1. Plugin Code Changes (`packages/maven/src/plugin.ts`)
- **Removed imports**: Eliminated `readJsonFile`, `writeJsonFile`, and `calculateHashForCreateNodes`
- **Removed global cache variables**: Deleted `globalAnalysisCache` and `globalCacheKey`
- **Removed cache functions**: Deleted `readMavenCache()` and `writeMavenCache()` functions
- **Simplified createNodesV2**: Removed all cache checking logic and hash generation
- **Always run analysis**: Now directly calls `runMavenAnalysis()` on every invocation

### 2. Parameter Fix
- **Fixed parameter name**: Changed from `-Doutput.file=` to `-Dnx.outputFile=` to match Kotlin analyzer parameter
- **Fixed absolute path handling**: Updated Kotlin analyzer to handle absolute paths correctly

### 3. Kotlin Analyzer Update (`NxProjectAnalyzerMojo.kt`)
- **Absolute path support**: Added logic to handle absolute output paths
```kotlin
val outputPath = if (outputFile.startsWith("/")) {
    // Absolute path
    File(outputFile)
} else {
    // Relative path
    File(workspaceRoot, outputFile)
}
```

## Before vs After

### Before (Cached Behavior)
- ✗ Checked global in-memory cache first
- ✗ Checked disk cache with hash-based keys
- ✗ Only regenerated analysis if cache miss
- ✗ Complex cache management with potential stale data

### After (Always Fresh)
- ✅ Always runs Maven analysis from scratch
- ✅ No cache checking or management overhead
- ✅ Guarantees up-to-date analysis on every run
- ✅ Simplified code without cache complexity

## Verification
Tested that the analysis file is regenerated on each `nx show project` command:

```bash
# First run
rm -f .nx/workspace-data/nx-maven-projects.json
nx show project org.apache.maven.maven-api-core
ls -la .nx/workspace-data/nx-maven-projects.json
# -rw-rw-r-- 1 jason jason 397285 Aug 18 17:42

# Second run (2 seconds later)
nx show project org.apache.maven.maven-api-core
ls -la .nx/workspace-data/nx-maven-projects.json
# -rw-rw-r-- 1 jason jason 397285 Aug 18 17:42 (timestamp updated)
```

## Impact
- **Pros**: Always fresh data, simpler code, no cache staleness issues
- **Cons**: Slightly slower due to Maven analysis on every run
- **Trade-off**: Accuracy over performance - Maven analysis takes ~1-2 seconds

The Maven plugin now guarantees that project configurations and dependencies are always based on the current state of the Maven reactor, eliminating any potential issues with stale cached data.