# Maven Project Discovery and Reactor Project Builder

## Overview
This document describes how Maven discovers and builds projects in a reactor (multi-module) build.

## 1. "Scanning for projects..." Log Message

**Source File**: `/home/jason/projects/triage/java/maven/impl/maven-cli/src/main/java/org/apache/maven/cling/event/ExecutionEventLogger.java` (line 114)

**Entry Point**: `ExecutionEventLogger.projectDiscoveryStarted(ExecutionEvent event)`

The message is logged when the `projectDiscoveryStarted` execution event is triggered. This is called as part of the execution listener chain during the lifecycle phase that discovers projects.

```java
@Override
public void projectDiscoveryStarted(ExecutionEvent event) {
    if (logger.isInfoEnabled()) {
        init();
        logger.info("Scanning for projects...");
    }
}
```

**Deprecated Version**: Also exists in `/home/jason/projects/triage/java/maven/compat/maven-embedder/src/main/java/org/apache/maven/cli/event/ExecutionEventLogger.java` (marked as deprecated)

---

## 2. Project Discovery Flow

### A. Graph Builder - Entry Point
**File**: `/home/jason/projects/triage/java/maven/impl/maven-core/src/main/java/org/apache/maven/graph/DefaultGraphBuilder.java`

**Method**: `build(MavenSession session)` - Lines 95-116

This is the main orchestrator that:
1. Checks if a project dependency graph already exists (session already has projects)
2. If not, collects projects from the reactor using `getProjectsForMavenReactor()`
3. Validates and enriches the collected projects
4. Creates a dependency graph

**Key Decision Tree** (lines 349-368):
```
1. No POM file → use PomlessCollectionStrategy
2. Make behavior OR project activation requested → use MultiModuleCollectionStrategy
3. Otherwise → use RequestPomCollectionStrategy
```

### B. Collection Strategies

#### 1. MultiModuleCollectionStrategy
**File**: `/home/jason/projects/triage/java/maven/impl/maven-core/src/main/java/org/apache/maven/project/collector/MultiModuleCollectionStrategy.java`

**Purpose**: Collects all projects from a multi-module project root, even when invoked from a submodule.

**Process**:
1. Locates the root project using `.mvn` directory detection and `RootLocator`
2. Calls `getRootProject()` to find the actual root POM
3. Uses `ProjectsSelector.selectProjects()` to build all projects recursively
4. Validates that the requested project is included in the collected projects

#### 2. RequestPomCollectionStrategy
**File**: `/home/jason/projects/triage/java/maven/impl/maven-core/src/main/java/org/apache/maven/project/collector/RequestPomCollectionStrategy.java`

**Purpose**: Collects projects explicitly requested via -f/--file flag

#### 3. PomlessCollectionStrategy
**File**: `/home/jason/projects/triage/java/maven/impl/maven-core/src/main/java/org/apache/maven/project/collector/PomlessCollectionStrategy.java`

**Purpose**: Handles Maven invocation without a POM file

### C. Projects Selector
**File**: `/home/jason/projects/triage/java/maven/impl/maven-core/src/main/java/org/apache/maven/project/collector/DefaultProjectsSelector.java`

**Method**: `selectProjects(List<File> files, MavenExecutionRequest request)` - Lines 54-102

This delegates to `ProjectBuilder.build()` with `recursive` flag:
- Calls `ProjectBuilder.build(files, isRecursive, projectBuildingRequest)` 
- `isRecursive = hasProjectSelection || request.isRecursive()` (line 60)
- Collects problems and logs warnings

---

## 3. Recursive Module Discovery

### A. ProjectBuilder - Low Level Collection
**File**: `/home/jason/projects/triage/java/maven/impl/maven-core/src/main/java/org/apache/maven/project/DefaultProjectBuilder.java`

**Method**: `build(List<File> pomFiles, boolean recursive, ProjectBuildingRequest request)` - Lines 193-198

This method builds a BuildSession and calls `doBuild()`.

