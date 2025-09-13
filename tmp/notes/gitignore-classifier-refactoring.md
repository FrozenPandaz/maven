# GitIgnoreClassifier Refactoring

## Problem
The `GitIgnoreClassifier` was being created inside each `PhaseAnalyzer` instance, which was inefficient since multiple analyzers could be created during the Maven session.

## Solution
Moved the `GitIgnoreClassifier` creation to the main `NxProjectAnalyzerMojo` where it's created once per session and passed to `PhaseAnalyzer` instances.

## Changes Made

### PhaseAnalyzer.kt
- Updated constructor to accept `GitIgnoreClassifier?` as a parameter with default value `null`
- Removed internal `GitIgnoreClassifier` creation logic
- Removed `close()` method since resource management is now handled in the mojo

### NxProjectAnalyzerMojo.kt
- Added `GitIgnoreClassifier` creation in the `execute()` method
- Added proper cleanup in the `finally` block
- Updated method signature to pass the classifier to analysis methods
- Added resource cleanup with `gitIgnoreClassifier?.close()`

### PhaseAnalyzerTest.kt
- Updated test to create `GitIgnoreClassifier` exactly as done in the main mojo
- Added mock setup for `MavenSession.executionRootDirectory` to return test project directory
- Added proper resource cleanup with `@AfterEach` method to close `GitIgnoreClassifier`
- Added field to store `GitIgnoreClassifier` instance for cleanup

## Benefits
1. **Performance**: Only one `GitIgnoreClassifier` instance per session instead of multiple
2. **Resource Management**: Proper cleanup in finally block ensures Git resources are released
3. **Architecture**: Better separation of concerns with resource management in the main mojo

## Test Results
All tests pass successfully. The PhaseAnalyzerTest works correctly with the new constructor signature.