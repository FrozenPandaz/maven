# MavenInvoker Execution Flow Diagram

## High-Level Invocation Flow

```
┌─────────────────────────────────────────────────────────────┐
│ Invoker.invoke(InvokerRequest)                              │
│ Thread.currentThread().setContextClassLoader(container CL)   │
│ System.setProperties(save old props)                        │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ createContext(invokerRequest) → MavenContext                │
│ ├─ cwd, installationDirectory, userDirectory               │
│ ├─ Create ProtoSession with properties                     │
│ └─ Initialize logger (AccumulatingLogger initially)        │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ try (context)  /* AutoCloseable */                         │
│   └─ doInvoke(context)                                     │
└─────────────────────────────────────────────────────────────┘
                           ↓
        ╔═══════════════════════════════════════╗
        ║  PHASE 1: ENVIRONMENT SETUP           ║
        ╚═══════════════════════════════════════╝
                           ↓
        ┌─────────────────────────────────────┐
        │ 1. validate(context)                │
        │    Check parsing success            │
        │    Emit accumulated logs            │
        └─────────────────────────────────────┘
                           ↓
        ┌─────────────────────────────────────┐
        │ 2. pushCoreProperties()             │
        │    MAVEN_HOME → System.properties   │
        └─────────────────────────────────────┘
                           ↓
        ┌─────────────────────────────────────┐
        │ 3. pushUserProperties()             │
        │    User props → System.properties   │
        └─────────────────────────────────────┘
                           ↓
        ┌─────────────────────────────────────┐
        │ 4. setupGuiceClassLoading()         │
        │    guice_custom_class_loading=CHILD │
        └─────────────────────────────────────┘
                           ↓
        ┌─────────────────────────────────────┐
        │ 5. configureLogging()               │
        │    ├─ Color detection               │
        │    ├─ SLF4J config factory          │
        │    └─ Logger level setting          │
        └─────────────────────────────────────┘
                           ↓
        ┌─────────────────────────────────────┐
        │ 6. createTerminal()                 │
        │    ├─ Build terminal                │
        │    ├─ Configure streams             │
        │    └─ Setup message builder         │
        └─────────────────────────────────────┘
                           ↓
        ┌─────────────────────────────────────┐
        │ 7. activateLogging()                │
        │    ├─ SLF4J activation              │
        │    ├─ Drain accumulated logs        │
        │    └─ Swap logger instance          │
        └─────────────────────────────────────┘
                           ↓
        ┌─────────────────────────────────────┐
        │ 8. helpOrVersionAndMayExit()        │
        │    If --help or --version EXIT      │
        └─────────────────────────────────────┘
                           ↓
        ┌─────────────────────────────────────┐
        │ 9. preCommands()                    │
        │    Show version if -v or --version  │
        └─────────────────────────────────────┘
                           ↓
        ╔═══════════════════════════════════════╗
        ║  PHASE 2: CONTAINER & DI SETUP        ║
        ╚═══════════════════════════════════════╝
                           ↓
        ┌─────────────────────────────────────┐
        │ 10. container(context)              │
        │     ├─ Create PlexusContainerCapsule│
        │     ├─ Load core extensions         │
        │     └─ Setup Lookup service         │
        └─────────────────────────────────────┘
                           ↓
        ┌─────────────────────────────────────┐
        │ 11. postContainer(context)          │
        │     ├─ Run PropertyContributor SPI  │
        │     └─ Merge contributed properties │
        └─────────────────────────────────────┘
                           ↓
        ┌─────────────────────────────────────┐
        │ 12. pushUserProperties() [2nd time] │
        │     Refresh after SPI modifications │
        └─────────────────────────────────────┘
                           ↓
        ┌─────────────────────────────────────┐
        │ 13. lookup(context)                 │
        │     ├─ Get Maven instance           │
        │     └─ Get EventSpyDispatcher       │
        └─────────────────────────────────────┘
                           ↓
        ┌─────────────────────────────────────┐
        │ 14. init(context)                   │
        │     Fire EventSpy init event        │
        └─────────────────────────────────────┘
                           ↓
        ┌─────────────────────────────────────┐
        │ 15. postCommands(context)           │
        │     Log settings/schemes if verbose │
        └─────────────────────────────────────┘
                           ↓
        ┌─────────────────────────────────────┐
        │ 16. settings(context)               │
        │     ├─ Build SettingsBuilderRequest │
        │     ├─ SettingsBuilder.build()      │
        │     ├─ Determine local repository   │
        │     └─ Store effective settings     │
        └─────────────────────────────────────┘
                           ↓
        ╔═══════════════════════════════════════╗
        ║  PHASE 3: MAVEN EXECUTION             ║
        ╚═══════════════════════════════════════╝
                           ↓
        ┌─────────────────────────────────────┐
        │ MavenInvoker.execute(context)       │
        │ ├─ prepareMavenExecutionRequest()   │
        │ ├─ toolchains()                     │
        │ ├─ populateRequest()                │
        │ └─ doExecute()                      │
        └─────────────────────────────────────┘
                           ↓
        ┌─────────────────────────────────────┐
        │ prepareMavenExecutionRequest()      │
        │ Create DefaultMavenExecutionRequest │
        │ with defaults:                      │
        │ ├─ repository cache                │
        │ ├─ interactive mode                │
        │ ├─ recursive = true                │
        │ ├─ failure behavior = FAIL_FAST    │
        │ ├─ logging level = INFO            │
        │ └─ degree of concurrency = 1       │
        └─────────────────────────────────────┘
                           ↓
        ┌─────────────────────────────────────┐
        │ toolchains(context, request)        │
        │ ├─ Resolve toolchains files        │
        │ ├─ ToolchainsBuilder.build()        │
        │ └─ Set toolchains on request        │
        └─────────────────────────────────────┘
                           ↓
        ┌─────────────────────────────────────┐
        │ populateRequest(context, lookup, r) │
        │ ├─ Settings → request               │
        │ ├─ Goals → request                  │
        │ ├─ Options → request                │
        │ ├─ Listeners → request              │
        │ └─ Profile/Project activation       │
        └─────────────────────────────────────┘
                           ↓
        ┌─────────────────────────────────────────────┐
        │ doExecute(context, request)                 │
        │ ├─ eventSpyDispatcher.onEvent(request)     │
        │ ├─ result = maven.execute(request)         │
        │ ├─ eventSpyDispatcher.onEvent(result)      │
        │ ├─ eventSpyDispatcher.close()              │
        │ └─ Handle exceptions (if any)              │
        └─────────────────────────────────────────────┘
                           ↓
                ┌──────────────────────┐
                │ Has Exceptions?      │
                └──────────────────────┘
                    ↙            ↘
                  YES           NO
                   ↓             ↓
            ┌────────────┐  ┌─────────┐
            │ Error Flow │  │Return 0 │
            └────────────┘  └─────────┘
                   ↓
        ┌─────────────────────────────────┐
        │ For each exception:             │
        │ ├─ handleException()            │
        │ │  → ExceptionSummary           │
        │ ├─ logSummary()                 │
        │ │  (recursive, with ANSI)       │
        │ └─ Check LifecycleException     │
        │    → Calculate resume hint      │
        └─────────────────────────────────┘
                   ↓
        ┌─────────────────────────────────┐
        │ Log error messages:             │
        │ ├─ Suggest -e for stacktrace   │
        │ ├─ Suggest -X for verbose      │
        │ ├─ Log help references         │
        │ └─ Log resume hints            │
        └─────────────────────────────────┘
                   ↓
        ┌─────────────────────────────────┐
        │ Check failNever option:         │
        │ ├─ If true: Return 0            │
        │ └─ Else: Return 1               │
        └─────────────────────────────────┘
                           ↓
        ╔═══════════════════════════════════════╗
        ║  PHASE 4: CLEANUP                     ║
        ╚═══════════════════════════════════════╝
                           ↓
        ┌─────────────────────────────────────┐
        │ finally/try-with-resources          │
        │ context.close()                     │
        ├─ Close closeables (reverse order)   │
        ├─ closeContainer()                   │
        │  └─ Plexus container.close()       │
        │     ├─ Clear lookup                │
        │     ├─ Clear eventSpyDispatcher    │
        │     ├─ Clear maven                 │
        │     └─ MavenContext.maven = null   │
        ├─ Thread.currentThread().           │
        │  setContextClassLoader(oldCL)      │
        └─ System.setProperties(oldProps)    │
        └─────────────────────────────────────┘
                           ↓
        ┌─────────────────────────────────────┐
        │ Return exit code                    │
        │ (0 = success, 1+ = failure)         │
        └─────────────────────────────────────┘
```

