package dev.banking.asyncapi.generator.gradle.plugin.tasks

import dev.banking.asyncapi.generator.core.GeneratorSettings
import dev.banking.asyncapi.generator.core.ProtocProvider
import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import dev.banking.asyncapi.generator.core.generator.GenerationPipeline
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationFactory
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.plan.GenerationPlanner
import dev.banking.asyncapi.generator.core.loader.AsyncApiDocumentLoader
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
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

    @get:Input
    @get:Optional
    abstract val modelAnnotation: Property<String>

    @get:Input
    @get:Optional
    abstract val modelType: Property<String>

    @get:Input
    @get:Optional
    abstract val clientType: Property<String>

    @get:Input
    @get:Optional
    abstract val clientContract: Property<String>

    @get:Input
    @get:Optional
    abstract val producerEnabled: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val producerAdditionalPayloadTypes: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val consumerEnabled: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val topicParameterProperties: MapProperty<String, String>

    @get:Input
    @get:Optional
    abstract val clientContractValidationAnnotation: Property<String>

    @get:Input
    @get:Optional
    abstract val payloadParameterValidationAnnotation: Property<String>

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

        val generatorConfiguration =
            GeneratorConfigurationFactory.create(
                settings().copy(
                    manifest = GeneratorSettings.Manifest(outputManifest.get().asFile, executionName.get()),
                ),
            )
        generatorConfiguration.formatWarnings().takeIf(String::isNotEmpty)?.let { logger.warn(it.trimEnd()) }
        val loaded = AsyncApiDocumentLoader().load(inputSpec.get().asFile)
        if (loaded.warnings.isNotEmpty()) {
            logger.warn(loaded.formatWarnings().trimEnd())
        }

        val bundled = AsyncApiBundler().bundle(loaded)
        GenerationPipeline(ProtocProvider { protoc.singleFile.toPath().also { it.toFile().setExecutable(true) } })
            .generate(bundled, generatorConfiguration)

        logger.lifecycle("AsyncAPI generation '$name' completed")
    }

    /**
     * The kinds of artifact this execution writes under [outputDirectory], which decide whether the
     * plugin compiles the directory, packages it as resources, or both. While the configuration is
     * invalid it is every kind: this task reports the configuration error itself when it runs.
     */
    fun outputKinds(): Set<GeneratedArtifactKind> =
        runCatching { GenerationPlanner().plan(GeneratorConfigurationFactory.create(settings())).outputKinds }
            .getOrDefault(GeneratedArtifactKind.entries.toSet())

    private fun settings(): GeneratorSettings =
        GeneratorSettings(
            generatorName = generatorName.orNull,
            outputDirectory = outputDirectory.get().asFile,
            outputFile = outputFile.orNull?.asFile,
            modelPackage = modelPackage.orNull,
            clientPackage = clientPackage.orNull,
            schemaPackage = schemaPackage.orNull,
            modelConfig = modelConfiguration(),
            clientConfig = clientConfiguration(),
            suppressedWarnings = suppressedWarnings.get().distinct(),
        )

    private fun modelConfiguration(): GeneratorSettings.ModelConfig? =
        if (modelAnnotation.isPresent || modelType.isPresent) {
            GeneratorSettings.ModelConfig(
                modelAnnotation = modelAnnotation.orNull,
                modelType = modelType.orNull,
            )
        } else {
            null
        }

    /** Client configuration is shared by all executions; it counts as configured once any property is set. */
    private fun clientConfiguration(): GeneratorSettings.ClientConfig? =
        if (
            clientType.isPresent ||
            clientContract.isPresent ||
            producerEnabled.isPresent ||
            consumerEnabled.isPresent ||
            producerAdditionalPayloadTypes.orNull.orEmpty().isNotEmpty() ||
            topicParameterProperties.orNull.orEmpty().isNotEmpty() ||
            clientContractValidationAnnotation.isPresent ||
            payloadParameterValidationAnnotation.isPresent
        ) {
            GeneratorSettings.ClientConfig(
                clientType = clientType.orNull,
                clientContract = clientContract.orNull,
                producer =
                    GeneratorSettings.ProducerConfig(
                        enabled = producerEnabled.orNull,
                        additionalPayloadTypes = producerAdditionalPayloadTypes.orNull,
                    ),
                consumer = GeneratorSettings.ConsumerConfig(enabled = consumerEnabled.orNull),
                topicParameterProperties = topicParameterProperties.orNull.orEmpty(),
                validationAnnotations = validationAnnotationsConfiguration(),
            )
        } else {
            null
        }

    private fun validationAnnotationsConfiguration(): GeneratorSettings.ValidationAnnotationsConfig? =
        if (
            clientContractValidationAnnotation.isPresent ||
            payloadParameterValidationAnnotation.isPresent
        ) {
            GeneratorSettings.ValidationAnnotationsConfig(
                clientContract = clientContractValidationAnnotation.orNull,
                payloadParameter = payloadParameterValidationAnnotation.orNull,
            )
        } else {
            null
        }
}
