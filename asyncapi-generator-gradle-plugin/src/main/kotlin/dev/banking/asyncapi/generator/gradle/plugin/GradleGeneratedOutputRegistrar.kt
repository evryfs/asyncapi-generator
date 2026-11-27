package dev.banking.asyncapi.generator.gradle.plugin

import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.gradle.plugin.tasks.GenerateAsyncApiTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.jvm.tasks.ProcessResources

/**
 * Registers the output directory of each execution with the main source set: as Java and Kotlin
 * sources when the execution generates sources, and as resources when it generates schemas.
 *
 * @param project the project whose main source set receives the directories
 */
internal class GradleGeneratedOutputRegistrar(
    private val project: Project,
) {
    fun register(task: TaskProvider<GenerateAsyncApiTask>) {
        val sourceDirectory = task.outputDirectoryFor(GeneratedArtifactKind.SOURCE)
        val schemaDirectory = task.outputDirectoryFor(GeneratedArtifactKind.SCHEMA)

        project.pluginManager.withPlugin("java") {
            val mainSourceSet = mainSourceSet()
            mainSourceSet.java.srcDir(sourceDirectory)
            // Sources and schemas share the directory; only the schemas are resources. The filter
            // applies to this directory alone, never to the project's own resources.
            project.tasks.named(mainSourceSet.processResourcesTaskName, ProcessResources::class.java) {
                from(schemaDirectory) { exclude("**/*.java", "**/*.kt") }
            }
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            val kotlinSources =
                requireNotNull(mainSourceSet().extensions.findByName("kotlin") as? SourceDirectorySet) {
                    "The Kotlin JVM plugin did not register the main Kotlin source directory set"
                }
            kotlinSources.srcDir(sourceDirectory)
        }
    }

    private fun mainSourceSet(): SourceSet =
        project.extensions
            .getByType(SourceSetContainer::class.java)
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)

    /**
     * The output directory of the task when it writes artifacts of [kind], else no directory; either
     * way built by the task. The decision reads the task's configuration, not its output, which
     * Gradle does not allow reading before the task has run.
     */
    private fun TaskProvider<GenerateAsyncApiTask>.outputDirectoryFor(kind: GeneratedArtifactKind): FileCollection =
        project
            .files(map { task -> if (kind in task.outputKinds()) listOf(task.outputDirectory.get()) else emptyList() })
            .builtBy(this)
}
