# MavenInvoker Deep Dive Analysis

## Overview
MavenInvoker is the core component that executes Maven builds. It acts as a bridge between command-line options and the Maven core execution engine. It's part of the "Cling" CLI framework and manages the entire lifecycle from option parsing to build execution and error handling.

## Architecture

### Class Hierarchy
```
LookupInvoker<C extends LookupContext> (Abstract)
    └─ MavenInvoker extends LookupInvoker<MavenContext>
            └─ ResidentMavenInvoker (keeps Maven instance resident)
```

### Key Components

#### 1. MavenContext
- **Location**: `org.apache.maven.cling.invoker.mvn.MavenContext`
- **Role**: Extends `LookupContext` to provide Maven-specific context
- **Key Fields**:
  - `Maven maven` - The Maven execution engine instance
  - `MavenOptions` - Maven-specific options parsed from CLI
  - Inherited from LookupContext: settings, toolchains, EventSpyDispatcher, Lookup service

#### 2. MavenInvoker
- **Location**: `org.apache.maven.cling.invoker.mvn.MavenInvoker`
- **Role**: Main orchestrator for Maven build execution
- **Responsibilities**:
  - Parse Maven options and create context
  - Set up Maven execution environment
  - Build and populate MavenExecutionRequest
  - Execute Maven and handle results

#### 3. LookupInvoker
- **Location**: `org.apache.maven.cling.invoker.LookupInvoker`
- **Role**: Abstract base class managing dependency injection container and core lifecycle
- **Manages**:
  - Plexus DI container setup
  - Settings and toolchains resolution
  - Logging configuration
  - Terminal setup
  - Class loading and environment setup

## Complete Execution Flow

### Phase 1: Context Creation (invoke → doInvoke)
```
LookupInvoker.invoke(InvokerRequest)
  └─ createContext(invokerRequest) → MavenContext
  └─ doInvoke(context)
       ├─ validate(context) - Check parsing success
       ├─ pushCoreProperties(context) - Set MAVEN_HOME
       ├─ pushUserProperties(context) - Push user properties to System.properties
       ├─ setupGuiceClassLoading(context)
       ├─ configureLogging(context) - Configure SLF4J, log levels
       ├─ createTerminal(context) - Setup terminal, colors, output streams
       ├─ activateLogging(context) - Activate SLF4J
       ├─ helpOrVersionAndMayExit(context) - Handle --help/--version
       └─ preCommands(context) - Show version info if requested
```

### Phase 2: Container Setup
```
doInvoke(context) continues:
  ├─ container(context)
  │   └─ createContainerCapsule() → PlexusContainerCapsule
  │       ├─ Creates Plexus DI container
  │       ├─ Loads core extensions
  │       └─ Provides Lookup service
  │
  ├─ postContainer(context) - Run PropertyContributor SPI
  ├─ pushUserProperties(context) - Refresh after SPI
  ├─ lookup(context) - Get Maven and EventSpyDispatcher from container
  ├─ init(context) - Fire EventSpy init with context data
  ├─ postCommands(context) - Log configuration details
  └─ settings(context) - Load and build effective settings
```