**Recursive Build** (lines 484-503):
- When `recursive=true`, the ModelBuilder performs full multi-module discovery
- Uses parallel execution for performance

### B. Model Builder - Core Discovery Engine
**File**: `/home/jason/projects/triage/java/maven/impl/maven-impl/src/main/java/org/apache/maven/impl/model/DefaultModelBuilder.java`

#### Phase 1: Load From Root (lines 678-697)
```java
private void loadFromRoot(Path root, Path top) {
    // Creates a PhasingExecutor for parallel module loading
    try (PhasingExecutor executor = createExecutor()) {
        // Loads from top or root project
        loadFilePom(executor, top, root, Set.of(), r);
    }
}
```

#### Phase 2: Recursive Module Collection (lines 699-775)
**Method**: `loadFilePom(Executor executor, Path top, Path pom, Set<Path> parents, DefaultModelBuilderResult r)`

**Process**:
1. Reads the POM file at the given path
2. Activates profiles
3. Extracts subprojects/modules from the model (line 709)
4. For each subproject:
   - Locates the POM file (line 716)
   - Checks for aggregation cycles (line 733)
   - Creates result object
   - Recursively loads child POM (line 765)

#### Key Methods for Module Discovery:

**Line 706-708 - Module Location**:
```java
Model activated = activateFileModel(model);
for (String subproject : getSubprojects(activated)) {
    // process each subproject
    Path rawSubprojectFile = modelProcessor.locateExistingPom(
        pomDirectory.resolve(subproject)
    );
}
```

**Lines 1929-1935 - Subproject Extraction**:
```java
private static List<String> getSubprojects(Model activated) {
    List<String> subprojects = activated.getSubprojects();
    if (subprojects.isEmpty()) {
        subprojects = activated.getModules();  // fallback to <modules>
    }
    return subprojects;
}
```

**Lines 1395-1418 - Auto-discovery** (for Maven > 4.0.0):
When POM packaging is "pom" but no modules/subprojects are declared, Maven auto-discovers:
```java
if (getSubprojects(model).isEmpty()
    && !MODEL_VERSION_4_0_0.equals(model.getModelVersion())
    && Type.POM.equals(model.getPackaging())) {
    List<String> subprojects = new ArrayList<>();
    try (Stream<Path> files = Files.list(model.getProjectDirectory())) {
        for (Path f : files.toList()) {
            if (Files.isDirectory(f)) {
                Path subproject = modelProcessor.locateExistingPom(f);
                if (subproject != null) {
                    subprojects.add(f.getFileName().toString());
                }
            }
        }
    }
}
```

### C. Model Processor - POM Locator
**File**: `/home/jason/projects/triage/java/maven/impl/maven-impl/src/main/java/org/apache/maven/impl/model/DefaultModelProcessor.java`

**Method**: `locateExistingPom(Path projectDirectory)` - Lines 81-95

**Process**:
1. Tries custom ModelParsers first (for custom file types)
2. Falls back to `doLocateExistingPom()` (lines 126-138)
3. Looks for `pom.xml` in directory or treats single file as POM

```java
private Path doLocateExistingPom(Path project) {
    if (project == null) {
        project = Paths.get(System.getProperty("user.dir"));
    }
    if (Files.isDirectory(project)) {
        Path pom = project.resolve("pom.xml");
        return Files.isRegularFile(pom) ? pom : null;
    } else if (Files.isRegularFile(project)) {
        return project;
    } else {
        return null;
    }
}
```

---

## 4. Parallelism and Performance

### A. PhasingExecutor for Parallel Module Loading
**Lines 337-352 in DefaultModelBuilder**:
```java
PhasingExecutor createExecutor() {
    return new PhasingExecutor(Executors.newFixedThreadPool(getParallelism()));
}

private int getParallelism() {
    int parallelism = Runtime.getRuntime().availableProcessors() / 2 + 1;
    try {
        String str = request.getUserProperties()
            .get(Constants.MAVEN_MODEL_BUILDER_PARALLELISM);
        if (str != null) {
            parallelism = Integer.parseInt(str);
        }
    } catch (Exception e) {
        // ignore
    }
    return Math.max(1, Math.min(parallelism, 
        Runtime.getRuntime().availableProcessors()));
}
```

