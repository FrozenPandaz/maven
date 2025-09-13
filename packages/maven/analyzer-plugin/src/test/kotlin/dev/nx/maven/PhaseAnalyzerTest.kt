package dev.nx.maven

import org.apache.maven.api.di.Inject
import org.apache.maven.api.plugin.testing.MojoTest
import org.apache.maven.api.plugin.testing.InjectMojo
import org.apache.maven.api.plugin.testing.MojoParameter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import org.apache.maven.execution.MavenSession
import org.apache.maven.project.MavenProject
import java.io.File

/**
 * Unit test for PhaseAnalyzer using Maven 4's @MojoTest with real DI
 * This properly tests the DI components in a real Maven 4 environment
 */
@MojoTest
class PhaseAnalyzerTest {

    @Inject
    private phaseAnalyzer: PhaseAnalyzer

    @Test
    @InjectMojo(goal = "analyze")
    fun testAnalyzeCompilePhase(mojo: NxProjectAnalyzerMojo) {
        val testProject = createMockProject()

        // Access PhaseAnalyzer from the injected mojo
        val phaseAnalyzerField = mojo.javaClass.getDeclaredField("phaseAnalyzer")
        phaseAnalyzerField.isAccessible = true
        val phaseAnalyzer = phaseAnalyzerField.get(mojo) as PhaseAnalyzer

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
    @InjectMojo(goal = "analyze")
    fun testAnalyzeTestPhase(mojo: NxProjectAnalyzerMojo) {
        val testProject = createMockProject()

        // Access PhaseAnalyzer from the injected mojo
        val phaseAnalyzerField = mojo.javaClass.getDeclaredField("phaseAnalyzer")
        phaseAnalyzerField.isAccessible = true
        val phaseAnalyzer = phaseAnalyzerField.get(mojo) as PhaseAnalyzer

        val result = phaseAnalyzer.analyze(testProject, "test")

        assertNotNull(result)
        assertTrue(result.isThreadSafe, "Test phase should be thread safe")

        println("Test phase analysis:")
        println("  Thread Safe: ${result.isThreadSafe}")
        println("  Cacheable: ${result.isCacheable}")
        println("  Inputs: ${result.inputs.size}, Outputs: ${result.outputs.size}")
    }

    @Test
    @InjectMojo(goal = "analyze")
    fun testAnalyzeEmptyPhase(mojo: NxProjectAnalyzerMojo) {
        val testProject = createMockProject()

        // Access PhaseAnalyzer from the injected mojo
        val phaseAnalyzerField = mojo.javaClass.getDeclaredField("phaseAnalyzer")
        phaseAnalyzerField.isAccessible = true
        val phaseAnalyzer = phaseAnalyzerField.get(mojo) as PhaseAnalyzer

        val result = phaseAnalyzer.analyze(testProject, "non-existent-phase")

        assertNotNull(result)
        assertTrue(result.isThreadSafe, "Empty phase should be thread safe")
        assertTrue(result.isCacheable, "Empty phase should be cacheable")
        assertTrue(result.inputs.isEmpty(), "Empty phase should have no inputs")
        assertTrue(result.outputs.isEmpty(), "Empty phase should have no outputs")
    }

    @Test
    @InjectMojo(goal = "analyze")
    fun testDIComponentsAreInjected(mojo: NxProjectAnalyzerMojo) {
        // Verify that Maven 4 DI properly injected all components
        assertNotNull(mojo)

        // Access PhaseAnalyzer from the injected mojo
        val phaseAnalyzerField = mojo.javaClass.getDeclaredField("phaseAnalyzer")
        phaseAnalyzerField.isAccessible = true
        val phaseAnalyzer = phaseAnalyzerField.get(mojo) as PhaseAnalyzer

        assertNotNull(phaseAnalyzer)

        println("✅ Maven 4 DI successfully injected NxProjectAnalyzerMojo and PhaseAnalyzer")
    }

    // Helper method to create test project
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
}
