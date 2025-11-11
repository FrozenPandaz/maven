# MavenInvoker: Code Patterns and Examples

## 1. Main Entry Point Pattern

### Basic Structure
```java
// In MavenCling.java or main entry point
Lookup protoLookup = ... // Bootstrap lookup from classpath
MavenInvoker invoker = new MavenInvoker(protoLookup, contextConsumer);
int exitCode = invoker.invoke(invokerRequest);
System.exit(exitCode);
```

### With Context Consumer
```java
// Setup context before execution (e.g., for testing or special setup)
MavenInvoker invoker = new MavenInvoker(protoLookup, context -> {
    // context is LookupContext at this point
    // Can examine or modify context state
    context.coloredOutput = false;  // Force no colors
});
int exitCode = invoker.invoke(invokerRequest);
```

## 2. Request Building Pattern

### Building MavenExecutionRequest
```java
// From MavenInvoker.prepareMavenExecutionRequest()
private MavenExecutionRequest prepareMavenExecutionRequest() throws Exception {
    DefaultMavenExecutionRequest request = new DefaultMavenExecutionRequest();
    
    // Set caching
    request.setRepositoryCache(new DefaultRepositoryCache());
    
    // Set defaults
    request.setInteractiveMode(true);
    request.setCacheTransferError(false);
    request.setIgnoreInvalidArtifactDescriptor(true);
    request.setIgnoreMissingArtifactDescriptor(true);
    request.setRecursive(true);
    request.setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_FAST);
    request.setStartInstant(MonotonicClock.now());
    request.setLoggingLevel(MavenExecutionRequest.LOGGING_LEVEL_INFO);
    request.setDegreeOfConcurrency(1);
    request.setBuilderId("singlethreaded");
    
    return request;
}
```

### Populating from Options
```java
// From MavenInvoker.populateRequest()
protected void populateRequest(MavenContext context, Lookup lookup, 
                               MavenExecutionRequest request) throws Exception {
    super.populateRequest(context, lookup, request);
    
    // Goals
    request.setGoals(context.options().goals().orElse(List.of()));
    
    // Reactor failure behavior
    if (context.options().failFast().isPresent()) {
        request.setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_FAST);
    } else if (context.options().failAtEnd().isPresent()) {
        request.setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_AT_END);
    } else if (context.options().failNever().isPresent()) {
        request.setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_NEVER);
    } else {
        request.setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_FAST);
    }
    
    // Offline
    request.setOffline(context.options().offline().orElse(request.isOffline()));
    
    // Snapshot updates
    request.setUpdateSnapshots(context.options().updateSnapshots().orElse(false));
    request.setNoSnapshotUpdates(
        context.options().suppressSnapshotUpdates().orElse(false));
    
    // Checksum policy
    request.setGlobalChecksumPolicy(determineGlobalChecksumPolicy(context));
    
    // POM file
    Path pom = determinePom(context, lookup);
    if (pom != null) {
        request.setPom(pom.toFile());
        if (pom.getParent() != null) {
            request.setBaseDirectory(pom.getParent().toFile());
        }
    }
    
    // Listeners
    request.setTransferListener(
        determineTransferListener(context, 
            context.options().noTransferProgress().orElse(false)));
    request.setExecutionListener(determineExecutionListener(context));
}
```

## 3. Options Parsing Pattern

