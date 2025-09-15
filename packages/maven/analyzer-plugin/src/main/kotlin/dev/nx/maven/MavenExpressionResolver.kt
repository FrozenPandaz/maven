package dev.nx.maven

import org.apache.maven.api.Session
import org.apache.maven.api.di.Inject
import org.apache.maven.api.di.Named
import org.apache.maven.api.di.Singleton
import org.apache.maven.api.Project
import org.apache.maven.api.ProjectScope
import org.apache.maven.api.Language
import org.apache.maven.api.services.Interpolator
import org.apache.maven.api.services.ProjectManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Resolves Maven expressions and parameter values
 */
//@Named
//@Singleton
class MavenExpressionResolver {

    @Inject
    private lateinit var session: Session

    @Inject
    private lateinit var interpolator: Interpolator

    @Inject
    private lateinit var projectManager: ProjectManager

    private val log: Logger = LoggerFactory.getLogger(MavenExpressionResolver::class.java)

    /**
     * Resolves a mojo parameter value by trying expression, default value, and known mappings
     */
    fun resolveParameterValue(name: String, defaultValue: String?, expression: String?, project: Project): String? {
        // Try expression first
        expression?.let { expr ->
            val resolved = resolveExpression(expr, project)
            if (resolved != expr) {
                // Filter out values that look like version numbers, not paths
                if (isValidPath(resolved)) {
                    return resolved
                } else {
                    return null
                }
            }
        }

        // Try default value
        defaultValue?.let { default ->
            val resolved = resolveExpression(default, project)
            if (isValidPath(resolved)) {
                return resolved
            } else {
                return null
            }
        }

        // Try known parameter mappings using interpolator
        val parameterExpression = when (name) {
            "sourceDirectory" -> "\${project.build.sourceDirectory}"
            "testSourceDirectory" -> "\${project.build.testSourceDirectory}"
            "outputDirectory" -> "\${project.build.outputDirectory}"
            "testOutputDirectory" -> "\${project.build.testOutputDirectory}"
            "buildDirectory" -> "\${project.build.directory}"
            "classpathElements", "compileClasspathElements" -> "\${project.compileClasspathElements}"
            "testClasspathElements" -> "\${project.testClasspathElements}"
            else -> null
        }

        val result = parameterExpression?.let { expr ->
            val resolved = resolveExpression(expr, project)
            if (isValidPath(resolved)) resolved else null
        }

        return result
    }

    /**
     * Checks if a resolved value looks like a valid file path rather than a version number or other non-path value
     */
    private fun isValidPath(value: String?): Boolean {
        if (value.isNullOrBlank()) return false

        // Filter out values that look like version numbers (e.g., "1.8", "11", "17")
        // Use simple string matching instead of regex to avoid StackOverflowError
        if (isVersionNumber(value)) {
            return false
        }

        // Filter out other common non-path values
        if (value in setOf("true", "false", "UTF-8", "jar", "war", "ear", "pom", "test-jar")) {
            return false
        }

        // Must contain at least one path separator or be an absolute path
        return value.contains("/") || value.contains("\\") || value.startsWith(".") || java.io.File(value).isAbsolute
    }

    /**
     * Resolves Maven expressions in a string
     */
    fun resolveExpression(expression: String, project: Project): String {
        if (!expression.contains("\${")) {
            return expression
        }

        return interpolator.interpolate(expression) { variable ->
            when (variable) {
                "project.basedir" -> project.basedir.toString()
                "basedir" -> project.basedir.toString()
                "project.build.directory" -> project.build.directory
                "project.build.sourceDirectory" -> {
                    // Get main source root from ProjectManager
                    projectManager.getEnabledSourceRoots(project, ProjectScope.MAIN, Language.JAVA_FAMILY)
                        .findFirst()
                        .map { it.directory().toString() }
                        .orElse("${project.basedir}/src/main/java")
                }
                "project.build.testSourceDirectory" -> {
                    // Get test source root from ProjectManager
                    projectManager.getEnabledSourceRoots(project, ProjectScope.TEST, Language.JAVA_FAMILY)
                        .findFirst()
                        .map { it.directory().toString() }
                        .orElse("${project.basedir}/src/test/java")
                }
                "project.build.outputDirectory" -> project.build.outputDirectory
                "project.build.testOutputDirectory" -> project.build.testOutputDirectory
                "project.build.finalName" -> project.build.finalName ?: "${project.artifactId}-${project.version}"
                "project.compileClasspathElements" -> {
                    // TODO: Implement actual classpath resolution using DependencyResolver
                    // For now, return basic classpath with output directory
                    listOf(
                        project.build.outputDirectory,
                        // Add dependency artifacts here when implementing full resolution
                    ).joinToString(java.io.File.pathSeparator)
                }
                "project.testClasspathElements" -> {
                    // TODO: Implement actual test classpath resolution using DependencyResolver
                    // For now, return basic classpath with test output directory
                    listOf(
                        project.build.testOutputDirectory,
                        project.build.outputDirectory, // Test classpath includes main classes
                        // Add dependency artifacts here when implementing full resolution
                    ).joinToString(java.io.File.pathSeparator)
                }
                "project.artifactId" -> project.artifactId
                "project.groupId" -> project.groupId
                "project.version" -> project.version
                "project.name" -> project.model.name ?: project.artifactId
                "session.executionRootDirectory" -> session.rootDirectory.toString()
                else -> {
                    // Try session properties first, then system properties
                    session.userProperties[variable]
                        ?: session.systemProperties[variable]
                        ?: System.getProperty(variable)
                }
            }
        } ?: expression
    }

    /**
     * Check if a string looks like a version number without using regex
     */
    private fun isVersionNumber(value: String): Boolean {
        if (value.isEmpty()) return false

        // Simple check: starts with digit and contains only digits and dots
        if (!value[0].isDigit()) return false

        for (char in value) {
            if (!char.isDigit() && char != '.') {
                return false
            }
        }

        // Avoid multiple consecutive dots or ending with dot
        if (value.contains("..") || value.endsWith(".")) {
            return false
        }

        return true
    }
}
