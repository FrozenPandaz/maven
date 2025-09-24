package dev.nx.maven.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Handles path resolution, Maven command detection, and input/output path formatting for Nx
 */
class PathFormatter(
    private val workspaceRoot: File,
) {

    private val log: Logger = LoggerFactory.getLogger(PathFormatter::class.java)

    fun formatInputPath(path: File, projectRoot: File): String {
        return formatPath(path, projectRoot)
    }

    fun toDependentTaskOutputs(path: File, projectRoot: File): DependentTaskOutputs {
        val relativePath = path.relativeTo(projectRoot)
        return DependentTaskOutputs(relativePath.path)
    }

    fun formatOutputPath(path: File, projectRoot: File): String {
        return formatPath(path, projectRoot)
    }

    private fun formatPath(path: File, projectRoot: File): String {
        return toProjectPath(path, projectRoot) ?: toWorkspacePath(path)
    }

    private fun toWorkspacePath(path: File): String {
        return "{workspaceRoot}/${path.relativeToOrSelf(workspaceRoot)}"
    }

    private fun toProjectPath(path: File, projectRoot: File): String? {
        return if (path.path.startsWith(projectRoot.path)) {
            "{projectRoot}/${path.relativeTo(projectRoot)}"
        } else {
            null
        }
    }
}

data class DependentTaskOutputs(val path: String, val transitive: Boolean = true)
