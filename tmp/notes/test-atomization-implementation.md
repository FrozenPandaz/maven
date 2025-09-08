# Test Atomization Implementation for Maven

## Overview
Successfully implemented test atomization for Maven JUnit tests, allowing individual test classes to be executed as separate Nx targets for better parallelization and caching.

## Key Components Implemented

### 1. Test Class Discovery (`TestClassDiscovery.kt`)
- Simple string-based parsing to find test annotations
- Supports JUnit 4, JUnit 5, and TestNG test annotations
- Discovers test classes by looking for: `@Test`, `@ParameterizedTest`, `@TestFactory`, `@RepeatedTest`, `@TestTemplate`, `@org.junit.Test`, `@org.testng.annotations.Test`

### 2. Module Discovery Fix (`NxWorkspaceGraphMojo.kt`)
- **Critical Fix**: Replaced `session.allProjects` (discovered 1,888 pom.xml files) with Maven's module discovery logic
- Now processes only actual modules (39 in Apache Maven case) using `project.modules`
- Eliminated StackOverflowError when processing large Maven projects
- Uses recursive module traversal following Maven's own discovery pattern

### 3. Test Atomization Logic (`test-atomization.ts`)
- Creates individual test targets for each discovered test class
- Uses Maven Surefire `-Dtest` parameter for targeted test execution
- Target naming: `test-ci--{TestClassName}`
- Target group: Changed from "verification" to "test" for semantic consistency

### 4. Target Group Preservation (`maven-atomization-processor.ts`)
- Fixed issue where test atomization was overwriting existing target groups
- Now preserves plugin-specific target groups alongside test atomization groups
- Ensures proper separation of targets by plugin type

## Configuration
Test atomization is configurable via `nx.json`:
```json
{
  "plugin": "./packages/maven/dist",
  "options": {
    "atomizeTests": true,
    "minTestClassesForAtomization": 1
  }
}
```

## Results
- Successfully runs individual test classes: `nx run maven-cli:test-ci--BaseParserTest`
- Proper target grouping in Nx project view
- Eliminated StackOverflowError on large codebases
- Maintained compatibility with existing Maven workflows

## Commits Made
1. `7f5c978828` - Main implementation with module discovery fix
2. `b84a856f31` - Changed target group from 'verification' to 'test'