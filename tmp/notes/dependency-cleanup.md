# Maven Plugin Dependency Cleanup

## Problem  
The analyzer plugin pom.xml had several unused dependencies that were identified by Maven's dependency analyzer.

## Dependencies Removed
1. **commons-io** - Was declared as "test dependency to verify classpath capture" but never used
2. **maven-embedder** - Added to replace maven-project but not actually needed 
3. **maven-compat** - Declared for PluginVersionResolver but not used
4. **maven-artifact** - Declared for plugin artifact creation but not used
5. **maven-build-cache-extension** - Declared for cacheability detection but not used
6. **junit-jupiter** - Conflicted with kotlin-test, removed in favor of kotlin-test
7. **mockito-kotlin** - Not used, actual code uses mockito-core which is a transitive dependency
8. **kotlin-test-junit5** - Not used, replaced with kotlin-test

## Result
- Reduced pom.xml from 132 lines to 79 lines  
- Eliminated security vulnerabilities from unused transitive dependencies
- Build still passes successfully
- Cleaner, more maintainable dependency list