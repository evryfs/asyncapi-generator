package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorOutputConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.ProtobufModelType
import dev.banking.asyncapi.generator.core.generator.plan.GenerationPlanner
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import org.apache.maven.model.Resource
import org.apache.maven.project.MavenProject
import java.io.File

/**
 * Registers generated source and schema directories with the Maven project.
 *
 * Expected behavior is covered by:
 * - `MavenGeneratedOutputRegistrarTest`
 */
internal class MavenGeneratedOutputRegistrar(
    private val project: MavenProject,
    private val generationPlanner: GenerationPlanner = GenerationPlanner(),
) {
    fun register(configuration: GeneratorConfiguration) {
        val tasks = generationPlanner.plan(configuration).tasks

        tasks
            .flatMap { task -> task.sourceDirectories(configuration.output) }
            .map { directory -> directory.normalizedAbsolutePath() }
            .distinct()
            .forEach(::registerCompileSourceRoot)

        if (tasks.any { task -> task.generatesSchemaResources() }) {
            registerSchemaResourceRoot(configuration.output.resourceOutputDirectory)
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

    private fun GenerationTask.sourceDirectories(output: GeneratorOutputConfiguration): List<File> =
        when (this) {
            is GenerationTask.ModelArtifacts,
            is GenerationTask.KafkaKeyModelArtifacts,
            is GenerationTask.HeaderModelArtifacts,
            is GenerationTask.SpringKafkaClient,
            is GenerationTask.QuarkusKafkaClient,
            -> listOf(output.sourceOutputDirectory)
            is GenerationTask.NativeAvroArtifacts ->
                listOfNotNull(output.javaSourceOutputDirectory.takeIf { generateSpecificRecords })
            is GenerationTask.NativeProtobufArtifacts ->
                models?.let { modelGeneration ->
                    buildList {
                        add(output.javaSourceOutputDirectory)
                        if (modelGeneration.modelType == ProtobufModelType.KOTLIN) {
                            add(output.sourceOutputDirectory)
                        }
                    }
                }.orEmpty()
            is GenerationTask.AvroSchemaArtifacts,
            is GenerationTask.DocumentArtifact,
            is GenerationTask.JsonSchemaArtifacts,
            -> emptyList()
        }

    private fun GenerationTask.generatesSchemaResources(): Boolean =
        when (this) {
            is GenerationTask.AvroSchemaArtifacts,
            is GenerationTask.JsonSchemaArtifacts,
            is GenerationTask.NativeAvroArtifacts,
            is GenerationTask.NativeProtobufArtifacts,
            -> true
            is GenerationTask.DocumentArtifact,
            is GenerationTask.HeaderModelArtifacts,
            is GenerationTask.KafkaKeyModelArtifacts,
            is GenerationTask.ModelArtifacts,
            is GenerationTask.QuarkusKafkaClient,
            is GenerationTask.SpringKafkaClient,
            -> false
        }

    private fun File.normalizedAbsolutePath(): String =
        toPath().toAbsolutePath().normalize().toString()

    private fun String.normalizedAbsolutePath(): String =
        File(this).normalizedAbsolutePath()
}
