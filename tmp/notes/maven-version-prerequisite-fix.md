# Maven Version Prerequisite Fix

## Problem
The nx-maven-analyzer-plugin was failing with error:
```
The plugin dev.nx.maven:nx-maven-analyzer-plugin:4.1.0-SNAPSHOT has unmet prerequisites: Required Maven version 4.1.0-SNAPSHOT is not met by current version 4.0.0-rc-3
```

## Root Cause
The maven-plugin-plugin automatically sets `requiredMavenVersion` in the generated plugin.xml file based on the project's Maven version (4.1.0-SNAPSHOT in this case).

## Solution
Added `<requiredMavenVersion>3.6.0</requiredMavenVersion>` to the maven-plugin-plugin configuration in the pom.xml:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-plugin-plugin</artifactId>
  <version>3.11.0</version>
  <configuration>
    <goalPrefix>nx-maven</goalPrefix>
    <requiredMavenVersion>3.6.0</requiredMavenVersion>
  </configuration>
</plugin>
```

## Files Changed
- `/packages/maven/analyzer-plugin/pom.xml` - Added requiredMavenVersion configuration
- Plugin regenerated with compatible Maven version requirement (3.6.0 instead of 4.1.0-SNAPSHOT)

## Verification
Successfully ran the analyzer plugin:
```bash
mvn dev.nx.maven:nx-maven-analyzer-plugin:4.1.0-SNAPSHOT:analyze
```

The plugin now works with Maven 4.0.0-rc-3 and other compatible versions.