### Phase 3: Maven Execution (execute)
```
MavenInvoker.execute(MavenContext context)
  ├─ prepareMavenExecutionRequest() → DefaultMavenExecutionRequest
  │   ├─ Set defaults:
  │   ├─ setRepositoryCache(new DefaultRepositoryCache())
  │   ├─ setInteractiveMode(true)
  │   ├─ setRecursive(true)
  │   ├─ setReactorFailureBehavior(REACTOR_FAIL_FAST)
  │   ├─ setStartInstant(MonotonicClock.now())
  │   ├─ setLoggingLevel(LOGGING_LEVEL_INFO)
  │   └─ setDegreeOfConcurrency(1)
  │
  ├─ toolchains(context, request) - Load and set toolchains
  │   ├─ Resolve user and installation toolchains files
  │   ├─ Run ToolchainsBuilderRequest through SettingsBuilder
  │   ├─ Set effective toolchains on request
  │   └─ Emit warnings if problems detected
  │
  ├─ populateRequest(context, lookup, request) - Build full request
  │   ├─ populateRequestFromSettings(request, settings)
  │   │   ├─ Offline mode, plugin groups
  │   │   ├─ Servers, proxies, mirrors
  │   │   └─ Remote repositories
  │   │
  │   ├─ Determine POM file
  │   ├─ Set multi-module project directory and root
  │   ├─ Set toolchains
  │   ├─ Set goals from options
  │   ├─ Set reactor failure behavior
  │   ├─ Set offline/online, snapshot updates
  │   ├─ Set checksum policy
  │   ├─ Set TransferListener (progress reporting)
  │   ├─ Set ExecutionListener (build logging)
  │   ├─ Set profile activation
  │   ├─ Set project selection (-pl, -rf, -am, -amd)
  │   ├─ Set threading/concurrency options
  │   └─ Set builder selection
  │
  └─ doExecute(context, request) - Execute and handle results
      ├─ Emit request through EventSpy
      ├─ context.maven.execute(request) → MavenExecutionResult
      ├─ Handle exceptions if present:
      │   ├─ For each exception:
      │   │   ├─ handler.handleException(exception) → ExceptionSummary
      │   │   └─ logSummary() with ANSI color handling
      │   │
      │   ├─ Emit error messages
      │   ├─ Suggest -e flag for full stacktrace
      │   ├─ Suggest -X flag for verbose output
      │   ├─ Log help references
      │   ├─ Calculate resume hint (-r or -rf) if applicable
      │   └─ Return 0 if failNever, else 1
      │
      └─ Return 0 on success

### Phase 4: Cleanup
```
context.close() (from try-with-resources)
  ├─ Close closeables in reverse order
  ├─ closeContainer()
  │   ├─ Plexus container shutdown
  │   ├─ Clear lookup, eventSpyDispatcher, maven
  │   └─ MavenContext.doCloseContainer() nullifies maven
  │
  ├─ Restore Thread context classloader
  └─ Restore System properties
```

## Key Design Patterns

### 1. Layered Context
- **LookupContext**: Base context with common functionality (settings, toolchains, logging)
- **MavenContext**: Maven-specific context extending LookupContext
- **ResidentMavenContext**: Shared context for resident invoker mode (e.g., mvnsh)

### 2. Options Layering
```
MavenOptions (from LookupInvoker.parseOptions)
  ├─ CLI options (highest priority)
  ├─ @file options (from @filename argument)
  ├─ maven.config options (from .mvn/maven.config)
  └─ Combined via LayeredMavenOptions
```

### 3. Event-Driven Architecture
- **EventSpyDispatcher**: Publishes events through build lifecycle
  - `SettingsBuilderRequest`, `SettingsBuilderResult`
  - `ToolchainsBuilderRequest`, `ToolchainsBuilderResult`
  - `MavenExecutionRequest`, `MavenExecutionResult`
  - Build events (project start/end, mojo execution, etc.)

### 4. Listener Chain
- **ExecutionListener** (execution events)
  - `ExecutionEventLogger` - Logs build events
  - `LoggingExecutionListener` - Wraps with BuildEventListener
  - Optional `EventSpyDispatcher` chain

- **TransferListener** (artifact transfer events)
  - `QuietMavenTransferListener` - Silent mode (quiet, CI, batch)
  - `ConsoleMavenTransferListener` - Interactive console progress
  - `Slf4jMavenTransferListener` - SLF4J logging
  - Wrapped in `SimplexTransferListener` for thread-safety in multi-threaded builds
  - Wrapped in `MavenTransferListener` with BuildEventListener

## Maven Instance Setup

### Container Creation (PlexusContainerCapsule)
```
ContainerCapsuleFactory.createContainerCapsule()
  ├─ Creates Plexus DI container
  ├─ Registers core Maven components
  ├─ Loads core extensions from:
  │   ├─ User extensions (.mvn/extensions.xml)
  │   └─ Installation extensions
  ├─ Loads PropertyContributor SPI implementations
  └─ Returns ContainerCapsule wrapping container + Lookup service
