# MavenInvoker Deep Dive - Complete Analysis

This directory contains comprehensive documentation of the MavenInvoker architecture and execution flow.

## Documents

### 1. maven-invoker-deep-dive.md (19 KB, 515 lines)
The comprehensive overview document covering:
- Architecture and class hierarchy
- Complete 4-phase execution flow
- Key design patterns (layered context, options layering, event-driven)
- Maven instance setup and container creation
- Request building from options to MavenExecutionRequest
- Error handling strategy
- Output/logging integration
- Version handling and compatibility
- ResidentMavenInvoker specialization
- Integration points with Maven API
- Performance considerations
- Thread safety notes

**Key Sections:**
- Overview, Architecture, Complete Execution Flow (4 phases)
- Key Design Patterns, Maven Instance Setup
- Request Building with detailed mapping table
- Error Handling Strategy, Output/Logging Integration
- Code Examples, Key Files, Summary

### 2. execution-flow-diagram.md (35 KB, 520 lines)
Visual flowcharts and detailed diagrams showing:
- High-level invocation flow (16 steps with ASCII art)
- populateRequest details with option-to-request mapping
- Error handling flow
- Listener chain architecture
- Transfer listener selection logic

**Key Sections:**
- High-Level Invocation Flow (16-step diagram with 4 phases)
- populateRequest Details (complete mapping)
- Error Handling Flow (exception → resume hints)
- Listener Chain Architecture (execution + transfer events)
- TransferListener Selection Logic

### 3. code-patterns-and-examples.md (21 KB, 601 lines)
Practical code examples showing actual implementation patterns:
1. Main Entry Point Pattern
2. Request Building Pattern
3. Options Parsing Pattern (layered)
4. Profile & Project Activation Pattern
5. Threading Configuration Pattern
6. Transfer Listener Selection Pattern
7. Execution Listener Setup Pattern
8. Error Handling Pattern
9. Recursive Exception Summary Logging
10. ResidentMavenInvoker Pattern
11. Checksum Policy Determination
12. Make Behavior Determination
13. Reactor Failure Behavior

Each pattern includes actual Java code from the codebase with explanations.

## Quick Reference Tables

### MavenOptions → MavenExecutionRequest Mapping
See: maven-invoker-deep-dive.md → "Request Building From Options" section

Maps all Maven CLI options to request properties, including:
- Reactor failure behavior (fail-fast, fail-at-end, fail-never)
- Checksum policies (strict, relaxed)
- Threading/concurrency options
- Profile and project activation
- Make behavior (upstream, downstream, both)

### Default Behaviors
- **Single-threaded** by default
- **Interactive mode** (unless CI or -B detected)
- **Fail-fast** by default
- **Repository cache** enabled
- **Artifact descriptors** ignored (unless --strict-artifact-descriptor-policy)

### Phase Overview

| Phase | Steps | Purpose |
|-------|-------|---------|
| 1: Environment | 9 steps | Setup properties, logging, terminal, validation |
| 2: Container | 7 steps | Create DI container, load extensions, setup lookup |
| 3: Execution | 4 steps | Prepare request, add toolchains, populate, execute |
| 4: Cleanup | 6 steps | Close containers, restore class loaders, cleanup |

## Key Architecture Insights

### Layered Context Model
```
LookupContext (base)
  ├─ Environment setup, logging, terminal
  ├─ Settings and toolchains
  ├─ Properties management
  └─ DI container management
       └─ MavenContext (extends)
            ├─ Maven instance reference
            ├─ Maven-specific options
            └─ Maven-specific execution
                 └─ ResidentMavenContext (specialized)
                      └─ Keeps container/Maven resident
```

### Options Layering (Priority Order)
1. CLI arguments (highest)
2. @file arguments
3. .mvn/maven.config (lowest)

### Event-Driven Architecture
- EventSpyDispatcher publishes lifecycle events
- ExecutionListener for build events
- TransferListener for artifact transfer events
- BuildEventListener for project-specific logging

### Error Handling
- DefaultExceptionHandler converts exceptions to ExceptionSummary
- Recursive logSummary preserves ANSI colors
- Resume hints calculated from failed projects
- Artifact ID collision detection for `-rf` selector

## Important Patterns

### Transfer Listener Selection
```
Conditions → Listener Type
quiet/noProgress/CI → QuietMavenTransferListener (silent)
interactive && !logFile → SimplexTransferListener(ConsoleMavenTransferListener) (progress)
else → Slf4jMavenTransferListener (file output)
```

### Threading Configuration
```
--threads 4      → 4 threads
--threads 1.5C   → 1.5 * availableProcessors threads
--builder custom → Use custom builder
```

### Reactor Selection (am/amd)
```
-pl module1          → only module1
-pl module1 -am      → module1 + dependencies
-pl module1 -amd     → module1 + dependents
-pl module1 -am -amd → module1 + both
```

