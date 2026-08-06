package dev.banking.asyncapi.generator.gradle.plugin.tasks

import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import dev.banking.asyncapi.generator.core.generator.AsyncApiGenerator
import dev.banking.asyncapi.generator.core.loader.AsyncApiDocumentLoader
import dev.banking.asyncapi.generator.gradle.plugin.GradleClientConfiguration
import dev.banking.asyncapi.generator.gradle.plugin.GradleConsumerConfiguration
import dev.banking.asyncapi.generator.gradle.plugin.GradleGeneratorConfigurationMapper
import dev.banking.asyncapi.generator.gradle.plugin.GradleGeneratorConfigurationRequest
import dev.banking.asyncapi.generator.gradle.plugin.GradleModelConfiguration
import dev.banking.asyncapi.generator.gradle.plugin.GradleProducerConfiguration
import dev.banking.asyncapi.generator.gradle.plugin.GradleValidationAnnotationsConfiguration
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
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Generates the outputs configured by one named AsyncAPI execution.
 *
 * Expected behavior is covered by:
 * - `AsyncApiPluginTest`
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

    @TaskAction
    fun generate() {
        logger.lifecycle("AsyncAPI generation '${name}' started")

        val generatorConfiguration =
            GradleGeneratorConfigurationMapper.map(
                GradleGeneratorConfigurationRequest(
                    generatorName = generatorName.orNull,
                    outputDirectory = outputDirectory.get().asFile,
                    outputFile = outputFile.orNull?.asFile,
                    modelPackage = modelPackage.orNull,
                    clientPackage = clientPackage.orNull,
                    schemaPackage = schemaPackage.orNull,
                    modelConfig = modelConfiguration(),
                    clientConfig = clientConfiguration(),
                ),
            )
        val loaded = AsyncApiDocumentLoader().load(inputSpec.get().asFile)
        if (loaded.warnings.isNotEmpty()) {
            logger.warn(loaded.formatWarnings().trimEnd())
        }

        val bundled = AsyncApiBundler().bundle(loaded.document)
        AsyncApiGenerator().generate(bundled, generatorConfiguration)

        logger.lifecycle("AsyncAPI generation '${name}' completed")
    }

    private fun modelConfiguration(): GradleModelConfiguration? =
        if (modelPackage.isPresent || modelAnnotation.isPresent || modelType.isPresent) {
            GradleModelConfiguration(
                modelAnnotation = modelAnnotation.orNull,
                modelType = modelType.orNull,
            )
        } else {
            null
        }

    private fun clientConfiguration(): GradleClientConfiguration? =
        if (clientPackage.isPresent) {
            GradleClientConfiguration(
                clientType = clientType.orNull,
                clientContract = clientContract.orNull,
                producer =
                    GradleProducerConfiguration(
                        enabled = producerEnabled.orNull,
                        additionalPayloadTypes = producerAdditionalPayloadTypes.orNull,
                    ),
                consumer = GradleConsumerConfiguration(enabled = consumerEnabled.orNull),
                topicParameterProperties = topicParameterProperties.orNull.orEmpty(),
                validationAnnotations = validationAnnotationsConfiguration(),
            )
        } else {
            null
        }

    private fun validationAnnotationsConfiguration(): GradleValidationAnnotationsConfiguration? =
        if (
            clientContractValidationAnnotation.isPresent ||
            payloadParameterValidationAnnotation.isPresent
        ) {
            GradleValidationAnnotationsConfiguration(
                clientContract = clientContractValidationAnnotation.orNull,
                payloadParameter = payloadParameterValidationAnnotation.orNull,
            )
        } else {
            null
        }
}
