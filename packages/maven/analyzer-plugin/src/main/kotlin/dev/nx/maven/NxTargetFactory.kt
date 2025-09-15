package dev.nx.maven

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.maven.api.Project
import org.apache.maven.api.di.Inject
import org.apache.maven.api.di.Named
import org.apache.maven.api.di.Singleton
import org.apache.maven.api.model.Plugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Collects lifecycle and plugin information directly from Maven APIs
 */
@Named
@Singleton
class NxTargetFactory {


    private val objectMapper = ObjectMapper()

    @Inject
    private lateinit var testClassDiscovery: TestClassDiscovery

     @Inject
     private lateinit var phaseAnalyzer: PhaseAnalyzer
    private val log: Logger = LoggerFactory.getLogger(NxTargetFactory::class.java)
    fun createNxTargets(
        mavenCommand: String,
        project: Project
    ): Pair<ObjectNode, ObjectNode> {
        val nxTargets = objectMapper.createObjectNode()

        // Generate targets from discovered plugin goals
        val targetGroups = objectMapper.createObjectNode()

        val phaseTargets = generatePhaseTargets(project, mavenCommand)
        val mavenPhasesGroup = objectMapper.createArrayNode()
        phaseTargets.forEach { (phase, target) ->
            nxTargets.set<ObjectNode>(phase, target)
            mavenPhasesGroup.add(phase)
        }
        targetGroups.put("maven-phases", mavenPhasesGroup)

        val (goalTargets, goalGroups) = generateGoalTargets(project, mavenCommand)

        goalTargets.forEach { (goal, target) ->
            nxTargets.set<ObjectNode>(goal, target)
        }
        goalGroups.forEach { (groupName, group) ->
            val groupArray = objectMapper.createArrayNode()
            group.forEach { goal -> groupArray.add(goal) }
            targetGroups.put(groupName, groupArray)
        }

        val (atomizedTestTargets, atomizedTestTargetGroups) = generateAtomizedTestTargets(project, mavenCommand)

        atomizedTestTargets.forEach { (goal, target) ->
            nxTargets.set<ObjectNode>(goal, target)
        }
        atomizedTestTargetGroups.forEach { (groupName, group) ->
            val groupArray = objectMapper.createArrayNode()
            group.forEach { goal -> groupArray.add(goal) }
            targetGroups.put(groupName, groupArray)
        }
        return Pair(nxTargets, targetGroups)
    }

    private fun generatePhaseTargets(
        project: Project,
        mavenCommand: String,
    ): Map<String, ObjectNode> {
        val targets = mutableMapOf<String, ObjectNode>()
        // Extract discovered phases from lifecycle analysis
        val phasesToAnalyze = getPhases()

        log.info("Analyzing ${phasesToAnalyze.size} phases for ${project.artifactId}: ${phasesToAnalyze.joinToString(", ")}")

        // Generate targets from phase analysis
        phasesToAnalyze.forEach { phase ->
            try {
                // PhaseAnalyzer temporarily disabled for Maven 4.0.0-rc-3 compatibility
                // TODO: Re-enable when PathMatcherFactory is available

                val target = objectMapper.createObjectNode()
                target.put("executor", "nx:run-commands")

                val options = objectMapper.createObjectNode()
                options.put("command", "$mavenCommand $phase -am -pl ${project.groupId}:${project.artifactId}")
                target.put("options", options)

                val phaseInformation = phaseAnalyzer.analyze(project, phase)

                // Basic defaults without phase analysis
                target.put("cache", phaseInformation.isCacheable)
                target.put("parallelism", phaseInformation.isThreadSafe)

                // Basic inputs/outputs
                val inputsArray = objectMapper.createArrayNode()
                phaseInformation.inputs.forEach { input -> inputsArray.add(input) }
                target.set<ArrayNode>("inputs", inputsArray)

                val outputsArray = objectMapper.createArrayNode()
                phaseInformation.outputs.forEach { output -> outputsArray.add(output) }
                target.set<ArrayNode>("outputs", outputsArray)
                targets[phase] = target

            } catch (e: Exception) {
                log.debug("Failed to analyze phase '$phase' for project ${project.artifactId}: ${e.message}")
            }
        }
        return targets
    }

