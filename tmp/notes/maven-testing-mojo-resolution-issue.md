# Maven Testing Framework Mojo Resolution Issue

## Problem
The `ParameterResolutionException: Unable to resolve mojo` error occurs because the Maven 4 testing framework cannot find the Mojo in the DI container.

## Root Cause
The testing framework looks for a Mojo with the full coordinates as the `@Named` value:
```
@Named("dev.nx.maven:nx-maven-analyzer-plugin:4.1.0-SNAPSHOT:analyze") Mojo
```

But our Mojo only has:
```kotlin
@Named("analyze")
```

## Investigation Findings
- The Maven 4 DI testing framework requires Mojos to be registered with their full coordinates
- The `MojoExtension.resolveParameter()` method calls `lookup(Mojo.class, coord[0] + ":" + coord[1] + ":" + coord[2] + ":" + coord[3])`
- This expects the DI system to have a binding for the full coordinate string

## Current Status
- Maven 4 testing framework is implemented but not working with current plugin structure
- The Mojo extends `AbstractMojo` which is Maven 3 style, not Maven 4 DI compatible
- Need to determine if Maven 4 plugin structure should be different from Maven 3

## Potential Solutions
1. Register Mojo with full coordinates in `@Named`
2. Modify plugin structure to be purely Maven 4 DI-based
3. Use traditional Maven testing approach instead of Maven 4 testing framework