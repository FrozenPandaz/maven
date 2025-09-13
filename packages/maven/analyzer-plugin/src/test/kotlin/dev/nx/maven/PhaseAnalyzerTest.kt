package dev.nx.maven

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.MavenPluginManager
import org.apache.maven.project.MavenProject
import org.apache.maven.lifecycle.DefaultLifecycles
import org.apache.maven.lifecycle.LifecycleExecutor
import java.io.File

/**
 * Unit test for PhaseAnalyzer using JUnit 5 with manual DI component setup
 * Tests that the DI components work correctly together
 */
class PhaseAnalyzerTest {

    private lateinit var phaseAnalyzer: PhaseAnalyzer
    private lateinit var expressionResolver: MavenExpressionResolver
    private lateinit var pathResolver: PathResolver
    private lateinit var gitIgnoreClassifier: GitIgnoreClassifier
    private lateinit var session: MavenSession
    private lateinit var pluginManager: MavenPluginManager
    private lateinit var testProject: MavenProject

    @BeforeEach
    fun setUp() {
        // Create mock session
        session = mock(MavenSession::class.java)
        `when`(session.executionRootDirectory).thenReturn(System.getProperty("user.dir"))
        `when`(session.allProjects).thenReturn(listOf())

        // Create mock test project
        testProject = mock(MavenProject::class.java)
        val baseDir = File(System.getProperty("user.dir"))
        `when`(testProject.basedir).thenReturn(baseDir)
        `when`(testProject.groupId).thenReturn("test.group")
        `when`(testProject.artifactId).thenReturn("test-artifact")
        `when`(testProject.version).thenReturn("1.0.0-SNAPSHOT")
        `when`(testProject.build).thenReturn(mock(org.apache.maven.model.Build::class.java))
        `when`(testProject.build.plugins).thenReturn(emptyList())

        // Create mock plugin manager
        pluginManager = mock(MavenPluginManager::class.java)

        // Create DI components manually (simulating DI injection)
        expressionResolver = MavenExpressionResolver().apply {
            // Use reflection to set the private session field
            val sessionField = MavenExpressionResolver::class.java.getDeclaredField("session")
            sessionField.isAccessible = true
            sessionField.set(this, session)
        }

        pathResolver = PathResolver().apply {
            // Use reflection to set the private session field
            val sessionField = PathResolver::class.java.getDeclaredField("session")
            sessionField.isAccessible = true
            sessionField.set(this, session)
        }

        // Create a mock GitIgnoreClassifier to avoid initialization issues in tests
        gitIgnoreClassifier = mock(GitIgnoreClassifier::class.java)

        phaseAnalyzer = PhaseAnalyzer().apply {
            // Use reflection to set all the DI fields
            val sessionField = PhaseAnalyzer::class.java.getDeclaredField("session")
            sessionField.isAccessible = true
            sessionField.set(this, session)

            val pluginManagerField = PhaseAnalyzer::class.java.getDeclaredField("pluginManager")
            pluginManagerField.isAccessible = true
            pluginManagerField.set(this, pluginManager)

            val expressionResolverField = PhaseAnalyzer::class.java.getDeclaredField("expressionResolver")
            expressionResolverField.isAccessible = true
            expressionResolverField.set(this, expressionResolver)

            val pathResolverField = PhaseAnalyzer::class.java.getDeclaredField("pathResolver")
            pathResolverField.isAccessible = true
            pathResolverField.set(this, pathResolver)

            val gitIgnoreField = PhaseAnalyzer::class.java.getDeclaredField("gitIgnoreClassifier")
            gitIgnoreField.isAccessible = true
            gitIgnoreField.set(this, gitIgnoreClassifier)
        }
    }

    @Test
    fun testAnalyzeCompilePhase() {
        // Test compile phase analysis
        val result = phaseAnalyzer.analyze(testProject, "compile")

        // Verify basic properties
        assertNotNull(result)
        assertTrue(result.isThreadSafe, "Compile phase should be thread safe")
        assertTrue(result.isCacheable, "Compile phase should be cacheable")

        // Print results for debugging
        println("Compile phase analysis:")
        println("  Thread Safe: ${result.isThreadSafe}")
        println("  Cacheable: ${result.isCacheable}")
        println("  Inputs (${result.inputs.size}): ${result.inputs}")
        println("  Outputs (${result.outputs.size}): ${result.outputs}")
    }

    @Test
    fun testAnalyzeTestPhase() {
        // Test phase analysis
        val result = phaseAnalyzer.analyze(testProject, "test")

        // Verify basic properties
        assertNotNull(result)
        assertTrue(result.isThreadSafe, "Test phase should be thread safe")

        println("Test phase analysis:")
        println("  Thread Safe: ${result.isThreadSafe}")
        println("  Cacheable: ${result.isCacheable}")
        println("  Inputs (${result.inputs.size}): ${result.inputs}")
        println("  Outputs (${result.outputs.size}): ${result.outputs}")
    }

    @Test
    fun testAnalyzeEmptyPhase() {
        // Test with a phase that has no plugins
        val result = phaseAnalyzer.analyze(testProject, "non-existent-phase")

        assertNotNull(result)
        assertTrue(result.isThreadSafe, "Empty phase should be thread safe")
        assertTrue(result.isCacheable, "Empty phase should be cacheable")
        assertTrue(result.inputs.isEmpty(), "Empty phase should have no inputs")
        assertTrue(result.outputs.isEmpty(), "Empty phase should have no outputs")
    }

    @Test
    fun testDIComponentsAreInjected() {
        // Verify that all DI components are properly injected
        assertNotNull(phaseAnalyzer)

        // Use reflection to verify the fields are set
        val sessionField = PhaseAnalyzer::class.java.getDeclaredField("session")
        sessionField.isAccessible = true
        assertNotNull(sessionField.get(phaseAnalyzer))

        val pluginManagerField = PhaseAnalyzer::class.java.getDeclaredField("pluginManager")
        pluginManagerField.isAccessible = true
        assertNotNull(pluginManagerField.get(phaseAnalyzer))

        val expressionResolverField = PhaseAnalyzer::class.java.getDeclaredField("expressionResolver")
        expressionResolverField.isAccessible = true
        assertNotNull(expressionResolverField.get(phaseAnalyzer))

        val pathResolverField = PhaseAnalyzer::class.java.getDeclaredField("pathResolver")
        pathResolverField.isAccessible = true
        assertNotNull(pathResolverField.get(phaseAnalyzer))

        val gitIgnoreField = PhaseAnalyzer::class.java.getDeclaredField("gitIgnoreClassifier")
        gitIgnoreField.isAccessible = true
        assertNotNull(gitIgnoreField.get(phaseAnalyzer))

        println("✅ All DI components are properly injected into PhaseAnalyzer")
    }
}