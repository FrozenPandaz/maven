# Single Project Analysis Testing

## How to Test Analysis for a Single Project

With the two-tiered approach, you can now test the analysis for individual projects using:

```bash
mvn dev.nx.maven:nx-maven-analyzer-plugin:1.0.1:analyze-project -pl <project-name>
```

## Example Usage

```bash
# Test analysis for maven-cli project
mvn dev.nx.maven:nx-maven-analyzer-plugin:1.0.1:analyze-project -pl impl/maven-cli
```

## What It Does

- Runs the `NxProjectAnalyzerSingleMojo` on just that specific project
- Generates a JSON file at `target/nx-project-analysis.json` in that project's directory
- Analyzes common Maven phases: compile, test-compile, test, package
- Captures inputs/outputs for each phase to enable Nx caching
- Works independently without requiring full reactor build

## Output Structure

The generated JSON contains:
- Basic project metadata (artifactId, groupId, version, etc.)
- Project structure (sourceRoots, testSourceRoots, etc.)
- Dependencies list with coordinates
- Per-phase cacheability analysis with inputs/outputs
- Maven-specific metadata like packaging type

## Key Benefits

1. **Fast Testing**: Test single projects without full workspace analysis
2. **Independent Execution**: No cross-project coordination required  
3. **Detailed Analysis**: Full input/output analysis for each Maven phase
4. **Nx Integration Ready**: Output format matches what Nx expects for project configuration

The plugin was rebuilt with `npm run build` to include all three mojos:
- `analyze` - Original full workspace analyzer
- `analyze-project` - Single project analyzer
- `analyze-graph` - Workspace graph merger