# Maven DI Testing Against Actual Repositories

## Key Components for DI Testing

### Base Test Setup
Use `@PlexusTest` annotation and extend `AbstractCoreMavenComponentTestCase`:

```java
@PlexusTest
public class YourTest extends AbstractCoreMavenComponentTestCase {

    @Inject
    protected PlexusContainer container;

    @Inject
    protected RepositorySystem repositorySystem;

    @Inject
    protected org.apache.maven.project.ProjectBuilder projectBuilder;
}
```

### Testing Against Actual Repository Files

1. **Set up test project directory structure**:
   ```
   src/test/projects/your-test/
   ├── pom.xml
   └── (other project files)
   ```

2. **Override `getProjectsDirectory()` in your test**:
   ```java
   @Override
   protected String getProjectsDirectory() {
       return "src/test/projects/your-test-directory";
   }
   ```

3. **Load actual project from disk**:
   ```java
   @Test
   void testWithActualRepo() throws Exception {
       File pom = getProject("your-project-name");  // Copies from src/test/projects to target/
       MavenSession session = createMavenSession(pom);
       MavenProject project = session.getCurrentProject();

       // Your test logic here
   }
   ```

### DI Container Setup Patterns

#### Pattern 1: Using Guice Modules
```java
container = new DefaultPlexusContainer(
    new DefaultContainerConfiguration(),
    new AbstractModule() {
        @Override
        protected void configure() {
            bind(YourInterface.class).to(YourImplementation.class);
        }
    },
    new SisuDiBridgeModule(false)
);
```

#### Pattern 2: Using Implicit Binding
```java
container = new DefaultPlexusContainer(
    new DefaultContainerConfiguration(),
    new SisuDiBridgeModule(false) {
        @Override
        protected void configure() {
            super.configure();
            injector.bindImplicit(YourTestClass.class);
        }
    }
);
```

### Creating Maven Sessions with Properties
```java
Properties executionProperties = new Properties();
executionProperties.setProperty("your.property", "/path/to/value");

MavenSession session = createMavenSession(pom, executionProperties);
```

### Dependency Injection in Tests
```java
static class DiInjected {
    @org.apache.maven.api.di.Inject
    YourService service;

    @org.apache.maven.api.di.Inject
    List<YourService> services;

    @org.apache.maven.api.di.Inject
    Map<String, YourService> serviceMap;
}

@Test
void testDI() throws Exception {
    DiInjected diInjected = new DiInjected();
    container.lookup(org.apache.maven.di.Injector.class).injectInstance(diInjected);

    assertNotNull(diInjected.service);
    // Test your injected components
}
```

## Key Files Examined
- `/Users/jason/projects/triage/java/maven/impl/maven-core/src/test/java/org/apache/maven/di/DiTest.java:1` - DI testing patterns
- `/Users/jason/projects/triage/java/maven/impl/maven-core/src/test/java/org/apache/maven/AbstractCoreMavenComponentTestCase.java:84` - Base test infrastructure
- `/Users/jason/projects/triage/java/maven/impl/maven-core/src/test/java/org/apache/maven/project/ProjectBuilderTest.java:64` - Repository testing examples

## Summary
Maven DI tests work by:
1. Setting up a Plexus container with necessary DI modules
2. Using test project directories with real pom.xml files
3. Creating Maven sessions that simulate real execution context
4. Injecting components and testing against actual repository structures