```

### Maven Service Lookup
```
context.lookup.lookup(Maven.class)
  └─ Returns configured Maven instance
      ├─ Configured with:
      │   ├─ Repository system
      │   ├─ Model builder
      │   ├─ Project builder
      │   ├─ Lifecycle executor
      │   └─ Plugin manager
      └─ Ready for execute(MavenExecutionRequest) → MavenExecutionResult
```

## Request Building From Options

### MavenOptions → MavenExecutionRequest Mapping

| MavenOptions | MavenExecutionRequest | Notes |
|---|---|---|
| `alternatePomFile()` | `pom` | Via ModelProcessor.locateExistingPom() |
| `nonRecursive()` | `recursive` | Inverted boolean |
| `updateSnapshots()` | `updateSnapshots` | |
| `activatedProfiles()` | `profileActivation` | Parsed with +/- prefix |
| `suppressSnapshotUpdates()` | `noSnapshotUpdates` | |
| `strictChecksums()` | `globalChecksumPolicy` | CHECKSUM_POLICY_FAIL |
| `relaxedChecksums()` | `globalChecksumPolicy` | CHECKSUM_POLICY_WARN |
| `failFast()` | `reactorFailureBehavior` | REACTOR_FAIL_FAST |
| `failAtEnd()` | `reactorFailureBehavior` | REACTOR_FAIL_AT_END |
| `failNever()` | `reactorFailureBehavior` | REACTOR_FAIL_NEVER |
| `offline()` | `offline` | |
| `threads()` | `degreeOfConcurrency`, `builderId` | "1C" format for core multiplier |
| `builder()` | `builderId` | "singlethreaded", "multithreaded" |
| `projects()` | `projectActivation` | Parsed with +/- prefix |
| `alsoMake()` | `makeBehavior` | REACTOR_MAKE_UPSTREAM |
| `alsoMakeDependents()` | `makeBehavior` | REACTOR_MAKE_DOWNSTREAM |
| `resume()` | `resume` | |
| `resumeFrom()` | `resumeFrom` | |
| `goals()` | `goals` | Direct pass-through |

### Options Parsing Sources (in order of precedence)
1. **CLI arguments** (highest priority)
2. **@file arguments** (from `@filename` option)
3. **.mvn/maven.config** (project-level defaults)

### Project/Profile Activation Syntax
```
-pl projectSelector1,projectSelector2  # Multiple comma-separated
-P +profile,-other,?optional           # +/-/? prefixes
```
- No prefix or `+` = activate
- `-` or `!` = deactivate
- `?` = optional (doesn't fail if not found)

## Error Handling Strategy

### Exception Flow
```
Maven.execute(request) throws any exception
  └─ Captured in MavenExecutionResult
      ├─ result.getExceptions() → List<Throwable>
      ├─ For each exception:
      │   ├─ DefaultExceptionHandler.handleException(ex) → ExceptionSummary
      │   ├─ ExceptionSummary contains:
      │   │   ├─ message - User-friendly message
      │   │   ├─ reference - Help article URL
      │   │   ├─ exception - Root cause
      │   │   └─ children - Nested summaries
      │   └─ logSummary() recursively logs with indentation
      │
      └─ Special handling for LifecycleExecutionException
          ├─ Extract failed project
          ├─ Determine resume point (-rf :artifactId)
          ├─ Handle artifact ID collisions (use groupId:artifactId)
```

### Resume Hints
```
Build failed at first project:
  → Suggest: mvn [args] -rf :artifactId (or groupId:artifactId if collision)

Build can be resumed:
  → Suggest: mvn [args] -r

Resume from skipped:
  → logBuildResumeHint()
