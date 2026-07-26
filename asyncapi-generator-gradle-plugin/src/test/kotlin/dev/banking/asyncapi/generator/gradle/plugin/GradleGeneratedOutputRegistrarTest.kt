package dev.banking.asyncapi.generator.gradle.plugin

import dev.banking.asyncapi.generator.gradle.plugin.tasks.GenerateAsyncApiTask
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class GradleGeneratedOutputRegistrarTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `registers generated output as Java sources and filtered resources`() {
        val project =
            ProjectBuilder
                .builder()
                .withProjectDir(temporaryDirectory.toFile())
                .build()
        project.pluginManager.apply("java")
        val outputDirectory = project.layout.buildDirectory.dir("generated/asyncapi/example")
        val generationTask =
            project.tasks.register(
                "generateExampleAsyncApi",
                GenerateAsyncApiTask::class.java,
            ) {
                this.outputDirectory.set(outputDirectory)
            }

        GradleGeneratedOutputRegistrar(project).register(generationTask)

        val mainSourceSet =
            project.extensions
                .getByType(SourceSetContainer::class.java)
                .getByName("main")
        val expectedDirectory = outputDirectory.get().asFile
        val compileJava = project.tasks.getByName("compileJava") as JavaCompile
        val processResources = project.tasks.getByName("processResources")

        assertTrue(expectedDirectory in mainSourceSet.java.srcDirs)
        assertTrue(expectedDirectory in mainSourceSet.resources.srcDirs)
        assertTrue("**/*.java" in mainSourceSet.resources.excludes)
        assertTrue("**/*.kt" in mainSourceSet.resources.excludes)
        assertTrue(generationTask.get() in compileJava.taskDependencies.getDependencies(compileJava))
        assertTrue(generationTask.get() in processResources.taskDependencies.getDependencies(processResources))
    }
}
