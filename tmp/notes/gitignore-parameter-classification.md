# Using .gitignore for Parameter Classification

## Key Insight
**Inputs are typically NOT gitignored, while outputs ARE gitignored**

This is a brilliant heuristic because:
- Source files (inputs) are tracked in version control
- Build artifacts (outputs) are excluded from version control
- The `.gitignore` file already encodes this knowledge

## Common .gitignore Patterns

### Typically Ignored (Outputs)
- `**/target/**` - Maven build output directory
- `dist` - Distribution/build output
- `*.class` - Compiled Java files
- `*.jar`, `*.war`, `*.ear` - Built artifacts
- `.nx/cache` - Build cache
- `node_modules` - Downloaded dependencies

### Typically NOT Ignored (Inputs)
- `src/**` - Source code
- `pom.xml` - Project configuration
- `*.java`, `*.kt` - Source files
- `*.xml`, `*.properties` - Configuration files
- `*.proto` - Protocol buffer definitions

## Implementation Strategy

### 1. Simple Pattern Matching
```kotlin
class GitIgnoreParameterClassifier(
    private val projectRoot: File
) {
    private val gitIgnorePatterns = loadGitIgnorePatterns()
    
    fun classifyParameter(path: String): ParameterRole {
        val file = File(path)
        
        // Check if path matches gitignore patterns
        val isIgnored = matchesGitIgnore(file)
        
        return when {
            isIgnored -> ParameterRole.OUTPUT  // Ignored = likely output
            !isIgnored -> ParameterRole.INPUT  // Not ignored = likely input
            else -> ParameterRole.UNKNOWN
        }
    }
    
    private fun matchesGitIgnore(file: File): Boolean {
        val relativePath = file.relativeTo(projectRoot).path
        
        // Check common patterns
        return when {
            relativePath.startsWith("target/") -> true
            relativePath.startsWith("build/") -> true
            relativePath.startsWith("dist/") -> true
            relativePath.startsWith("src/") -> false
            relativePath.endsWith(".java") -> false
            relativePath.endsWith(".class") -> true
            else -> checkAgainstPatterns(relativePath)
        }
    }
}
```

### 2. Using JGit Library (More Accurate)
```kotlin
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

class JGitIgnoreChecker(projectRoot: File) {
    private val repository: Repository = FileRepositoryBuilder()
        .setGitDir(File(projectRoot, ".git"))
        .readEnvironment()
        .findGitDir()
        .build()
    
    private val git = Git(repository)
    
    fun isIgnored(path: String): Boolean {
        val status = git.status()
            .addPath(path)
            .call()
        
        return status.ignoredNotInIndex.contains(path)
    }
}
```

### 3. Lightweight Pattern Matcher (No Dependencies)
```kotlin
class SimpleGitIgnoreMatcher(
    private val projectRoot: File
) {
    private val patterns = mutableListOf<GitIgnorePattern>()
    
    init {
        loadGitIgnoreFile()
    }
    
    private fun loadGitIgnoreFile() {
        val gitIgnoreFile = File(projectRoot, ".gitignore")
        if (gitIgnoreFile.exists()) {
            gitIgnoreFile.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    patterns.add(GitIgnorePattern(trimmed))
                }
            }
        }
    }
    
    fun isIgnored(path: String): Boolean {
        val relativePath = File(path).relativeTo(projectRoot).path
        return patterns.any { it.matches(relativePath) }
    }
}

data class GitIgnorePattern(val pattern: String) {
    private val isNegation = pattern.startsWith("!")
    private val actualPattern = if (isNegation) pattern.substring(1) else pattern
    
    fun matches(path: String): Boolean {
        // Simplified glob matching
        val regex = actualPattern
            .replace("**", ".*")
            .replace("*", "[^/]*")
            .replace("?", "[^/]")
        
        val matches = path.matches(Regex(regex))
        return if (isNegation) !matches else matches
    }
}
```

## Integration with PhaseAnalyzer