### Layered Options (CLI → @file → maven.config)
```java
// From MavenParser.parseCliOptions()
protected Options parseCliOptions(LocalContext context) {
    ArrayList<MavenOptions> result = new ArrayList<>();
    
    // 1. Parse CLI arguments
    MavenOptions cliOptions = parseMavenCliOptions(context.parserRequest.args());
    result.add(cliOptions);
    
    // 2. Parse @file if present
    if (cliOptions.atFile().isPresent()) {
        Path file = context.cwd.resolve(cliOptions.atFile().orElseThrow());
        if (Files.isRegularFile(file)) {
            result.add(parseMavenAtFileOptions(file));
        } else {
            throw new IllegalArgumentException("File does not exist: " + file);
        }
    }
    
    // 3. Parse maven.config if exists
    Path mavenConfig = context.rootDirectory != null 
        ? context.rootDirectory.resolve(".mvn/maven.config") 
        : null;
    if (mavenConfig != null && Files.isRegularFile(mavenConfig)) {
        result.add(parseMavenConfigOptions(mavenConfig));
    }
    
    // Combine layers
    return LayeredMavenOptions.layerMavenOptions(result);
}

// Parse file arguments (removing comments)
protected MavenOptions parseMavenConfigOptions(Path configFile) {
    try (Stream<String> lines = Files.lines(configFile, Charset.defaultCharset())) {
        List<String> args = lines
            .filter(arg -> !arg.isEmpty() && !arg.startsWith("#"))
            .toList();
        MavenOptions options = parseArgs("maven.config", args);
        
        // Validate: maven.config cannot contain goals
        if (options.goals().isPresent()) {
            throw new IllegalArgumentException(
                "Unrecognized entries in maven.config: " + options.goals().get());
        }
        return options;
    } catch (IOException e) {
        throw new IllegalStateException("Error reading config file: " + configFile, e);
    }
}
```

## 4. Profile & Project Activation Pattern

### Parsing Activation Selectors
```java
// From MavenInvoker.performProfileActivation()
protected void performProfileActivation(MavenContext context, 
                                       ProfileActivation profileActivation) {
    if (context.options().activatedProfiles().isPresent()) {
        List<String> optionValues = context.options().activatedProfiles().get();
        
        for (final String optionValue : optionValues) {
            for (String token : optionValue.split(",")) {
                String profileId = token.trim();
                boolean active = true;
                
                // Parse prefix
                if (!profileId.isEmpty()) {
                    if (profileId.charAt(0) == '-' || profileId.charAt(0) == '!') {
                        active = false;
                        profileId = profileId.substring(1);
                    } else if (token.charAt(0) == '+') {
                        profileId = profileId.substring(1);
                    }
                }
                
                // Parse optional marker
                boolean optional = false;
                if (!profileId.isEmpty() && profileId.charAt(0) == '?') {
                    optional = true;
                    profileId = profileId.substring(1);
                }
                
                profileActivation.addProfileActivation(profileId, active, optional);
            }
        }
    }
}

// Usage:
// -P profile1,profile2        → activate both
// -P +profile1,-profile2      → activate 1, deactivate 2
// -P ?optional,required       → optional doesn't fail if missing
```

## 5. Threading Configuration Pattern

### Thread Calculation
```java
// From MavenInvoker.populateRequest()
if (context.options().threads().isPresent()) {
    int degreeOfConcurrency = calculateDegreeOfConcurrency(
        context.options().threads().get());
    if (degreeOfConcurrency > 1) {
        request.setBuilderId("multithreaded");
        request.setDegreeOfConcurrency(degreeOfConcurrency);
    }
}

// From LookupInvoker.calculateDegreeOfConcurrency()
protected int calculateDegreeOfConcurrency(String threadConfiguration) {
    try {
        if (threadConfiguration.endsWith("C")) {
            // Core multiplier format: "1.5C"
            String str = threadConfiguration.substring(0, 
                threadConfiguration.length() - 1);
            float coreMultiplier = Float.parseFloat(str);
            
            if (coreMultiplier <= 0.0f) {
                throw new IllegalArgumentException(
                    "Value must be positive: " + threadConfiguration);
            }
            
            int procs = Runtime.getRuntime().availableProcessors();
            int threads = (int) (coreMultiplier * procs);
            return threads == 0 ? 1 : threads;
        } else {
            // Absolute thread count
            int threads = Integer.parseInt(threadConfiguration);
            if (threads <= 0) {
                throw new IllegalArgumentException(
                    "Value must be positive: " + threadConfiguration);
            }
            return threads;
        }
    } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "Invalid format. Supported: int or float ending with 'C'", e);
    }
}
```

