package dev.nx.maven

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.maven.api.Project
import org.apache.maven.api.Session
import org.apache.maven.api.di.Inject
import org.apache.maven.api.di.Named
import org.apache.maven.api.di.Singleton
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Analyzer for a single Maven project structure to generate JSON for Nx integration
 * This is a simplified, per-project analyzer that doesn't require cross-project coordination
 */
@Named
@Singleton
class NxProjectAnalyzer {

    @Inject
    private lateinit var session: Session

    @Inject
    private lateinit var nxTargetFactory: NxTargetFactory

    @Inject
    private lateinit var pathResolver: PathResolver
    private val objectMapper = ObjectMapper()
    private val log: Logger = LoggerFactory.getLogger(NxProjectAnalyzer::class.java)


    /**
     * Analyzes the project and returns Nx project config
     */
    fun analyze(project: Project): Pair<String, ObjectNode>? {
        try {
            val mavenCommand = pathResolver.getMavenCommand()

            // Calculate relative path from workspace root
            val workspaceRootPath = session.rootDirectory
            val projectPath = project.basedir
            val root = workspaceRootPath.relativize(projectPath).toString().replace('\\', '/')
            val projectName = "${project.groupId}.${project.artifactId}"
            val projectType = determineProjectType(project)

            // Create Nx project configuration
            val nxProject = objectMapper.createObjectNode()
            nxProject.put("name", projectName)
            nxProject.put("root", root)
            nxProject.put("projectType", projectType)
            nxProject.put("sourceRoot", "${root}/src/main/java")

            val (nxTargets, targetGroups) = nxTargetFactory.createNxTargets(mavenCommand, project)
            nxProject.set<ObjectNode>("targets", nxTargets)

            // Project metadata including target groups
            val projectMetadata = objectMapper.createObjectNode()
            projectMetadata.put("targetGroups", targetGroups)
            nxProject.put("metadata", projectMetadata)

            // Tags
            val tags = objectMapper.createArrayNode()
            tags.add("maven:${project.groupId}")
            tags.add("maven:${project.packaging}")
            nxProject.put("tags", tags)

            log.info("Analyzed project: ${project.artifactId} at $root")

            return root to nxProject

        } catch (e: Exception) {
            log.error("Failed to analyze project ${project.artifactId}: ${e.message}", e)
            return null
        }
    }

    private fun determineProjectType(project: Project): String {
        return when (project.packaging.id()) {
            "pom" -> "library"
            "jar", "war", "ear" -> "application"
            "maven-plugin" -> "library"
            else -> "library"
        }
    }
}
