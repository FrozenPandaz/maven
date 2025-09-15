# Maven 4 Plugin Testing Guide

This document outlines the comprehensive testing approach implemented for the Nx Maven Analyzer Plugin, showcasing best practices for Maven 4 plugin testing.

## Testing Strategy Overview

We implement a three-tier testing strategy:

1. **Unit Tests** - Fast, isolated testing of individual components
2. **Integration Tests** - Testing plugin behavior in Maven environment
3. **Functional Tests** - End-to-end testing with real projects

## 1. Unit Testing with @MojoTest

Maven 4 introduces a modern, annotation-based testing framework that replaces the legacy `AbstractMojoTestCase`.

### Key Features:
- JUnit 5 integration
- Dependency injection via `@Inject`
- Parameter injection via `@MojoParameter`
- Mock component injection via `@Provides`
- Real project loading capabilities

### Example Test Class:

```kotlin
@MojoTest
class NxProjectAnalyzerMojoTest {
    @Inject
    private lateinit var session: Session

    @Test
    @InjectMojo(goal = "analyze")
    @MojoParameter(name = "outputFile", value = "target/nx-analysis.json")
    fun testAnalyzeWithDefaults(mojo: NxProjectAnalyzerMojo) {
        // Test mojo execution with injected parameters
        assertNotNull(mojo)
    }

    @Provides
    @Singleton
    fun provideMockComponent(): SomeComponent {
        return mock(SomeComponent.class)
    }
}
```

### Benefits:
- ✅ Clean, annotation-based configuration
- ✅ Automatic dependency injection
- ✅ Easy parameter configuration
- ✅ Mock component support
- ✅ No complex setup required

## 2. Integration Testing with Maven Verifier

Integration tests execute Maven in a separate process to test plugin behavior in a realistic environment.

### Example Integration Test:

```kotlin
class NxProjectAnalyzerIT {
    @Test
    fun testPluginExecutionOnSimpleProject() {
        val projectDir = createSimpleTestProject()
        val verifier = Verifier(projectDir.absolutePath)

        verifier.addCliArgument("dev.nx.maven:nx-maven-analyzer-plugin:analyze")
        verifier.execute()
        verifier.verifyErrorFreeLog()
        verifier.verifyFilePresent("target/nx-analysis.json")
    }
}
```

### Benefits:
- ✅ Tests plugin in real Maven environment
- ✅ Process isolation for maximum realism
- ✅ Can test CLI arguments and system properties
- ✅ Verifies actual file outputs
- ✅ Tests error handling and logging

## 3. Real Project Loading Tests

Demonstrates how to programmatically load and analyze real Maven projects using Maven 4's ProjectBuilder API.

### Example:

```kotlin
@MojoTest
class RealProjectLoadingTest {
    @Inject
    private lateinit var projectBuilder: ProjectBuilder

    @Test
    fun testLoadingProject() {
        val request = ProjectBuilderRequest.builder()
            .session(session)
            .source(Sources.fromPath(pomPath))
            .build()

        val project = projectBuilder.build(request).project.get()

        // Analyze project structure, dependencies, plugins, etc.
        assertEquals("com.example", project.groupId)
        assertTrue(project.dependencies.isNotEmpty())
    }
}
```

### Benefits:
- ✅ Works with real project structures
- ✅ Analyzes dependencies, modules, plugins
- ✅ Tests complex project hierarchies
- ✅ Validates project metadata

## Maven Configuration

### pom.xml Setup:

```xml
<build>
  <plugins>
    <!-- Unit tests -->
    <plugin>
      <artifactId>maven-surefire-plugin</artifactId>
      <configuration>
        <excludes>
          <exclude>**/*IT.java</exclude>
          <exclude>**/*IT.kt</exclude>
        </excludes>
      </configuration>
    </plugin>

    <!-- Integration tests -->
    <plugin>
      <artifactId>maven-failsafe-plugin</artifactId>
      <executions>
        <execution>
          <goals>
            <goal>integration-test</goal>
            <goal>verify</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

### Test Dependencies:

```xml
<dependencies>
  <!-- Maven 4 Testing Framework -->
  <dependency>
    <groupId>org.apache.maven</groupId>
    <artifactId>maven-testing</artifactId>
    <scope>test</scope>
  </dependency>

  <!-- Maven Integration Test Helper -->
  <dependency>
    <groupId>org.apache.maven.its</groupId>
    <artifactId>maven-it-helper</artifactId>
    <scope>test</scope>
  </dependency>

  <!-- JUnit 5 -->
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

## Test Execution Commands

### Run All Tests:
```bash
mvn verify
```

### Run Only Unit Tests:
```bash
mvn test
```

### Run Only Integration Tests:
```bash
mvn integration-test
```

### Run Specific Test:
```bash
mvn test -Dtest=NxProjectAnalyzerMojoTest
mvn integration-test -Dit.test=NxProjectAnalyzerIT
```

## Test Project Structure

```
src/test/
├── kotlin/
│   └── dev/nx/maven/
│       ├── NxProjectAnalyzerMojoTest.kt    # Unit tests
│       ├── NxProjectAnalyzerIT.kt          # Integration tests
│       ├── RealProjectLoadingTest.kt       # Project loading demos
│       └── MavenProjectLoaderTest.kt       # DI testing demos
└── resources/
    └── it-projects/
        ├── simple-java-project/            # Test project
        └── multi-module-project/           # Multi-module test
```

## Key Testing Principles

### 1. Test Naming Conventions
- Unit tests: `*Test.java/kt`
- Integration tests: `*IT.java/kt` or `*ITCase.java/kt`

### 2. Test Isolation
- Unit tests run in `test` phase
- Integration tests run in `integration-test` phase
- Each test type has separate configuration

### 3. Maven 4 Specific Features
- Use `@MojoTest` instead of `AbstractMojoTestCase`
- Leverage dependency injection with `@Inject`
- Use `ProjectBuilder` for real project loading
- Take advantage of embedded execution for speed

### 4. Best Practices
- Mock external dependencies
- Test both success and error scenarios
- Verify file outputs and logs
- Use realistic test data
- Test with multiple project structures

## Conclusion

This comprehensive testing approach ensures:

- **Fast feedback** with unit tests
- **Realistic validation** with integration tests
- **Real-world compatibility** with project loading tests
- **Modern tooling** with Maven 4's new testing framework

The combination provides confidence that the plugin works correctly across different scenarios while maintaining fast development cycles.