## 6. Transfer Listener Selection Pattern

### Choosing Transfer Listener
```java
// From MavenInvoker.determineTransferListener()
protected TransferListener determineTransferListener(MavenContext context, 
                                                    boolean noTransferProgress) {
    boolean quiet = context.options().quiet().orElse(false);
    boolean logFile = context.options().logFile().isPresent();
    boolean quietCI = context.invokerRequest.ciInfo().isPresent()
        && !context.options().forceInteractive().orElse(false);
    
    TransferListener delegate;
    
    if (quiet || noTransferProgress || quietCI) {
        // Silent mode
        delegate = new QuietMavenTransferListener();
    } else if (context.interactive && !logFile) {
        // Interactive console with progress bar
        SimplexTransferListener simplex = new SimplexTransferListener(
            new ConsoleMavenTransferListener(
                context.invokerRequest.messageBuilderFactory(),
                context.terminal.writer(),
                context.invokerRequest.effectiveVerbose()));
        context.closeables.add(simplex);
        delegate = simplex;
    } else {
        // File or batch mode - use SLF4J
        delegate = new Slf4jMavenTransferListener();
    }
    
    // Wrap with BuildEventListener
    return new MavenTransferListener(delegate, 
        determineBuildEventListener(context));
}
```

## 7. Execution Listener Setup Pattern

### Setting Up Listener Chain
```java
// From MavenInvoker.determineExecutionListener()
protected ExecutionListener determineExecutionListener(MavenContext context) {
    // Base logger
    ExecutionListener listener = new ExecutionEventLogger(
        context.invokerRequest.messageBuilderFactory());
    
    // Chain with EventSpy if present
    if (context.eventSpyDispatcher != null) {
        listener = context.eventSpyDispatcher.chainListener(listener);
    }
    
    // Wrap with logging and build event listener
    return new LoggingExecutionListener(listener, 
        determineBuildEventListener(context));
}
```

## 8. Error Handling Pattern

### Exception Summary Logging
```java
// From MavenInvoker.doExecute()
if (result.hasExceptions()) {
    ExceptionHandler handler = new DefaultExceptionHandler();
    Map<String, String> references = new LinkedHashMap<>();
    List<MavenProject> failedProjects = new ArrayList<>();
    
    // Handle each exception
    for (Throwable exception : result.getExceptions()) {
        ExceptionSummary summary = handler.handleException(exception);
        logSummary(context, summary, references, "");
        
        // Extract failed project if available
        if (exception instanceof LifecycleExecutionException le) {
            failedProjects.add(le.getProject());
        }
    }
    
    // Emit suggestions
    context.logger.error("");
    
    if (!context.options().showErrors().orElse(false)) {
        context.logger.error("Re-run Maven with the '" 
            + MessageUtils.builder().strong("-e") + "' switch");
    }
    
    if (!context.invokerRequest.effectiveVerbose()) {
        context.logger.error("Re-run Maven with the '" 
            + MessageUtils.builder().strong("-X") + "' switch");
    }
    
    // Log help references
    if (!references.isEmpty()) {
        context.logger.error("");
        context.logger.error("For more information about the errors...");
        for (Map.Entry<String, String> entry : references.entrySet()) {
            context.logger.error(
                MessageUtils.builder().strong(entry.getValue()) 
                + " " + entry.getKey());
        }
    }
    
    // Calculate resume hint
    if (result.canResume()) {
        logBuildResumeHint(context, "mvn [args] -r");
    } else if (!failedProjects.isEmpty()) {
        List<MavenProject> sortedProjects = result.getTopologicallySortedProjects();
        failedProjects.sort(comparing(sortedProjects::indexOf));
        
        MavenProject firstFailedProject = failedProjects.get(0);
        if (!firstFailedProject.equals(sortedProjects.get(0))) {
            String selector = getResumeFromSelector(sortedProjects, 
                firstFailedProject);
            logBuildResumeHint(context, "mvn [args] -rf " + selector);
        }
    }
    
    return context.options().failNever().orElse(false) ? 0 : 1;
} else {
    return 0;
}
```