## populateRequest Details (Options to Request Mapping)

```
INPUT: MavenOptions (from CLI, @file, maven.config)

┌─────────────────────────────────────────────────────────────┐
│ populateRequestFromSettings(request, settings)              │
├─ setOffline(settings.isOffline())                           │
├─ setInteractiveMode(settings.isInteractiveMode())           │
├─ setPluginGroups(settings.getPluginGroups())                │
├─ addServers(settings.getServers())                          │
├─ addProxies(settings.getProxies())                          │
├─ addMirrors(settings.getMirrors())                          │
└─ setRemoteRepositories(settings.getRepositories())          │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ From Options:                                               │
├─ setGoals(options.goals())                                  │
├─ setReactorFailureBehavior(                                 │
│   options.failFast()      ? REACTOR_FAIL_FAST              │
│   options.failAtEnd()     ? REACTOR_FAIL_AT_END            │
│   options.failNever()     ? REACTOR_FAIL_NEVER             │
│   :default                ? REACTOR_FAIL_FAST              │
├─ setRecursive(                                              │
│   !options.nonRecursive().orElse(false)                    │
├─ setOffline(options.offline().orElse(false))               │
├─ setUpdateSnapshots(options.updateSnapshots())             │
├─ setGlobalChecksumPolicy(                                   │
│   options.strictChecksums()  ? CHECKSUM_POLICY_FAIL        │
│   options.relaxedChecksums() ? CHECKSUM_POLICY_WARN        │
│   :default                   ? null                        │
├─ setNoSnapshotUpdates(options.suppressSnapshotUpdates())   │
├─ setResume(options.resume())                               │
├─ setResumeFrom(options.resumeFrom())                        │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ POM & Directories:                                          │
├─ setPom(determinePom(...))                                  │
│  └─ From options.alternatePomFile()                         │
│  └─ Via ModelProcessor.locateExistingPom()                  │
├─ setBaseDirectory(pom.getParent())                          │
├─ setMultiModuleProjectDirectory(rootDirectory)             │
└─ setRootDirectory(rootDirectory)                            │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ Listeners:                                                  │
├─ setTransferListener(determineTransferListener())           │
│  ├─ quiet || noTransferProgress || quietCI                 │
│  │  → QuietMavenTransferListener                           │
│  ├─ interactive && !logFile                                │
│  │  → SimplexTransferListener(                             │
│  │     ConsoleMavenTransferListener)                       │
│  └─ else                                                    │
│     → Slf4jMavenTransferListener                           │
│  All wrapped in:                                            │
│    MavenTransferListener(delegate, BuildEventListener)     │
│                                                             │
└─ setExecutionListener(determineExecutionListener())         │
   ├─ ExecutionEventLogger                                    │
   ├─ Optional EventSpyDispatcher chain                       │
   └─ Wrapped in LoggingExecutionListener with               │
      BuildEventListener                                     │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ Profile & Project Activation:                               │
├─ performProfileActivation(options.activatedProfiles())     │
│  └─ Parse: +profile (activate), -profile (deactivate)     │
│  └─ Parse: ?optional (optional, no error if missing)       │
│                                                             │
└─ performProjectActivation(options.projects())              │
   └─ Selectors: +project, -project, ?optional               │
   └─ Also supports: :artifactId, groupId:artifactId        │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ Concurrency & Builder:                                      │
├─ if options.threads().isPresent():                          │
│  ├─ calculateDegreeOfConcurrency(threadSpec)               │
│  │  ├─ If ends with 'C': float multiplier                  │
│  │  │  → degreeOfConcurrency = multiplier * processors    │
│  │  └─ Else: integer value                                │
│  │     → degreeOfConcurrency = value                      │
│  └─ if > 1: setBuilderId("multithreaded")                 │
│                                                             │
└─ if options.builder().isPresent():                          │
   └─ setBuilderId(options.builder())                        │
   └─ Overrides default/thread-based value                   │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ Make Behavior (reactor selection):                          │
├─ if alsoMake && !alsoMakeDependents:                        │
│  └─ setMakeBehavior(REACTOR_MAKE_UPSTREAM)                 │
├─ if !alsoMake && alsoMakeDependents:                        │
│  └─ setMakeBehavior(REACTOR_MAKE_DOWNSTREAM)               │
└─ if alsoMake && alsoMakeDependents:                         │
   └─ setMakeBehavior(REACTOR_MAKE_BOTH)                     │
└─────────────────────────────────────────────────────────────┘
```

