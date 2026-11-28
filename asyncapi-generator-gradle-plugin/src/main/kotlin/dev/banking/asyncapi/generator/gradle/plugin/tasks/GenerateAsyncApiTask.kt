package dev.banking.asyncapi.generator.gradle.plugin.tasks

import dev.banking.asyncapi.generator.core.AsyncApiGenerationException
import dev.banking.asyncapi.generator.core.AsyncApiGenerator
import dev.banking.asyncapi.generator.core.ConfiguredGeneration
import dev.banking.asyncapi.generator.core.GeneratorSettings
import dev.banking.asyncapi.generator.core.ProtocProvider
import dev.banking.asyncapi.generator.gradle.plugin.extensions.AsyncApiClientConfiguration
import dev.banking.asyncapi.generator.gradle.plugin.extensions.AsyncApiModelConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Generates the outputs configured by one named AsyncAPI execution.
 */
@DisableCachingByDefault(because = "Code generation output is not yet configured for the Gradle build cache")
abstract class GenerateAsyncApiTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputSpec: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFiles: ConfigurableFileCollection

    @get:OutputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val generatorName: Property<String>

    @get:Input
    @get:Optional
    abstract val modelPackage: Property<String>

    @get:Input
    @get:Optional
    abstract val clientPackage: Property<String>

    @get:Input
    @get:Optional
    abstract val schemaPackage: Property<String>

    /** The execution's model settings. */
    @get:Nested
    abstract val modelConfig: AsyncApiModelConfiguration

    /** The client settings shared by every execution. */
    @get:Nested
    abstract val clientConfig: AsyncApiClientConfiguration

    @get:Input
    abstract val suppressedWarnings: ListProperty<String>

    /** The `protoc` executable; empty unless this execution generates native Protobuf models. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val protoc: ConfigurableFileCollection

    /** Shared by all executions of the project; records generated files so stale ones are removed. */
    @get:Internal
    abstract val outputManifest: RegularFileProperty

    /** The execution name, which owns this task's entries in [outputManifest]. */
    @get:Internal
    abstract val executionName: Property<String>

    @TaskAction
    fun generate() {
        logger.lifecycle("AsyncAPI generation '$name' started")

        val generator = AsyncApiGenerator(ProtocProvider { protoc.singleFile.also { it.setExecutable(true) }.toPath() })
        try {
            val generation =
                generator.configure(
                    settings().copy(
                        manifest = GeneratorSettings.Manifest(outputManifest.get().asFile, executionName.get()),
                    ),
                )
            if (generation.warnings.isNotEmpty()) {
                logger.warn(generation.warnings)
            }
            generator.generate(inputSpec.get().asFile, generation) { warnings -> logger.warn(warnings) }
        } catch (exception: AsyncApiGenerationException) {
            // Gradle shows only the message of a GradleException, without the exception's class name.
            throw GradleException(exception.message.orEmpty(), exception)
        }

        logger.lifecycle("AsyncAPI generation '$name' completed")
    }

    /**
     * What this execution generates, which decides whether the plugin compiles [outputDirectory], packages
     * it as resources, and resolves `protoc`; `null` while the configuration is invalid, which this task
     * reports itself when it runs.
     */
    fun generation(): ConfiguredGeneration? = runCatching { AsyncApiGenerator().configure(settings()) }.getOrNull()

    private fun settings(): GeneratorSettings =
        GeneratorSettings(
            generatorName = generatorName.orNull,
            outputDirectory = outputDirectory.get().asFile,
            outputFile = outputFile.orNull?.asFile,
            modelPackage = modelPackage.orNull,
            clientPackage = clientPackage.orNull,
            schemaPackage = schemaPackage.orNull,
            modelConfig = modelConfig.toSettings(),
            clientConfig = clientConfig.toSettings(),
            suppressedWarnings = suppressedWarnings.get().distinct(),
        )
}