```

### Error Reporting Options
- `-e` / `--show-errors` - Full stack traces
- `-X` / `--debug` - Verbose output
- `-q` / `--quiet` - Minimal output (error level only)

### ANSI Color Handling in Error Messages
- Preserves ANSI color codes from exception messages
- Tracks "current color" across multi-line output
- Adds ANSI_RESET if line ends with color code
- Pattern: `(\u001B\[[;\\d]*[ -/]*[@-~])[^\u001B]*$`

## Output/Logging Integration

### Log Configuration (LookupInvoker.configureLogging)
```
Color Detection
  ├─ Option: --color=(always|auto|never)
  ├─ Default: environment-specific
  ├─ Batch mode (-B) or log file → no color
  └─ MessageUtils enables/disables colors

Logger Setup
  ├─ SLF4J configuration factory (Logback, Log4j2)
  ├─ Root logger level:
  │   ├─ DEBUG if -X (verbose)
  │   ├─ ERROR if -q (quiet)
  │   └─ INFO default
  └─ Activation via slf4jConfiguration.activate()
```

### Terminal & Output Streams
```
Terminal Creation
  ├─ Embedded mode: Uses provided streams
  └─ Normal mode: Uses System.out via JLine3

Stream Handling
  ├─ Raw streams disabled (default):
  │   ├─ Wrap System.out → stdout logger
  │   ├─ Wrap System.err → stderr logger
  │   └─ Preserve ANSI, capture all output
  │
  └─ Raw streams enabled:
      └─ Use true System.out/err directly

Log File (-l logfile)
  ├─ ProjectBuildLogAppender writes events to file
  ├─ SimpleBuildEventListener captures build events
  └─ Output also to console (not file-only)
```

### Build Event Listener
```
Execution Events → ExecutionEventLogger
  ├─ Project start/end
  ├─ Mojo execution
  ├─ Fork execution
  ├─ Project skip
  └─ Session start/end → Reactor summary

Transfer Events → TransferListener (delegate selectable)
  ├─ Transfer initiated/progressed/succeeded/failed
  ├─ Corruption detected
  └─ Wrapped with SimplexTransferListener for thread-safety

Output Destinations
  ├─ Console (ConsoleMavenTransferListener) - Interactive progress meter
  ├─ SLF4J (Slf4jMavenTransferListener) - Log file output
  ├─ Quiet (QuietMavenTransferListener) - Silent
  └─ BuildEventListener - Per-project logging to file
```

## Version Handling & Compatibility

### Default Behavior
- **Single-threaded** by default (degreeOfConcurrency=1, builderId="singlethreaded")
- **Interactive mode** unless CI detected or -B specified
- **Fail-fast** by default (first build failure stops reactor)
- **Repository cache** enabled (DEFAULT_REPOSITORY_CACHE)
- **Checksum policy** unspecified (maven default)

### Concurrent Build Support
```
--threads 4      → degreeOfConcurrency=4, builderId="multithreaded"
--threads 1.5C   → degrees = (1.5 * availableProcessors)
--builder custom → builderId="custom"
```

### Artifact Descriptor Handling
- `setIgnoreMissingArtifactDescriptor(true)` - Default
- `setIgnoreInvalidArtifactDescriptor(true)` - Default
- `--strict-artifact-descriptor-policy` - Both false

### Plugin Groups
- Always includes: `org.apache.maven.plugins`, `org.codehaus.mojo`
- From settings: `<pluginGroups>` section

## ResidentMavenInvoker Specialization

### Key Differences
```
MavenInvoker (fresh container per invocation)
  └─ Clean shutdown of Plexus container
  └─ Fresh Maven instance each time

ResidentMavenInvoker (persistent container)
  ├─ Shares Plexus container across invocations
  ├─ Shares Maven instance via ConcurrentHashMap
  ├─ Key requirement: Same environment (no env changes)
  ├─ Usage: mvnsh (Maven shell)
  └─ copyIfDifferent() creates shadow context
      ├─ Reuses: containerCapsule, lookup, eventSpyDispatcher, maven
      └─ Fresh: invokerRequest, cwd, options
```

## Integration Points

### Maven API Used
- `Maven.execute(MavenExecutionRequest)` → `MavenExecutionResult`
- `ModelProcessor.locateExistingPom(Path)`
- `SettingsBuilder.build(SettingsBuilderRequest)`
- `ToolchainsBuilder.build(ToolchainsBuilderRequest)`
- `Lookup.lookup(Class<T>)` - DI service lookup

### Event System
- `EventSpyDispatcher` - Publishes lifecycle events
- `ExecutionListener` - Build execution events
- `TransferListener` - Artifact transfer events
- `PropertyContributor` SPI - Custom properties

### Settings & Toolchains
- **SettingsBuilderRequest/Result** - Build effective settings
- **ToolchainsBuilderRequest/Result** - Build effective toolchains
- **Sources** - Configuration file sources
- **ProtoSession** - Session with properties pre-resolution

## Code Examples

### Basic Execution Flow
```java
// 1. Parse options
MavenParser parser = new MavenParser();
MavenOptions options = parser.parseCliOptions(args);

// 2. Create invoker
MavenInvoker invoker = new MavenInvoker(protoLookup, contextConsumer);

// 3. Create request
InvokerRequest invokerRequest = ...build from args...

// 4. Invoke (handles everything)
int exitCode = invoker.invoke(invokerRequest);
```

### Listening to Events
```java
// EventSpyDispatcher publishes:
eventSpyDispatcher.onEvent(settingsBuilderRequest);
eventSpyDispatcher.onEvent(settingsBuilderResult);
eventSpyDispatcher.onEvent(mavenExecutionRequest);
eventSpyDispatcher.onEvent(mavenExecutionResult);

// ExecutionListener callbacks:
listener.projectStarted(ExecutionEvent event);
listener.mojoStarted(ExecutionEvent event);
listener.mojoSucceeded(ExecutionEvent event);
listener.projectSucceeded(ExecutionEvent event);
```

### Error Handling Example
```java
// In doExecute():
if (result.hasExceptions()) {
    for (Throwable exception : result.getExceptions()) {
        ExceptionSummary summary = handler.handleException(exception);
        logSummary(context, summary, references, "");  // Recursive
    }
    // Log help references, resume hints
    return 1;  // or 0 if failNever
}
return 0;  // Success
```

## Key Files

### Core Files
- `/impl/maven-cli/src/main/java/org/apache/maven/cling/invoker/mvn/MavenInvoker.java` (443 lines)
- `/impl/maven-cli/src/main/java/org/apache/maven/cling/invoker/mvn/MavenContext.java` (48 lines)
- `/impl/maven-cli/src/main/java/org/apache/maven/cling/invoker/LookupInvoker.java` (934 lines)
- `/impl/maven-cli/src/main/java/org/apache/maven/cling/invoker/LookupContext.java` (156 lines)

### Supporting Files
- `MavenParser.java` - Options parsing with layering
- `CommonsCliMavenOptions.java` - CLI option definitions
- `ExecutionEventLogger.java` - Build event logging
- `ConsoleMavenTransferListener.java` - Progress meter
- `SimplexTransferListener.java` - Thread-safe wrapper
- `Slf4jMavenTransferListener.java` - Log file output

## Performance Considerations

1. **Single-threaded by default** - Safe choice, can be overridden
2. **Repository cache** - Enabled to avoid redundant lookups
3. **Recursive by default** - Processes all modules
4. **Fail-fast by default** - Stops on first failure (can change with options)
5. **Artifact descriptor caching** - Missing/invalid ignored (strict mode available)

## Thread Safety

- **ConsoleMavenTransferListener** - NOT thread-safe (wrapped in SimplexTransferListener)
- **ExecutionEventLogger** - Uses synchronized currentVisitedProjectCount for multi-project builds
- **ResidentMavenInvoker** - Uses ConcurrentHashMap for context storage

## Summary

MavenInvoker is a sophisticated orchestrator that:
1. Manages layered CLI/config file options
2. Sets up dependency injection container (Plexus)
3. Resolves settings, toolchains, and properties
4. Configures logging, terminal, and output streams
5. Builds MavenExecutionRequest from all inputs
6. Executes Maven core and handles results/errors
7. Provides rich error reporting with resume hints
8. Supports both fresh and resident modes
9. Integrates event-driven architecture throughout

The design emphasizes:
- Separation of concerns (parsing, setup, execution, error handling)
- Extensibility (listeners, event spies, property contributors)
- User experience (rich logging, helpful error messages)
- Resource management (proper cleanup and restoration)