### Artifact ID Collision Detection
```java
// From MavenInvoker.getResumeFromSelector()
protected String getResumeFromSelector(List<MavenProject> mavenProjects, 
                                      MavenProject firstFailedProject) {
    boolean hasOverlappingArtifactId = mavenProjects.stream()
        .filter(project -> 
            firstFailedProject.getArtifactId()
                .equals(project.getArtifactId()))
        .count() > 1;
    
    if (hasOverlappingArtifactId) {
        // Use groupId:artifactId to disambiguate
        return firstFailedProject.getGroupId() + ":" 
            + firstFailedProject.getArtifactId();
    }
    
    // Use :artifactId (simpler)
    return ":" + firstFailedProject.getArtifactId();
}
```

## 9. Recursive Exception Summary Logging

### ANSI Color Preservation
```java
// From MavenInvoker.logSummary()
protected static final Pattern NEXT_LINE = Pattern.compile("\r?\n");
protected static final Pattern LAST_ANSI_SEQUENCE = 
    Pattern.compile("(\u001B\\[[;\\d]*[ -/]*[@-~])[^\u001B]*$");
protected static final String ANSI_RESET = "\u001B\u005Bm";

protected void logSummary(MavenContext context, ExceptionSummary summary, 
                         Map<String, String> references, String indent) {
    String referenceKey = "";
    
    // Assign reference number
    if (summary.getReference() != null && !summary.getReference().isEmpty()) {
        referenceKey = references.computeIfAbsent(summary.getReference(),
            k -> "[Help " + (references.size() + 1) + "]");
    }
    
    String msg = summary.getMessage();
    
    // Append reference
    if (!referenceKey.isEmpty()) {
        if (msg.indexOf('\n') < 0) {
            msg += " -> " + MessageUtils.builder().strong(referenceKey);
        } else {
            msg += "\n-> " + MessageUtils.builder().strong(referenceKey);
        }
    }
    
    // Split and process lines preserving ANSI color
    String[] lines = NEXT_LINE.split(msg);
    String currentColor = "";
    
    for (int i = 0; i < lines.length; i++) {
        String line = currentColor + lines[i];
        
        // Find last ANSI sequence
        Matcher matcher = LAST_ANSI_SEQUENCE.matcher(line);
        String nextColor = "";
        if (matcher.find()) {
            nextColor = matcher.group(1);
            if (ANSI_RESET.equals(nextColor)) {
                nextColor = "";
            }
        }
        
        // Add reset if needed
        line = indent + line + ("".equals(nextColor) ? "" : ANSI_RESET);
        
        // Log with or without exception
        if ((i == lines.length - 1) 
            && (context.options().showErrors().orElse(false)
                || (summary.getException() instanceof InternalErrorException))) {
            context.logger.error(line, summary.getException());
        } else {
            context.logger.error(line);
        }
        
        currentColor = nextColor;
    }
    
    // Recursively log children
    indent += "  ";
    for (ExceptionSummary child : summary.getChildren()) {
        logSummary(context, child, references, indent);
    }
}
```

## 10. ResidentMavenInvoker Pattern

