package dev.nx.maven

import org.apache.maven.api.Session
import org.apache.maven.api.di.Inject
import org.apache.maven.api.di.Named
import org.apache.maven.api.di.Singleton
import java.io.File

/**
 * Handles path resolution, Maven command detection, and input/output path formatting for Nx
 */
@Named
@Singleton
class PathResolver {

    @Inject
    private lateinit var session: Session

    private val workspaceRoot: String
        get() = session.rootDirectory.toString()

    // Allow using a different project base dir for specific contexts
    fun toProjectPath(path: String, projectBaseDir: String? = null): String = try {
        val filePath = java.nio.file.Paths.get(path)
        val baseDirPath = if (projectBaseDir != null) {
            java.nio.file.Paths.get(projectBaseDir)
        } else {
            java.nio.file.Paths.get(workspaceRoot)
        }
        val relativePath = baseDirPath.relativize(filePath)
        "{projectRoot}/$relativePath".replace('\\', '/')
    } catch (e: Exception) {
        "{projectRoot}/$path"
    }

    /**
     * Adds an input path to the inputs collection, checking existence and formatting appropriately
     */
    fun addInputPath(path: String, inputs: MutableSet<String>) {
        // Handle classpath-style paths (multiple paths separated by : or ;)
        val pathSeparator = File.pathSeparator
        if (path.contains(pathSeparator)) {
            // Split classpath and add each path individually
            path.split(pathSeparator).forEach { singlePath ->
                if (singlePath.isNotBlank()) {
                    addSingleInputPath(singlePath.trim(), inputs)
                }
            }
        } else {
            addSingleInputPath(path, inputs)
        }
    }

    /**
     * Adds a single input path to the inputs collection
     */
    private fun addSingleInputPath(path: String, inputs: MutableSet<String>) {
        val file = File(path)
        if (file.exists()) {
            // TODO: External dependencies (like JARs from .m2/repository) are not yet supported by Nx
            // as cache inputs. For now, we exclude them to avoid Nx errors. When Nx supports external
            // file dependencies, we should include them as: inputs.add(path)
            // This is important for proper cache invalidation when external dependencies change.
            if (isExternalDependency(path)) {
                // Skip external dependencies for now - Nx doesn't support them yet
                return
            } else if (isInterProjectDependency(path)) {
                // Inter-project dependency JAR - include as workspace input
                val projectPath = toProjectPath(path)
                inputs.add(projectPath)
            } else {
                val projectPath = toProjectPath(path)
                if (file.isDirectory) {
                    inputs.add("$projectPath/**/*")
                } else {
                    inputs.add(projectPath)
                }
            }
        }
    }

    /**
     * Checks if a path is an external dependency (JAR file outside the workspace)
     */
    private fun isExternalDependency(path: String): Boolean {
        val file = File(path)
        return (file.name.endsWith(".jar") || file.name.endsWith(".war") || file.name.endsWith(".ear")) &&
               !path.startsWith(workspaceRoot)
    }

    /**
     * Checks if a path is an inter-project dependency (output directory or JAR within the workspace)
     */
    private fun isInterProjectDependency(path: String): Boolean {
        if (!path.startsWith(workspaceRoot)) return false

        val file = File(path)
        // Inter-project dependencies can be:
        // 1. JAR files within workspace (built artifacts)
        // 2. target/classes directories (direct classpath references)
        return (file.name.endsWith(".jar") || file.name.endsWith(".war") || file.name.endsWith(".ear")) ||
               (path.contains("/target/classes") || path.contains("/target/test-classes"))
    }

    /**
     * Adds an output path to the outputs collection
     */
    fun addOutputPath(path: String, outputs: MutableSet<String>) {
        outputs.add(toProjectPath(path))
    }


    /**
     * Determines the best Maven executable: mvnd > mvnw > mvn
     */
    fun getMavenCommand(): String {
        val root = findProjectWorkspaceRoot()

        // First priority: Check for Maven Daemon
        try {
            val process = ProcessBuilder("mvnd", "--version")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                return "mvnd"
            }
        } catch (e: Exception) {
            // mvnd not available, continue to next option
        }

        // Second priority: Check for Maven wrapper
        val mvnwFile = File(root, "mvnw")
        return if (mvnwFile.exists() && mvnwFile.canExecute()) {
            "./mvnw"
        } else {
            "mvn"
        }
    }

    /**
     * Finds the workspace root by looking for the top-level pom.xml
     */
    private fun findProjectWorkspaceRoot(): File {
        var current = session.rootDirectory.toFile()
        while (current.parent != null) {
            val parentPom = File(current.parent, "pom.xml")
            if (parentPom.exists()) {
                current = current.parentFile
            } else {
                break
            }
        }
        return current
    }
}