## Error Handling Flow

```
Maven.execute(request) → MavenExecutionResult

┌──────────────────────────────────────────┐
│ MavenExecutionResult.getExceptions()     │
│ Empty? → return 0 (success)              │
│ Present? → enter error handling flow     │
└──────────────────────────────────────────┘
            ↓
┌──────────────────────────────────────────┐
│ For each Throwable exception:            │
├─ DefaultExceptionHandler.handleException │
│  → ExceptionSummary                      │
│     ├─ message: user-friendly            │
│     ├─ reference: help URL               │
│     ├─ exception: root cause             │
│     └─ children: nested ExceptionSummary │
│                                           │
└─ logSummary(context, summary, refs, ind)│
   (recursive function)                    │
└──────────────────────────────────────────┘
            ↓
┌──────────────────────────────────────────┐
│ Extract LifecycleExecutionException      │
│ → failedProjects.add(project)            │
└──────────────────────────────────────────┘
            ↓
┌──────────────────────────────────────────┐
│ Log error messages:                      │
├─ If NOT showErrors && NOT InternalError: │
│  ├─ "Re-run with -e for full stacktrace" │
│  ├─ "Re-run with -X for verbose output"  │
│  └─ Store all in references map          │
│                                           │
└─ For each reference:                    │
   └─ Log "[Help N] URL" format            │
└──────────────────────────────────────────┘
            ↓
┌──────────────────────────────────────────┐
│ Calculate Resume Hint:                   │
├─ If canResume():                         │
│  └─ Suggest: mvn [args] -r               │
├─ Else if !failedProjects.isEmpty():      │
│  ├─ getResumeFromSelector()              │
│  │  └─ Check for artifact ID collisions │
│  │  └─ Use groupId:artifactId if needed │
│  └─ Suggest: mvn [args] -rf selector     │
│                                           │
└─ logBuildResumeHint()                    │
└──────────────────────────────────────────┘
            ↓
┌──────────────────────────────────────────┐
│ Return Exit Code:                        │
├─ If options.failNever():                 │
│  └─ return 0 (ignore failures)           │
└─ Else:                                   │
   └─ return 1 (build failed)              │
└──────────────────────────────────────────┘
```

