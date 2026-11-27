package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.GeneratorSettings
import dev.banking.asyncapi.generator.core.generator.output.GeneratedOutputManifest

/**
 * Turns [GeneratorSettings] into a typed [GeneratorConfiguration].
 *
 * Every check on the settings happens here or in the resolvers this factory
 * calls, and every message names the setting as users write it.
 */
object GeneratorConfigurationFactory {
    /** Checks [settings] and resolves them; throws [IllegalArgumentException] for the first invalid setting. */
    fun create(settings: GeneratorSettings): GeneratorConfiguration {
        validateClientConfigAndPaths(settings)
        val generatorName = GeneratorName.fromConfigurationValue(settings.generatorName, "generatorName")
        val configuredModelType =
            settings.modelConfig?.modelType?.let { ModelType.fromConfigurationValue(it, "modelConfig.modelType") }
        val checkedClientConfig = ClientConfigurationResolver.check(settings)
        val warnings = warnings(settings)

        validateProfile(generatorName, settings)
        validateModelPackage(settings)
        settings.schemaPackage?.let { PackageName.fromConfigurationValue(it, "schemaPackage") }

        val modelType = settings.modelPackage?.let { ModelTypeResolver.resolve(generatorName, configuredModelType) }
        val configuration =
            GeneratorConfiguration(
                profile = generatorName.profile,
                output = output(settings, generatorName.profile),
                models = sourceModels(settings, modelType),
                schemas = SchemaConfigurationResolver.schemas(generatorName.profile, settings, modelType),
                client = ClientConfigurationResolver.client(settings, checkedClientConfig),
                warnings = warnings,
            )

        validateActivatedOutputs(settings, configuration)
        return configuration
    }

    private fun validateClientConfigAndPaths(settings: GeneratorSettings) {
        require(settings.clientPackage == null || settings.clientConfig != null) {
            "clientConfig is required when clientPackage is configured"
        }
        require(!settings.outputDirectory.exists() || settings.outputDirectory.isDirectory) {
            "outputDirectory must be a directory: ${settings.outputDirectory}"
        }
        require(settings.outputFile?.isDirectory != true) {
            "outputFile must be a file: ${settings.outputFile}"
        }
    }

    /** The configuration warnings, without the ones listed in `suppressedWarnings`. */
    private fun warnings(settings: GeneratorSettings): List<ConfigurationWarning> {
        val suppressed =
            settings.suppressedWarnings
                .map { ConfigurationWarningId.fromConfigurationValue(it, "suppressedWarnings") }
                .toSet()
        return listOfNotNull(ClientConfigurationResolver.unusedClientConfigWarning(settings))
            .filterNot { it.id in suppressed }
    }

    private fun validateProfile(
        generatorName: GeneratorName,
        settings: GeneratorSettings,
    ) {
        val name = generatorName.configurationValue
        when (generatorName.profile) {
            is GeneratorProfile.Source -> Unit
            is GeneratorProfile.Schema -> {
                rejectModelSettings(settings, name)
                requireNotNull(settings.schemaPackage) { "schemaPackage is required when generatorName is $name" }
            }
            is GeneratorProfile.Document -> {
                requireNotNull(settings.outputFile) { "outputFile is required when generatorName is $name" }
                rejectModelSettings(settings, name)
                require(settings.schemaPackage == null) {
                    "schemaPackage cannot be configured when generatorName is $name"
                }
            }
        }
    }

    private fun rejectModelSettings(
        settings: GeneratorSettings,
        generatorName: String,
    ) {
        require(settings.modelPackage == null) {
            "modelPackage cannot be configured when generatorName is $generatorName"
        }
        require(settings.modelConfig == null) {
            "modelConfig cannot be configured when generatorName is $generatorName"
        }
    }

    private fun validateModelPackage(settings: GeneratorSettings) {
        require(settings.modelConfig == null || settings.modelPackage != null) {
            "modelPackage is required when modelConfig is configured"
        }
        settings.modelPackage?.let { PackageName.fromConfigurationValue(it, "modelPackage") }
    }

    private fun validateActivatedOutputs(
        settings: GeneratorSettings,
        configuration: GeneratorConfiguration,
    ) {
        require(settings.schemaPackage == null || configuration.schemas.isNotEmpty()) {
            "schemaPackage is only supported by schema generator profiles and native Avro or Protobuf models"
        }
        require(configuration.hasConfiguredOutputs()) {
            "No generator output is configured. Configure modelPackage, clientPackage with clientConfig, " +
                "schemaPackage with a schema generator, or outputFile."
        }
    }

    private fun output(
        settings: GeneratorSettings,
        profile: GeneratorProfile,
    ): GeneratorOutputConfiguration =
        GeneratorOutputConfiguration(
            outputDirectory = settings.outputDirectory,
            manifest = settings.manifest?.let { GeneratedOutputManifest(it.file, it.executionName) },
            document =
                settings.outputFile?.let { outputFile ->
                    DocumentOutput(
                        file = outputFile,
                        format = (profile as? GeneratorProfile.Document)?.format ?: DocumentFormat.YAML,
                    )
                },
        )

    /** Source models, which only model types written by a source generator produce. */
    private fun sourceModels(
        settings: GeneratorSettings,
        modelType: ModelType?,
    ): ModelGeneration {
        val modelPackage = settings.modelPackage
        val annotation = modelAnnotation(settings, modelType)
        return if (modelPackage != null && modelType?.language != null) {
            ModelGeneration.Enabled(packageName = modelPackage, annotation = annotation, modelType = modelType)
        } else {
            ModelGeneration.Disabled
        }
    }

    private fun modelAnnotation(
        settings: GeneratorSettings,
        modelType: ModelType?,
    ): QualifiedTypeName? {
        val annotation = settings.modelConfig?.modelAnnotation ?: return null
        require(modelType?.supportsAnnotation == true) {
            val annotated = ModelType.entries.filter { it.supportsAnnotation }.map { it.configurationValue }
            "modelConfig.modelAnnotation is only supported for " +
                annotated.dropLast(1).joinToString(", ") + ", and ${annotated.last()} model types"
        }
        return QualifiedTypeName.fromConfigurationValue(annotation, "modelConfig.modelAnnotation")
    }
}
