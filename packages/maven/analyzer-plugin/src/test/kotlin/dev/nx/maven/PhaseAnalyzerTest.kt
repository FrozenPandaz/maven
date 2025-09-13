package dev.nx.maven

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.MavenPluginManager
import org.apache.maven.project.MavenProject
import java.io.File

/**
 * Unit test for PhaseAnalyzer using Maven 4 DI components
 * Tests that the DI components work correctly together
 */
class PhaseAnalyzerTest {

    private lateinit var phaseAnalyzer: PhaseAnalyzer
    private lateinit var session: MavenSession
    private lateinit var testProject: MavenProject

    @BeforeEach
    fun setUp() {
        // Create test fixtures
        session = createMockSession()
        testProject = createMockProject()
        val pluginManager = mock(MavenPluginManager::class.java)

        // Create DI components with injected dependencies
        val expressionResolver = createComponent(MavenExpressionResolver::class.java, session)
        val pathResolver = createComponent(PathResolver::class.java, session)
        val gitIgnoreClassifier = mock(GitIgnoreClassifier::class.java)

        // Create PhaseAnalyzer with all DI dependencies
        phaseAnalyzer = PhaseAnalyzer().apply {
            injectField("session", session)
            injectField("pluginManager", pluginManager)
            injectField("expressionResolver", expressionResolver)
            injectField("pathResolver", pathResolver)
            injectField("gitIgnoreClassifier", gitIgnoreClassifier)
        }
    }

    @Test
    fun testAnalyzeCompilePhase() {
        val result = phaseAnalyzer.analyze(testProject, "compile")

        assertNotNull(result)
        assertTrue(result.isThreadSafe, "Compile phase should be thread safe")
        assertTrue(result.isCacheable, "Compile phase should be cacheable")

        println("Compile phase analysis:")
        println("  Thread Safe: ${result.isThreadSafe}")
        println("  Cacheable: ${result.isCacheable}")
        println("  Inputs: ${result.inputs.size}, Outputs: ${result.outputs.size}")
    }

    @Test
    fun testAnalyzeTestPhase() {
        val result = phaseAnalyzer.analyze(testProject, "test")

        assertNotNull(result)
        assertTrue(result.isThreadSafe, "Test phase should be thread safe")

        println("Test phase analysis:")
        println("  Thread Safe: ${result.isThreadSafe}")
        println("  Cacheable: ${result.isCacheable}")
        println("  Inputs: ${result.inputs.size}, Outputs: ${result.outputs.size}")
    }

    @Test
    fun testAnalyzeEmptyPhase() {
        val result = phaseAnalyzer.analyze(testProject, "non-existent-phase")

        assertNotNull(result)
        assertTrue(result.isThreadSafe, "Empty phase should be thread safe")
        assertTrue(result.isCacheable, "Empty phase should be cacheable")
        assertTrue(result.inputs.isEmpty(), "Empty phase should have no inputs")
        assertTrue(result.outputs.isEmpty(), "Empty phase should have no outputs")
    }

    @Test
    fun testDIComponentsAreInjected() {
        // Verify all DI components are properly injected
        assertNotNull(phaseAnalyzer)

        phaseAnalyzer.verifyFieldInjected("session")
        phaseAnalyzer.verifyFieldInjected("pluginManager")
        phaseAnalyzer.verifyFieldInjected("expressionResolver")
        phaseAnalyzer.verifyFieldInjected("pathResolver")
        phaseAnalyzer.verifyFieldInjected("gitIgnoreClassifier")

        println("✅ All DI components are properly injected into PhaseAnalyzer")
    }

    // Helper methods to reduce boilerplate

    private fun createMockSession(): MavenSession {
        return mock(MavenSession::class.java).apply {
            `when`(executionRootDirectory).thenReturn(System.getProperty("user.dir"))
            `when`(allProjects).thenReturn(emptyList())
        }
    }

    private fun createMockProject(): MavenProject {
        return mock(MavenProject::class.java).apply {
            val baseDir = File(System.getProperty("user.dir"))
            `when`(basedir).thenReturn(baseDir)
            `when`(groupId).thenReturn("test.group")
            `when`(artifactId).thenReturn("test-artifact")
            `when`(version).thenReturn("1.0.0-SNAPSHOT")
            `when`(build).thenReturn(mock(org.apache.maven.model.Build::class.java))
            `when`(build.plugins).thenReturn(emptyList())
        }
    }

    private fun <T> createComponent(clazz: Class<T>, session: MavenSession): T {
        return clazz.getDeclaredConstructor().newInstance().apply {
            injectField("session", session)
        }
    }

    // Extension functions to make reflection cleaner

    private fun Any.injectField(fieldName: String, value: Any?) {
        val field = this::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(this, value)
    }

    private fun Any.verifyFieldInjected(fieldName: String) {
        val field = this::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        assertNotNull(field.get(this), "Field '$fieldName' should be injected")
    }
}