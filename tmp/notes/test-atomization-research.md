# Test Atomization Research Notes

## Gradle Implementation Analysis

### Key Components
1. **CiTargetsUtils.kt** - Main atomization logic
   - Creates individual test targets per test class
   - Groups them under "verification" target group
   - Creates parent CI target that depends on all atomized targets

2. **Test Detection**
   - Uses AST parsing (Java and Kotlin parsers)
   - Looks for JUnit annotations (@Test, @ParameterizedTest, etc.)
   - Supports JUnit 4, JUnit 5, and TestNG

3. **Target Structure**
   - Individual targets named: `test-ci--<ClassName>`
   - Parent target: `test-ci` (nx:noop executor)
   - Uses @nx/gradle:gradle executor with testClassName option

## Maven Current State
- Has basic test target execution
- Uses maven-surefire-plugin for test phase
- No atomization support yet
- Analyzer collects test source roots

## Design Approach for Maven
1. Parse test files to find test classes
2. Create atomized targets for each test class
3. Use maven-surefire-plugin with -Dtest=<ClassName> pattern
4. Create parent CI target that runs all atomized tests