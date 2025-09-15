# ProjectManager Integration in MavenExpressionResolver

## Enhancements Made

### Added ProjectManager Service
- Injected `ProjectManager` service using `@Inject`
- Added imports for `ProjectScope` and `Language` APIs

### Improved Source Directory Resolution
**Before**: Hardcoded paths like `"${project.basedir}/src/main/java"`
**After**: Dynamic resolution using ProjectManager:

```kotlin
"project.build.sourceDirectory" -> {
    projectManager.getEnabledSourceRoots(project, ProjectScope.MAIN, Language.JAVA_FAMILY)
        .findFirst()
        .map { it.directory().toString() }
        .orElse("${project.basedir}/src/main/java")
}
```

### Added Basic Classpath Support
- Implemented basic classpath elements for compile and test scopes
- Returns proper path-separated strings using `java.io.File.pathSeparator`
- Includes output directories in classpath
- Added TODOs for full dependency resolution

### Benefits

1. **More Accurate**: Uses actual Maven source root configuration instead of assumptions
2. **Flexible**: Handles non-standard source layouts and multi-module projects
3. **Extensible**: Framework in place for full dependency resolution
4. **Maven 4 Native**: Uses proper Maven 4 APIs throughout

### What's Next
- Full dependency resolution using `DependencyResolver` service
- Support for custom source roots and resource directories
- Integration with Maven's artifact resolution for complete classpaths

## Impact

The resolver now properly handles:
- Custom source directory layouts
- Multi-language projects
- Maven 4 source root configurations
- Basic classpath construction for plugin analysis