    private fun generateGoalTargets(
        project: Project,
        mavenCommand: String
    ): Pair<Map<String, ObjectNode>, Map<String, List<String>>> {
        val targets = mutableMapOf<String, ObjectNode>()
        val targetGroups = mutableMapOf<String, List<String>>()

        // Extract discovered plugin goals
        val plugins = getExecutablePlugins(project)

        plugins.forEach { plugin: Plugin ->
            val goals = getGoals(plugin)
            val targetGroup = mutableListOf<String>()
            val cleanPluginName = cleanPluginName(plugin)
            goals.forEach { goal ->
                val (targetName, targetNode) = createGoalTarget(mavenCommand, project, cleanPluginName, goal)
                targetGroup.add(targetName)
                targets[targetName] = targetNode
            }
            targetGroups[cleanPluginName] = targetGroup
        }

        return Pair(targets, targetGroups)
    }

    private fun getExecutablePlugins(project: Project): List<Plugin> {
        return project.build.plugins
    }

    private fun generateAtomizedTestTargets(
        project: Project,
        mavenCommand: String
    ): Pair<Map<String, ObjectNode>, Map<String, List<String>>> {
        val targets = mutableMapOf<String, ObjectNode>()
        val targetGroups = mutableMapOf<String, List<String>>()

        val testClasses = testClassDiscovery.discoverTestClasses(project)
        val testTargetNames = mutableListOf<String>()

        testClasses.forEach { testClass ->
            val targetName = "test--${testClass.packagePath}.${testClass.className}"

            testTargetNames.add(targetName)

            log.info("Generating target for test class: $targetName'")
            val target = objectMapper.createObjectNode()

            target.put("executor", "nx:run-commands")

            val options = objectMapper.createObjectNode()
            options.put(
                "command",
                "$mavenCommand test -am -pl ${project.groupId}:${project.artifactId} -Dtest=${testClass.packagePath}.${testClass.className}"
            )
            target.put("options", options)

            target.put("cache", false)
            target.put("parallelism", false)

            targets[targetName] = target
        }



        return Pair(targets, targetGroups)
    }

    private fun getPhases(): Set<String> {
        return setOf(
            "validate",
            "initialize",
            "generate-sources",
            "process-sources",
            "generate-resources",
            "process-resources",
            "compile",
            "process-classes",
            "generate-test-sources",
            "process-test-sources",
            "generate-test-resources",
            "process-test-resources",
            "test-compile",
            "process-test-classes",
            "test",
            "prepare-package",
            "package",
            "pre-integration-test",
            "integration-test",
            "post-integration-test",
            "verify",
            "install",
            "deploy",
            "clean",
            "site"
        )
    }

    private fun createGoalTarget(
        mavenCommand: String,
        project: Project,
        cleanPluginName: String,
        goalName: String
    ): Pair<String, ObjectNode> {
        val target = objectMapper.createObjectNode()

        target.put("executor", "nx:run-commands")

        val options = objectMapper.createObjectNode()
        options.put(
            "command",
            "$mavenCommand $cleanPluginName:$goalName -am -pl ${project.groupId}:${project.artifactId}"
        )
        target.put("options", options)

        target.put("cache", false)
        target.put("parallelism", false)

        return Pair("$cleanPluginName:$goalName", target);
    }

    private fun getGoals(plugin: Plugin): Set<String> {
        val result = mutableSetOf<String>()

        plugin.executions.forEach { execution ->
            execution.goals.forEach { goal ->
                result.add(goal)
            }
        }
        return result
    }


    /**
     * Clean plugin name for better target naming
     */
    private fun cleanPluginName(plugin: Plugin): String {
        val fullPluginName = "${plugin.groupId}.${plugin.artifactId}"
        return fullPluginName
            .replace("org.apache.maven.plugins.", "")
            .replace("maven-", "")
            .replace("-plugin", "")
    }
}
