package dev.banking.asyncapi.generator.gradle.plugin

import dev.banking.asyncapi.generator.gradle.plugin.tasks.GenerateAsyncApiTask
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider

/**
 * Registers generated output directories with the Gradle main source set.
 *
 * Expected behavior is covered by:
 * - `GradleGeneratedOutputRegistrarTest`
 */
internal class GradleGeneratedOutputRegistrar(
    private val project: Project,
) {
    fun register(task: TaskProvider<GenerateAsyncApiTask>) {
        val outputDirectory = task.flatMap(GenerateAsyncApiTask::outputDirectory)

        project.pluginManager.withPlugin("java") {
            val mainSourceSet =
                project.extensions
                    .getByType(SourceSetContainer::class.java)
                    .getByName("main")

            mainSourceSet.java.srcDir(outputDirectory)
            mainSourceSet.resources.srcDir(outputDirectory)
            mainSourceSet.resources.exclude(
                "**/*.java",
                "**/*.kt",
            )
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            val mainSourceSet =
                project.extensions
                    .getByType(SourceSetContainer::class.java)
                    .getByName("main")
            val kotlinSources =
                requireNotNull(
                    mainSourceSet.extensions.findByName("kotlin") as? SourceDirectorySet,
                ) {
                    "The Kotlin JVM plugin did not register the main Kotlin source directory set"
                }

            kotlinSources.srcDir(outputDirectory)
        }
    }
}