## Listener Chain Architecture

```
                    Maven Execution
                          ↓
                MavenExecutionRequest
                          ↓
        ┌──────────────────────────────┐
        │ Maven.execute()              │
        │ EventSpyDispatcher.onEvent() │
        └──────────────────────────────┘
                          ↓
        ╭─────────────────────────────────────╮
        │ During Build: Execution Events      │
        ╰─────────────────────────────────────╯
                          ↓
        ┌────────────────────────────────────────┐
        │ ExecutionListener (chain):             │
        ├─ ExecutionEventLogger                 │
        │  ├─ projectDiscoveryStarted()        │
        │  ├─ sessionStarted()                 │
        │  ├─ projectStarted()                 │
        │  ├─ mojoStarted()                    │
        │  ├─ mojoSucceeded/Failed/Skipped()   │
        │  ├─ projectSucceeded/Failed/Skipped()│
        │  ├─ sessionEnded()                   │
        │  └─ Outputs: build log, timing       │
        │                                       │
        ├─ Optional: EventSpyDispatcher.chain() │
        │  └─ Delegates to EventSpy listeners  │
        │                                       │
        └─ Wrapped in LoggingExecutionListener │
           └─ Injects: BuildEventListener      │
        └────────────────────────────────────────┘
                          ↓
        ╭─────────────────────────────────────╮
        │ During Build: Transfer Events       │
        ╰─────────────────────────────────────╯
                          ↓
        ┌────────────────────────────────────────┐
        │ TransferListener (selected based on):  │
        │                                        │
        │ IF quiet || noTransferProgress || CI:  │
        │  └─ QuietMavenTransferListener         │
        │     (silent, no output)                │
        │                                        │
        │ ELSE IF interactive && !logFile:       │
        │  └─ SimplexTransferListener            │
        │     └─ ConsoleMavenTransferListener    │
        │        └─ Progress meter to console   │
        │        (thread-safe wrapper)          │
        │                                        │
        │ ELSE:                                  │
        │  └─ Slf4jMavenTransferListener         │
        │     └─ Output to SLF4J loggers        │
        │     (for file logging)                │
        │                                        │
        │ Wrapped in MavenTransferListener       │
        │  └─ Injects: BuildEventListener       │
        └────────────────────────────────────────┘
                          ↓
                MavenExecutionResult
                          ↓
        ┌──────────────────────────────┐
        │ EventSpyDispatcher.onEvent() │
        │ (publish result)              │
        └──────────────────────────────┘
                          ↓
        ┌──────────────────────────────┐
        │ doExecute() returns:          │
        │ 0 = success                  │
        │ 1 = failure                  │
        │ (or 0 if failNever option)   │
        └──────────────────────────────┘
```

## TransferListener Selection Logic

```
determineTransferListener(context, noTransferProgress)
        ↓
    Evaluate conditions:
        ↓
    quiet = options.quiet()
    logFile = options.logFile().isPresent()
    quietCI = ciInfo.isPresent() && !forceInteractive()
        ↓
    ┌─────────────────────────────────────────┐
    │ IF quiet || noTransferProgress || quietCI│
    └─────────────────────────────────────────┘
               ↓
        delegate = QuietMavenTransferListener
        (no output)
               ↓
    ┌─────────────────────────────────────────┐
    │ ELSE IF interactive && !logFile         │
    └─────────────────────────────────────────┘
               ↓
        delegate = SimplexTransferListener(
            ConsoleMavenTransferListener(
                messageBuilderFactory,
                terminal.writer(),
                effectiveVerbose
            )
        )
        (console progress, thread-safe)
               ↓
    ┌─────────────────────────────────────────┐
    │ ELSE                                    │
    └─────────────────────────────────────────┘
               ↓
        delegate = Slf4jMavenTransferListener
        (SLF4J/logfile output)
               ↓
    Return: MavenTransferListener(
        delegate,
        determineBuildEventListener(context)
    )
    (wraps with BuildEventListener)
```

