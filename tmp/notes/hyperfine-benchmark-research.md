# Hyperfine Benchmark Research Summary

Date: 2025-11-07
Task: Setup hyperfine benchmark comparison for Nx vs Maven

## 1. Hyperfine Installation Status

✓ **Hyperfine is installed and ready to use**
- Version: 1.18.0
- Location: Available in system PATH
- Status: Fully functional

## 2. Project Structure

### Maven API Model Project
- **Full Path**: `/home/jason/projects/triage/java/maven/api/maven-api-model`
- **Project ID**: `org.apache.maven:maven-api-model`
- **Type**: Maven JAR project
- **Parent**: `org.apache.maven:maven-api` (version 4.1.0-SNAPSHOT)
- **Build Output**: `api/maven-api-model/target`

### Nx Configuration
- **Nx Version**: 22.1.0-beta.5
- **Plugin**: @nx/maven (22.1.0-beta.5)
- **Working Directory**: `/home/jason/projects/triage/java/maven`
- **Nx Cloud**: Configured (staging environment)

### Key Project Features
- Uses Modello Maven Plugin to generate sources from maven.mdo
- Has generate-sources phase for code generation
- Depends on maven-api-annotations and maven-api-xml

## 3. Existing Benchmark Infrastructure

The project already has JMH benchmarks in place:
- Location: `impl/maven-xml/BENCHMARKS.md`
- Benchmark classes found:
  - ModelValidationBenchmark.java
  - XmlPlexusConfigurationBenchmark.java
  - XmlPlexusConfigurationMemoryBenchmark.java
  - XmlPlexusConfigurationConcurrencyBenchmark.java

These use JMH (Java Microbenchmark Harness) for micro-benchmarks, while hyperfine will be used for command-level macro-benchmarks.

## 4. Commands to Compare

### Command 1: Nx Verify
```bash
nx verify maven-api-model
```
- Uses Nx's caching and task orchestration
- Full project name: `org.apache.maven:maven-api-model`
- Executes Maven verify goal through Nx wrapper

### Command 2: Maven Verify
```bash
mvn verify -pl org.apache.maven:maven-api-model -am
```
- Standard Maven command
- `-pl` flag: selects specific project
- `-am` flag: also make (builds dependencies)

## 5. Preparation Steps Needed

### Before Benchmark
1. **Clean build**: Remove target directories to ensure fair comparison
2. **Warmup**: Run each command at least once to prime caches
3. **Dependency resolution**: Ensure all Maven dependencies are downloaded

### Clean Command Options
```bash
# Option 1: Clean via Nx
nx clean maven-api-model

# Option 2: Clean via Maven
mvn clean -pl org.apache.maven:maven-api-model

# Option 3: Clean all (for full reset)
mvn clean
```

## 6. Recommended Hyperfine Command

### Basic Benchmark
```bash
hyperfine \
  --warmup 3 \
  --runs 10 \
  --prepare 'mvn clean -pl org.apache.maven:maven-api-model' \
  'nx verify maven-api-model' \
  'mvn verify -pl org.apache.maven:maven-api-model -am'
```

### Advanced Benchmark with Export
```bash
hyperfine \
  --warmup 3 \
  --min-runs 10 \
  --prepare 'mvn clean -pl org.apache.maven:maven-api-model' \
  --export-markdown /home/jason/projects/triage/java/maven/tmp/benchmark-results.md \
  --export-json /home/jason/projects/triage/java/maven/tmp/benchmark-results.json \
  --time-unit millisecond \
  'nx verify maven-api-model' \
  'mvn verify -pl org.apache.maven:maven-api-model -am'
```

### With Cleanup (if needed)
```bash
hyperfine \
  --warmup 3 \
  --runs 10 \
  --prepare 'mvn clean -pl org.apache.maven:maven-api-model' \
  --cleanup 'echo "Run completed"' \
  'nx verify maven-api-model' \
  'mvn verify -pl org.apache.maven:maven-api-model -am'
```

## 7. Hyperfine Key Options Explained

- `--warmup N`: Run N warmup iterations before measuring (important for JVM warm-up)
- `--runs N`: Execute exactly N benchmark runs
- `--min-runs N`: At least N runs (default 10)
- `--prepare CMD`: Run before each benchmark run (for clean state)
- `--cleanup CMD`: Run after each benchmark completes
- `--export-markdown FILE`: Save results as markdown table
- `--export-json FILE`: Save detailed results as JSON
- `--time-unit UNIT`: Display time in specific unit (millisecond, second, etc.)
- `--show-output`: Show command stdout/stderr (useful for debugging)

## 8. Expected Outcomes

### What to Measure
- **Mean execution time**: Average build time
- **Standard deviation**: Consistency of builds
- **Min/Max times**: Best and worst case scenarios
- **Relative performance**: Speed difference between Nx and Maven

### Nx Advantages (Expected)
- Faster on subsequent runs due to caching
- Better with unchanged code (cache hits)
- Parallel execution of independent tasks

### Maven Advantages (Expected)
- May be faster on first run (no Nx overhead)
- More predictable without caching layer

## 9. Considerations

### Important Notes
1. **JVM Warmup**: Java applications need warmup runs for JIT compilation
2. **Cache State**: Nx caching will affect results - consider testing both cache hit and miss scenarios
3. **Dependencies**: Both commands build dependencies with `-am` flag
4. **Disk I/O**: Benchmarks can be affected by disk speed and system load
5. **Nx Cloud**: Cloud caching may affect results (consider testing with/without)

### Cache Testing Scenarios
```bash
# Scenario 1: Cold cache (Nx cache cleared)
nx reset
hyperfine --warmup 1 --runs 5 'nx verify maven-api-model'

# Scenario 2: Warm cache (run twice)
hyperfine --warmup 1 --runs 5 'nx verify maven-api-model'
```

## 10. Alternative Approaches

### Test Without Dependencies
If you want to test just the single project without dependencies:
```bash
hyperfine \
  --warmup 2 \
  --runs 8 \
  --prepare 'mvn clean -pl org.apache.maven:maven-api-model' \
  'nx verify maven-api-model' \
  'mvn verify -pl org.apache.maven:maven-api-model'  # no -am flag
```

### Test Full Build
For complete project build comparison:
```bash
hyperfine \
  --warmup 1 \
  --runs 5 \
  --prepare 'mvn clean' \
  'nx run-many --target=verify --all' \
  'mvn verify'
```

## 11. Next Steps

1. Decide on benchmark scope (single project vs full build)
2. Choose cache scenario (cold vs warm)
3. Run preliminary test to verify commands work
4. Execute full benchmark suite
5. Analyze results and document findings
6. Consider additional scenarios (incremental builds, etc.)

## 12. Related Documentation

- Hyperfine docs: https://github.com/sharkdp/hyperfine
- Nx Maven plugin: Using @nx/maven version 22.1.0-beta.5
- Existing JMH benchmarks: See `impl/maven-xml/BENCHMARKS.md`
