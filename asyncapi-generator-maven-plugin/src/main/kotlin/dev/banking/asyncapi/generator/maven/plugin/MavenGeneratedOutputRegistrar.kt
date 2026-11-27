package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.plan.GenerationPlanner
import org.apache.maven.model.Resource
import org.apache.maven.project.MavenProject
import java.io.File

/**
 * Registers the output directory of an execution with the Maven project: as a compile source root
 * when the execution generates sources, and as a resource root without sources when it generates
 * schemas.
 *
 * @param project the project the directories are registered with
 * @param generationPlanner plans the execution, whose output kinds decide the registration
 */
internal class MavenGeneratedOutputRegistrar(
    private val project: MavenProject,
    private val generationPlanner: GenerationPlanner = GenerationPlanner(),
) {
    fun register(configuration: GeneratorConfiguration) {
        val outputKinds = generationPlanner.plan(configuration).outputKinds
        val outputDirectory = configuration.output.outputDirectory

        if (GeneratedArtifactKind.SOURCE in outputKinds) {
            registerCompileSourceRoot(outputDirectory.normalizedAbsolutePath())
        }
        if (GeneratedArtifactKind.SCHEMA in outputKinds) {
            registerSchemaResourceRoot(outputDirectory)
        }
    }

    private fun registerCompileSourceRoot(directory: String) {
        val registeredRoots = project.compileSourceRoots.map { root -> root.normalizedAbsolutePath() }
        if (directory !in registeredRoots) {
            project.addCompileSourceRoot(directory)
        }
    }

    private fun registerSchemaResourceRoot(directory: File) {
        val normalizedDirectory = directory.normalizedAbsolutePath()
        val sourceExcludes = listOf("**/*.java", "**/*.kt")
        val alreadyRegistered =
            project.resources.any { resource ->
                resource.directory?.let(::File)?.normalizedAbsolutePath() == normalizedDirectory &&
                    resource.excludes == sourceExcludes
            }

        if (!alreadyRegistered) {
            project.addResource(
                Resource().apply {
                    this.directory = normalizedDirectory
                    isFiltering = false
                    excludes = sourceExcludes
                },
            )
        }
    }

    private fun File.normalizedAbsolutePath(): String = toPath().toAbsolutePath().normalize().toString()

    private fun String.normalizedAbsolutePath(): String = File(this).normalizedAbsolutePath()
}