### Failure Behavior
```
-ff (--fail-fast)   → stop immediately
-fae (--fail-at-end) → continue, report at end
-fn (--fail-never)  → continue, exit code 0
```

## File Locations

### Core Implementation Files
- `MavenInvoker.java` (443 lines) - Main orchestrator
- `MavenContext.java` (48 lines) - Maven-specific context
- `LookupInvoker.java` (934 lines) - Base invoker with DI setup
- `LookupContext.java` (156 lines) - Base context

### Supporting Files
- `MavenParser.java` - Options parsing with layering
- `CommonsCliMavenOptions.java` - CLI option definitions
- `ExecutionEventLogger.java` - Build event logging
- `ConsoleMavenTransferListener.java` - Progress meter
- `SimplexTransferListener.java` - Thread-safe wrapper
- `ResidentMavenInvoker.java` - Resident mode specialization

All files at: `/impl/maven-cli/src/main/java/org/apache/maven/cling/invoker/`

## Key Takeaways

1. **Sophisticated Orchestration**: MavenInvoker manages complex lifecycle from options parsing through container setup to build execution and cleanup.

2. **Separation of Concerns**: Each phase has specific responsibilities (environment, container, execution, cleanup).

3. **Event-Driven**: Rich event system for extensibility (EventSpy, ExecutionListener, TransferListener).

4. **Layered Design**: Context types extend for specialization (LookupContext → MavenContext → shadow contexts).

5. **User Experience**: Rich error reporting with resume hints, helpful suggestions, preserved ANSI colors.

6. **Resource Management**: Proper cleanup in reverse order with exception aggregation.

7. **Extensibility Points**: PropertyContributor SPI, EventSpy, Listeners, Container factories.

8. **Both Fresh and Resident Modes**: Supports both independent invocations and persistent Maven instances (e.g., mvnsh).

## Understanding the Flow

1. **Start** → invoke(InvokerRequest)
2. **Setup** → Environment, container, settings (phases 1-2)
3. **Execute** → Build Maven request, execute, handle results (phase 3)
4. **End** → Cleanup and restoration (phase 4)

Each phase is well-isolated with clear entry/exit points. Listeners and events provide visibility and extensibility throughout.

## Questions to Answer

### "How does it set up Maven?"
See: Architecture → Maven Instance Setup → Container Creation

### "How does it build the request from options?"
See: code-patterns-and-examples.md → Patterns 2-6

### "How are errors handled?"
See: execution-flow-diagram.md → Error Handling Flow
And: code-patterns-and-examples.md → Pattern 8

### "What happens during execution?"
See: execution-flow-diagram.md → High-Level Invocation Flow (Phase 3)

### "How are events published?"
See: execution-flow-diagram.md → Listener Chain Architecture

### "How does resident mode work?"
See: code-patterns-and-examples.md → Pattern 10
And: maven-invoker-deep-dive.md → ResidentMavenInvoker Specialization

## Summary Diagram

```
┌─────────────────────────────────────────────────────┐
│ Invoker.invoke(InvokerRequest)                      │
└──────────────────────┬──────────────────────────────┘
                       ↓
┌─ PHASE 1: Environment Setup (9 steps) ──────────────┐
│ Validate, properties, logging, terminal, help/version│
└──────────────────────┬──────────────────────────────┘
                       ↓
┌─ PHASE 2: Container & DI (7 steps) ─────────────────┐
│ Create container, load extensions, setup lookup      │
└──────────────────────┬──────────────────────────────┘
                       ↓
┌─ PHASE 3: Maven Execution (4 steps) ────────────────┐
│ Prepare request, toolchains, populate, execute       │
│ ├─ Maven.execute() → build runs with listeners       │
│ ├─ Listeners: ExecutionEventLogger, TransferListener│
│ └─ Error handling: exception summary, resume hints   │
└──────────────────────┬──────────────────────────────┘
                       ↓
┌─ PHASE 4: Cleanup (6 steps) ────────────────────────┐
│ Close containers, restore environment                │
└──────────────────────┬──────────────────────────────┘
                       ↓
                 ┌─────────────┐
                 │ Return Code │
                 │ (0 or 1+)   │
                 └─────────────┘
```

## Related Information

- **Maven Core**: Uses Maven.execute(MavenExecutionRequest)
- **API Surface**: ModelProcessor, SettingsBuilder, ToolchainsBuilder
- **DI Framework**: Plexus (PlexusContainer via PlexusContainerCapsule)
- **SLF4J Integration**: Logback or Log4j2 configuration
- **CLI Parsing**: Apache Commons CLI via CommonsCliMavenOptions
- **Artifact Transfer**: Aether/Eclipse Aether for downloads

