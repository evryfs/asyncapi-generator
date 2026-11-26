package dev.banking.asyncapi.generator.gradle.plugin

import dev.banking.asyncapi.generator.gradle.plugin.tasks.GenerateAsyncApiTask
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class GradleGeneratedOutputRegistrarTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    private val project: Project by lazy {
        ProjectBuilder.builder().withProjectDir(temporaryDirectory.toFile()).build().also {
            it.pluginManager.apply("java")
        }
    }

    @Test
    fun `registers a model execution as sources without filtering the project's resources`() {
        val task =
            generationTask("models") {
                generatorName.set("kotlin")
                modelPackage.set("com.example.model")
            }

        GradleGeneratedOutputRegistrar(project).register(task)

        assertTrue(outputDirectory(task) in mainSourceSet().java.srcDirs)
        assertTrue(mainSourceSet().resources.excludes.isEmpty())
        assertTrue(task.get() in dependenciesOf("compileJava"))
    }

    @Test
    fun `registers a schema execution as resources only`() {
        val task =
            generationTask("schemas") {
                generatorName.set("avro-schema")
                schemaPackage.set("com.example.avro")
            }

        GradleGeneratedOutputRegistrar(project).register(task)

        assertFalse(outputDirectory(task) in mainSourceSet().java.srcDirs)
        assertTrue(task.get() in dependenciesOf("processResources"))
    }

    private fun generationTask(
        name: String,
        configure: GenerateAsyncApiTask.() -> Unit,
    ): TaskProvider<GenerateAsyncApiTask> =
        project.tasks.register(name, GenerateAsyncApiTask::class.java) {
            outputDirectory.set(project.layout.buildDirectory.dir("generated/asyncapi/$name"))
            configure()
        }

    private fun outputDirectory(task: TaskProvider<GenerateAsyncApiTask>): File =
        task
            .get()
            .outputDirectory
            .get()
            .asFile

    private fun mainSourceSet(): SourceSet =
        project.extensions
            .getByType(SourceSetContainer::class.java)
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)

    private fun dependenciesOf(taskName: String) =
        project.tasks.getByName(taskName).let { task -> task.taskDependencies.getDependencies(task) }
}
