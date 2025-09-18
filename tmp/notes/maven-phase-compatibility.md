# Maven Phase Compatibility Between Maven 3 and Maven 4

## Overview

This document summarizes the findings about how Maven handles phase name compatibility between Maven 3 and Maven 4 versions. The search focused on mappings, compatibility layers, and handling of the "after:" prefix for phases.

## Key Findings

### 1. Phase Name Aliases (Maven 3 → Maven 4 Mapping)

**Location:** `/Users/jason/projects/triage/java/maven/impl/maven-core/src/main/java/org/apache/maven/internal/impl/DefaultLifecycleRegistry.java` (lines 454-469)

The DefaultLifecycle class contains explicit aliases that map Maven 3 phase names to their Maven 4 equivalents:

```java
@Override
public Collection<Alias> aliases() {
    return List.of(
            alias("generate-sources", SOURCES),
            alias("process-sources", AFTER + SOURCES),
            alias("generate-resources", RESOURCES),
            alias("process-resources", AFTER + RESOURCES),
            alias("process-classes", AFTER + COMPILE),
            alias("generate-test-sources", TEST_SOURCES),
            alias("process-test-sources", AFTER + TEST_SOURCES),
            alias("generate-test-resources", TEST_RESOURCES),
            alias("process-test-resources", AFTER + TEST_RESOURCES),
            alias("process-test-classes", AFTER + TEST_COMPILE),
            alias("prepare-package", BEFORE + PACKAGE),
            alias("pre-integration-test", BEFORE + INTEGRATION_TEST),
            alias("post-integration-test", AFTER + INTEGRATION_TEST));
}
```

### 2. Phase Prefixes and Constants

**Location:** `/Users/jason/projects/triage/java/maven/api/maven-api-core/src/main/java/org/apache/maven/api/Lifecycle.java` (lines 55-57)

Maven 4 defines phase qualifiers as constants:

```java
String BEFORE = "before:";
String AFTER = "after:";
String AT = "at:";
```

### 3. Phase Name Parsing and Transformation

**Location:** `/Users/jason/projects/triage/java/maven/impl/maven-core/src/main/java/org/apache/maven/lifecycle/internal/PhaseId.java`

The PhaseId class handles parsing of phase identifiers including "before:" and "after:" prefixes:

```java
private PhaseId(String phase) {
    int phaseStart;
    if (phase.startsWith(PhaseExecutionPoint.BEFORE.prefix())) {
        executionPoint = PhaseExecutionPoint.BEFORE;
        phaseStart = PhaseExecutionPoint.BEFORE.prefix().length();
    } else if (phase.startsWith(PhaseExecutionPoint.AFTER.prefix())) {
        executionPoint = PhaseExecutionPoint.AFTER;
        phaseStart = PhaseExecutionPoint.AFTER.prefix().length();
    } else {
        executionPoint = PhaseExecutionPoint.AT;
        phaseStart = 0;
    }
    // ... parsing logic continues
}
```

### 4. CLI Phase Transformation and Warning

**Location:** `/Users/jason/projects/triage/java/maven/impl/maven-core/src/main/java/org/apache/maven/lifecycle/internal/DefaultLifecycleTaskSegmentCalculator.java` (lines 103-107)

When users try to call "before:" or "after:" phases directly from CLI, Maven transforms them and shows a warning:

```java
if (isBeforeOrAfterPhase(task)) {
    String prevTask = task;
    task = PhaseId.of(task).phase();
    LOGGER.warn("Illegal call to phase '{}'. The main phase '{}' will be used instead.", prevTask, task);
}
```

### 5. Phase Execution Points

**Location:** `/Users/jason/projects/triage/java/maven/impl/maven-core/src/main/java/org/apache/maven/lifecycle/internal/PhaseExecutionPoint.java`

Maven defines three execution points for phases:

```java
public enum PhaseExecutionPoint {
    BEFORE("before:"),
    AT(""),
    AFTER("after:");
}
```

### 6. Maven 3 vs Maven 4 Phase Ordering

**Location:** `/Users/jason/projects/triage/java/maven/api/maven-api-core/src/main/java/org/apache/maven/api/Lifecycle.java` (lines 75-83)

The Lifecycle interface includes a method specifically for Maven 3 compatibility:

```java
/**
 * Collection of main phases for this lifecycle used with the Maven 3 builders.
 * Those builders do not operate on a graph, but on the list and expect a slightly
 * different ordering (mainly unit test being executed before packaging).
 *
 * @return the collection of phases in Maven 3 compatible ordering
 */
default Collection<Phase> v3phases() {
    return phases();
}
```

## Key Phase Name Mappings

| Maven 3 Phase Name | Maven 4 Equivalent |
|-------------------|-------------------|
| `generate-sources` | `sources` |
| `process-sources` | `after:sources` |
| `generate-resources` | `resources` |
| `process-resources` | `after:resources` |
| `process-classes` | `after:compile` |
| `generate-test-sources` | `test-sources` |
| `process-test-sources` | `after:test-sources` |
| `generate-test-resources` | `test-resources` |
| `process-test-resources` | `after:test-resources` |
| `process-test-classes` | `after:test-compile` |
| `prepare-package` | `before:package` |
| `pre-integration-test` | `before:integration-test` |
| `post-integration-test` | `after:integration-test` |

## Testing Evidence

**Location:** `/Users/jason/projects/triage/java/maven/its/core-it-suite/src/test/java/org/apache/maven/it/MavenITmng8245BeforePhaseCliTest.java`

Integration tests verify that calling "before:" or "after:" phases from CLI produces warnings and executes the main phase instead:

```java
verifier.verifyTextInLog("Illegal call to phase 'before:clean'. The main phase 'clean' will be used instead.");
```

## Conclusion

Maven 4 provides comprehensive compatibility for Maven 3 phase names through:

1. **Explicit aliases** that map old phase names to new phase names with appropriate prefixes
2. **CLI transformation** that converts direct calls to "before:" or "after:" phases to the main phase
3. **Phase parsing infrastructure** that understands the prefixed phase naming convention
4. **Separate v3phases() method** for maintaining compatibility with Maven 3 builders

This allows Maven 4 to handle both legacy Maven 3 phase names and the new prefixed phase system seamlessly.