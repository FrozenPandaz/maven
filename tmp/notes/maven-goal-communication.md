# Maven Goal Communication Mechanisms Beyond Artifacts

Maven goals communicate with each other within a session through several mechanisms beyond just producing and consuming artifacts:

## 1. Session Data (SessionData API)

**Location**: `Session.getData()` returns a `SessionData` instance
**Pattern**: Key-value storage with typed keys
**Usage**: Goals can store and retrieve arbitrary data that persists for the entire session

```java
// Store data
SessionData.Key<MyData> key = SessionData.key(MyData.class, "identifier");
session.getData().set(key, myData);

// Retrieve data
MyData data = session.getData().get(key);
```

**Thread Safety**: SessionData implementations are thread-safe
**Examples Found**:
- Toolchain management stores toolchain contexts
- Request caching stores computed values
- Relocation configurations

## 2. Plugin Context per Project

**Location**: `Session.getPluginContext(Project project)`
**Pattern**: Map-based storage scoped to specific project
**Usage**: Goals can share state within the same plugin execution for a project

```java
Map<String, Object> context = session.getPluginContext(project);
context.put("shared-state", value);
```

**Scope**: Per-project, per-plugin execution
**Thread Safety**: Returns ConcurrentMap implementation

## 3. Execution Events and Listeners

**Location**: `Session.registerListener(Listener listener)`
**Pattern**: Event-driven communication
**Usage**: Goals can listen to lifecycle events and react accordingly

**Event Types**:
- SessionStarted/SessionEnded
- ProjectStarted/ProjectSucceeded/ProjectFailed
- MojoStarted/MojoSucceeded/MojoFailed
- ForkStarted/ForkSucceeded/ForkFailed

## 4. System and User Properties

**Location**: `Session.getSystemProperties()`, `Session.getUserProperties()`
**Pattern**: Property-based configuration sharing
**Usage**: Goals can read properties set by previous goals or system configuration

## 5. Project Model Modifications

**Location**: Through the `Project` object in session
**Pattern**: Direct model manipulation
**Usage**: Goals can modify project metadata that subsequent goals will see

## Communication Flow Summary

1. **Session Data** - Global session storage, like a blackboard pattern
2. **Plugin Context** - Project-scoped plugin storage
3. **Event System** - Reactive communication through listeners
4. **Properties** - Configuration and state sharing
5. **Project Model** - Direct project metadata sharing

These mechanisms work together to enable rich inter-goal communication beyond just file-based artifacts.