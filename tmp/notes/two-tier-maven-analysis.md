# Two-Tier Maven Analysis Implementation

## Overview
Successfully implemented a two-tier Maven analysis approach that dramatically simplifies per-project analysis while enabling incremental updates and better Nx integration.

## Architecture

### Before (Single-Tier)
- **Single aggregator plugin** (`@Mojo(aggregator = true)`)
- **All-or-nothing analysis** - must rerun entire workspace when any project changes
- **Complex cross-project coordination** in single analyzer
- **No incremental benefits**

### After (Two-Tier) 
- **Per-project analysis** (`analyze-project` goal) - simple, focused, cacheable
- **Workspace graph generation** (`analyze-graph` goal) - coordinates individual analyses
- **Incremental analysis** - only re-analyze changed projects
- **Parallel execution** - Nx can run individual analyses concurrently

## Implementation Details

### 1. Per-Project Analyzer (`NxProjectAnalyzerSingleMojo`)
- **Goal**: `analyze-project`
- **Scope**: Single project only (`aggregator = false`)
- **Output**: `{project}/target/nx-project-analysis.json`
- **Features**:
  - Analyzes project structure, dependencies, source roots
  - Generates phase configurations (compile, test-compile, test, package)
  - Includes `dependentTasksOutputFiles` for proper cache invalidation
  - No cross-project coordination needed

### 2. Workspace Graph Generator (`NxWorkspaceGraphMojo`)
- **Goal**: `analyze-graph` 
- **Scope**: Workspace-wide (`aggregator = true`)
- **Input**: Individual project analysis files
- **Output**: `nx-maven-projects.json`
- **Features**:
  - Merges individual project analyses
  - Resolves cross-project dependencies using Maven's dependency model
  - Generates complete Nx workspace graph
  - Coordinates `dependsOn` relationships

### 3. TypeScript Integration
- **New function**: `runMavenAnalysisTwoTier()` 
- **Process**: 
  1. Run `analyze-project` on all projects
  2. Run `analyze-graph` to merge results
- **Backwards compatible**: Original `runMavenAnalysis()` still available

## Key Benefits Achieved

### ✅ **Incremental Analysis**
- Only changed projects need re-analysis
- Individual project analysis files can be cached
- Much faster for large workspaces

### ✅ **Simplified Logic**
- Per-project analyzers have much simpler logic
- No need for complex `coordinatesToProjectName` mapping in individual analysis
- Cleaner separation of concerns

### ✅ **Better Nx Integration**
- Aligns with Nx's distributed execution model
- Each project analysis can be cached independently
- Proper `dependentTasksOutputFiles` configuration

### ✅ **Parallelization**
- Nx can run individual project analyses in parallel
- No serialization bottleneck from single aggregator

## Cache Invalidation Enhancement

### Problem Solved
Before: Maven tasks only included dependency outputs that Maven directly referenced in `compileClasspathElements`

After: All Maven tasks include `dependentTasksOutputFiles` to automatically include outputs from all `dependsOn` tasks as inputs

### Configuration Added
```json
{
  "inputs": [
    "{projectRoot}/pom.xml",
    {
      "dependentTasksOutputFiles": "**/*", 
      "transitive": true
    },
    "{projectRoot}/src/main/java/**/*"
  ]
}
```

This ensures that when any dependency's output changes, dependent tasks get cache invalidated automatically.

## Performance Comparison

### Traditional Single-Tier
- **Analysis Time**: O(n) for all projects always
- **Cache Hits**: All-or-nothing 
- **Parallelization**: None during analysis

### New Two-Tier  
- **Analysis Time**: O(changed projects only)
- **Cache Hits**: Per-project granular
- **Parallelization**: Full parallelization of individual analyses

## Testing Results

✅ **Per-project analysis works**: Successfully analyzed `maven-api-annotations` individually  
✅ **Workspace graph generation works**: Successfully merged all 39 project analyses  
✅ **Proper dependencies resolved**: `dependsOn` arrays correctly populated  
✅ **Cache configuration included**: `dependentTasksOutputFiles` in all tasks  
✅ **Backwards compatibility**: Original analyzer still functional

## Future Enhancements

1. **Nx Plugin Integration**: Update Nx plugin to use two-tier approach by default
2. **Intelligent Scheduling**: Let Nx determine when to run individual vs full analysis
3. **Incremental Dependency Resolution**: Only recalculate cross-project dependencies when needed

## Files Modified/Created

### New Maven Goals
- `NxProjectAnalyzerSingleMojo.kt` - Per-project analysis
- `NxWorkspaceGraphMojo.kt` - Workspace graph generation

### Updated Components  
- `maven-analyzer.ts` - Added `runMavenAnalysisTwoTier()` function
- `MavenInputOutputAnalyzer.kt` - Added `dependentTasksOutputFiles` configuration

### Analysis Method Identifier
The generated workspace graph includes `"analysisMethod": "two-tier"` to identify the new approach.