### Sharing Maven Instance
```java
// From ResidentMavenInvoker
public class ResidentMavenInvoker extends MavenInvoker {
    private final ConcurrentHashMap<String, MavenContext> residentContext;
    
    @Override
    protected MavenContext createContext(InvokerRequest invokerRequest) {
        // Compute resident context once
        MavenContext result = residentContext.computeIfAbsent(
            "resident",
            k -> new MavenContext(invokerRequest, false, 
                (MavenOptions) invokerRequest.options().orElse(null)));
        
        // Create shadow context if invokerRequest differs
        return copyIfDifferent(result, invokerRequest);
    }
    
    protected MavenContext copyIfDifferent(MavenContext mavenContext, 
                                          InvokerRequest invokerRequest) {
        if (invokerRequest == mavenContext.invokerRequest) {
            // Same request, reuse context
            return mavenContext;
        }
        
        // Create shadow context
        MavenContext shadow = new MavenContext(
            invokerRequest, false, 
            (MavenOptions) invokerRequest.options().orElse(null));
        
        // Carry over resident components (shared across invocations)
        shadow.containerCapsule = mavenContext.containerCapsule;
        shadow.lookup = mavenContext.lookup;
        shadow.eventSpyDispatcher = mavenContext.eventSpyDispatcher;
        shadow.maven = mavenContext.maven;
        
        return shadow;
    }
    
    @Override
    public void close() throws InvokerException {
        ArrayList<Exception> exceptions = new ArrayList<>();
        for (MavenContext context : residentContext.values()) {
            try {
                context.doCloseContainer();
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            InvokerException exception = 
                new InvokerException("Could not cleanly shut down context pool");
            exceptions.forEach(exception::addSuppressed);
            throw exception;
        }
    }
}
```

## 11. Checksum Policy Determination

### Checksum Policy Selection
```java
// From MavenInvoker.determineGlobalChecksumPolicy()
protected String determineGlobalChecksumPolicy(MavenContext context) {
    if (context.options().strictChecksums().orElse(false)) {
        // Fail on invalid checksum
        return MavenExecutionRequest.CHECKSUM_POLICY_FAIL;
    } else if (context.options().relaxedChecksums().orElse(false)) {
        // Warn on invalid checksum
        return MavenExecutionRequest.CHECKSUM_POLICY_WARN;
    } else {
        // Use Maven default (usually warn)
        return null;
    }
}
```

## 12. Make Behavior Determination

### Reactor Selection (am/amd)
```java
// From MavenInvoker.determineMakeBehavior()
protected String determineMakeBehavior(MavenContext context) {
    boolean alsoMake = context.options().alsoMake().isPresent();
    boolean alsoMakeDependents = 
        context.options().alsoMakeDependents().isPresent();
    
    if (alsoMake && !alsoMakeDependents) {
        // Build selected + their dependencies
        return MavenExecutionRequest.REACTOR_MAKE_UPSTREAM;
    } else if (!alsoMake && alsoMakeDependents) {
        // Build selected + modules depending on them
        return MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM;
    } else if (alsoMake && alsoMakeDependents) {
        // Build selected + both up and downstream
        return MavenExecutionRequest.REACTOR_MAKE_BOTH;
    } else {
        // No restriction
        return null;
    }
}

// Usage:
// -pl module1,module2         → only these modules
// -pl module1,module2 -am     → + their dependencies
// -pl module1,module2 -amd    → + modules depending on them
// -pl module1,module2 -am -amd → + both
```

## 13. Reactor Failure Behavior

### Failure Strategy Selection
```java
// From MavenInvoker.determineReactorFailureBehaviour()
protected String determineReactorFailureBehaviour(MavenContext context) {
    if (context.options().failFast().isPresent()) {
        // Stop at first failure (default, fastest)
        return MavenExecutionRequest.REACTOR_FAIL_FAST;
    } else if (context.options().failAtEnd().isPresent()) {
        // Build all, report all failures at end
        return MavenExecutionRequest.REACTOR_FAIL_AT_END;
    } else if (context.options().failNever().isPresent()) {
        // Continue building, ignore failures
        return MavenExecutionRequest.REACTOR_FAIL_NEVER;
    } else {
        // Default
        return MavenExecutionRequest.REACTOR_FAIL_FAST;
    }
}

// Usage:
// -ff (--fail-fast)        → stop immediately
// -fae (--fail-at-end)     → continue, report at end
// -fn (--fail-never)       → continue, return 0 even on failure
```

