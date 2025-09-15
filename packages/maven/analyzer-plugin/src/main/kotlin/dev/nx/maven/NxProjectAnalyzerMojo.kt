package dev.nx.maven

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.maven.api.Lifecycle.Phase
import org.apache.maven.api.Project
import org.apache.maven.api.Session
import org.apache.maven.api.di.Inject
import org.apache.maven.api.plugin.annotations.Mojo
import org.apache.maven.api.plugin.annotations.Parameter
import org.apache.maven.api.plugin.MojoException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Maven plugin to analyze project structure and generate JSON for Nx integration
 */
@Mojo(
    name = "analyze",
    defaultPhase = Phase.VALIDATE,
    aggregator = true
)
open class NxProjectAnalyzerMojo() : org.apache.maven.api.plugin.Mojo {

    private val log: Logger = LoggerFactory.getLogger(NxProjectAnalyzerMojo::class.java)

    @Inject
    private lateinit var mavenSession: Session

    @Inject
    private lateinit var nxProjectAnalyzer: NxProjectAnalyzer

    @Parameter(property = "outputFile", defaultValue = "nx-maven-projects.json")
    private lateinit var outputFile: String

    private val objectMapper = ObjectMapper()

    @Throws(MojoException::class)
    override fun execute() {
        log.info("Analyzing Maven projects using optimized two-tier approach...")
//        log.info("Parameters: outputFile='$outputFile', workspaceRoot='$workspaceRoot'")

        // GitIgnoreClassifier is now injected as a DI component

        try {
            val allProjects = mavenSession.projects
            log.info("Found ${allProjects.size} Maven projects")

            // Step 1: Execute per-project analysis for all projects (in-memory)
            log.info("Step 1: Running optimized per-project analysis...")
            val inMemoryAnalyses = executePerProjectAnalysisInMemory(allProjects)

            // Step 2: Write project analyses to output file
            log.info("Step 2: Writing project analyses to output file...")
            writeProjectAnalysesToFile(inMemoryAnalyses)

            log.info("Optimized two-tier analysis completed successfully")

        } catch (e: Exception) {
            throw MojoException("Failed to execute optimized two-tier Maven analysis", e)
        }
    }

    private fun executePerProjectAnalysisInMemory(allProjects: List<Project>): Map<String, Pair<String, JsonNode>?> {
        val startTime = System.currentTimeMillis()
        log.info("Creating shared component instances for optimized analysis...")

        // All components are now DI-managed - no manual creation needed!

        val setupTime = System.currentTimeMillis() - startTime
        log.info("Shared components created in ${setupTime}ms, analyzing ${allProjects.size} projects...")

        val projectStartTime = System.currentTimeMillis()

        // Process projects in parallel with separate analyzer instances
        val inMemoryAnalyses = allProjects.parallelStream().map { project ->
            try {
                log.info("Analyzing project: ${project.artifactId}")

                val analysis = nxProjectAnalyzer.analyze(project)
                project.artifactId to analysis

            } catch (e: Exception) {
                log.warn("Failed to analyze project ${project.artifactId}: ${e.message}")
                project.artifactId to null
            }
        }.collect(java.util.stream.Collectors.toList()).toMap()

        val totalTime = System.currentTimeMillis() - startTime
        val analysisTime = System.currentTimeMillis() - projectStartTime
        log.info("Completed in-memory analysis of ${allProjects.size} projects in ${totalTime}ms (setup: ${setupTime}ms, analysis: ${analysisTime}ms)")

        return inMemoryAnalyses
    }

    private fun writeProjectAnalysesToFile(inMemoryAnalyses: Map<String, Pair<String, JsonNode>?>) {
        val outputPath = if (outputFile.startsWith("/")) {
            File(outputFile)
        } else {
            File("", outputFile)
        }

        // Ensure parent directory exists
        outputPath.parentFile?.mkdirs()

        // Create JSON structure with both project analyses and createNodesResults
        val rootNode = objectMapper.createObjectNode()
        val projectsNode = objectMapper.createObjectNode()

        // Skip project analyses section - all data is in createNodesResults
        rootNode.set<JsonNode>("projects", projectsNode)

        // Generate createNodesResults for Nx plugin consumption
        val createNodesResults = generateCreateNodesResults(inMemoryAnalyses)
        rootNode.set<JsonNode>("createNodesResults", createNodesResults)

        // Add metadata
        rootNode.put("totalProjects", inMemoryAnalyses.size)
//        rootNode.put("workspaceRoot", workspaceRoot)
        rootNode.put("analysisMethod", "optimized-parallel")
        rootNode.put("analyzedProjects", inMemoryAnalyses.size)

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath, rootNode)
        log.info("Generated project analyses with ${inMemoryAnalyses.size} projects: ${outputPath.absolutePath}")
    }

    private fun generateCreateNodesResults(inMemoryAnalyses: Map<String, Pair<String, JsonNode>?>): com.fasterxml.jackson.databind.node.ArrayNode {
        val createNodesResults = objectMapper.createArrayNode()

        // Group projects by root directory (for now, assume all projects are at workspace root)
        val projects = objectMapper.createObjectNode()

        inMemoryAnalyses.forEach { (projectName, nxConfig) ->
            try {
                if (nxConfig != null) {
                    val (root, projectConfig) = nxConfig
                    projects.set<JsonNode>(root, projectConfig)
                }
            } catch (e: Exception) {
                log.warn("Failed to generate Nx config for project $projectName: ${e.message}")
            }
        }

        // Create the createNodesResults structure: [root, {projects: {...}}]
        val resultTuple = objectMapper.createArrayNode()
        resultTuple.add("") // Root path (workspace root)
        val projectsWrapper = objectMapper.createObjectNode()
        projectsWrapper.set<JsonNode>("projects", projects)
        resultTuple.add(projectsWrapper)

        createNodesResults.add(resultTuple)
        return createNodesResults
    }


}