### Enhanced Parameter Role Detection
```kotlin
private fun analyzeParameterRole(
    parameter: Parameter, 
    project: MavenProject
): ParameterRole {
    val name = parameter.name
    val type = parameter.type
    val expression = parameter.expression ?: parameter.defaultValue ?: ""
    val description = parameter.description?.lowercase() ?: ""
    
    // First, try to resolve the actual path
    val resolvedPath = expressionResolver.resolveParameterValue(
        name, 
        parameter.defaultValue,
        expression,
        project
    )
    
    // NEW: Check gitignore status if we have a resolved path
    if (resolvedPath != null) {
        val gitIgnoreRole = checkGitIgnoreStatus(resolvedPath, project)
        if (gitIgnoreRole != ParameterRole.UNKNOWN) {
            log.debug("Parameter $name: Gitignore check suggests $gitIgnoreRole")
            return gitIgnoreRole
        }
    }
    
    // Fall back to existing analysis methods...
    // (expression analysis, type analysis, description analysis)
}

private fun checkGitIgnoreStatus(
    path: String, 
    project: MavenProject
): ParameterRole {
    val file = File(path)
    val projectRoot = File(project.basedir)
    
    // Quick heuristic checks first
    val relativePath = try {
        file.relativeTo(projectRoot).path
    } catch (e: IllegalArgumentException) {
        // Path is outside project
        return ParameterRole.UNKNOWN
    }
    
    // Common patterns (fast check without reading .gitignore)
    return when {
        // Definitely outputs (commonly gitignored)
        relativePath.startsWith("target/") -> ParameterRole.OUTPUT
        relativePath.startsWith("build/") -> ParameterRole.OUTPUT
        relativePath.contains("/target/") -> ParameterRole.OUTPUT
        relativePath.endsWith(".class") -> ParameterRole.OUTPUT
        relativePath.endsWith(".jar") -> ParameterRole.OUTPUT
        
        // Definitely inputs (never gitignored)
        relativePath.startsWith("src/main/") -> ParameterRole.INPUT
        relativePath.startsWith("src/test/") -> ParameterRole.INPUT
        relativePath == "pom.xml" -> ParameterRole.INPUT
        relativePath.endsWith(".java") -> ParameterRole.INPUT
        relativePath.endsWith(".kt") -> ParameterRole.INPUT
        
        // Uncertain - would need actual .gitignore parsing
        else -> ParameterRole.UNKNOWN
    }
}
```

## Benefits of This Approach

1. **Leverages Existing Knowledge**: .gitignore already encodes what's version-controlled vs generated
2. **Project-Specific**: Each project's .gitignore reflects its specific structure
3. **Low False Positives**: Strong correlation between gitignore status and I/O role
4. **Fast Heuristics**: Can use common patterns without parsing .gitignore
5. **Fallback Strategy**: Use as additional signal, not sole determinant

## Caveats

1. **Not Always Perfect**:
   - Some inputs might be gitignored (e.g., local config files)
   - Some outputs might not be gitignored (e.g., generated documentation)

2. **Performance Considerations**:
   - Full .gitignore parsing can be expensive
   - Use fast heuristics for common cases

3. **Missing .gitignore**:
   - Fall back to standard patterns
   - Use Maven conventions (src=input, target=output)

## Recommended Implementation

1. **Start Simple**: Use common pattern matching without parsing .gitignore
2. **Add as Signal**: Use gitignore status as one of multiple signals
3. **Cache Results**: Parse .gitignore once and cache patterns
4. **Optional JGit**: Add JGit dependency only if high accuracy needed

## Priority Order for Parameter Classification

1. **Explicit Maven expressions** (${project.build.sourceDirectory} = INPUT)
2. **Gitignore status** (ignored = OUTPUT, tracked = INPUT)
3. **Parameter metadata** (name, type, description analysis)
4. **Default assumptions** (when all else fails)

This approach adds a powerful heuristic that aligns with developer intuition and version control best practices.