**Default**: Processors / 2 + 1

**Configuration**: Can be controlled via property `maven.model.builder.parallelism`

### B. Module Building Phases (lines 630-663)
After loading all models from files, the effective model is built in parallel:
```java
var allResults = results(result).toList();
try (PhasingExecutor executor = createExecutor()) {
    for (DefaultModelBuilderResult r : allResults) {
        executor.execute(() -> {
            mbs.buildEffectiveModel(new LinkedHashSet<>());
        });
    }
}
```

---

## 5. Cycle Detection

**File**: `/home/jason/projects/triage/java/maven/impl/maven-impl/src/main/java/org/apache/maven/impl/model/DefaultModelBuilder.java`

**Location**: Lines 387-395 and 733-751

**Graph-based Cycle Detection**:
- Maintains a DAG (Directed Acyclic Graph) in `ModelBuilderSessionState.dag`
- When adding edges between models, throws `Graph.CycleDetectedException` if detected
- Reports cycle as FATAL error with path: `A -> B -> C -> A`

---

## 6. Configuration and Performance Tuning

### A. User Properties
- `maven.model.builder.parallelism` - Controls number of threads for module loading

### B. Build Request Options
- `recursive` flag - Triggers full multi-module discovery
- `makeBehavior` - Can be `REACTOR_MAKE_UPSTREAM`, `REACTOR_MAKE_DOWNSTREAM`, or `REACTOR_MAKE_BOTH`
- `projectActivation` - Selects which projects to build based on patterns

### C. Request Scoping
**Lines 309-328 in DefaultGraphBuilder**:
- Projects can be filtered to request scope (modules within requested project)
- "Also make" options include/exclude upstream/downstream dependencies
- Resumed builds start from specified project
- Excluded projects removed from build

---

## 7. Error Handling

### A. Missing Subproject
Lines 718-729: Reports ERROR if subproject doesn't exist

### B. Aggregation Cycles
Lines 733-751: Reports ERROR if subproject forms a cycle

### C. Plugin Extension Issues
**MultiModuleCollectionStrategy** (lines 146-182):
- Handles case where module plugin can't be built in same session
- Falls through to next collection strategy

---

## Summary of Flow

```
ExecutionEventLogger.projectDiscoveryStarted()
  └─> "Scanning for projects..." logged
      
DefaultGraphBuilder.build()
  └─> getProjectsForMavenReactor()
      └─> Choose strategy:
          1. MultiModuleCollectionStrategy (if .mvn found)
          2. PomlessCollectionStrategy (no POM)
          3. RequestPomCollectionStrategy (explicit -f)
          
DefaultProjectsSelector.selectProjects()
  └─> ProjectBuilder.build(files, recursive=true)
      
DefaultProjectBuilder.BuildSession.doBuild()
  └─> Parallel for each POM file:
      └─> DefaultModelBuilder.buildBuildPom()
          └─> loadFromRoot()
              └─> loadFilePom() [RECURSIVE]
                  ├─> readFileModel()
                  ├─> getSubprojects() 
                  ├─> locateExistingPom() for each child
                  └─> executor.execute(() -> loadFilePom(child))
                  
                  With parallel buildEffectiveModel() phase
```

---

## Key Files

| File | Purpose |
|------|---------|
| ExecutionEventLogger | Logs "Scanning for projects..." message |
| DefaultGraphBuilder | Orchestrates project collection and dependency graph building |
| DefaultModelBuilder | Recursive module discovery and POM processing |
| MultiModuleCollectionStrategy | Finds root project and builds entire reactor |
| DefaultProjectsSelector | Delegates to ProjectBuilder with recursive flag |
| DefaultProjectBuilder | Low-level POM file collection |
| DefaultModelProcessor | Locates pom.xml files in directory structure |

