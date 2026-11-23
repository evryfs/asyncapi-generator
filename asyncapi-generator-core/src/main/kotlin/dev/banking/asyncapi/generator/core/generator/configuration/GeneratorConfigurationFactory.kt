package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Turns [GeneratorSettings] into a typed [GeneratorConfiguration].
 *
 * Every check on the settings lives here, and every message names the
 * setting as users write it.
 */
object GeneratorConfigurationFactory {
    /** Checks [settings] and resolves them; throws [IllegalArgumentException] for the first invalid setting. */
    fun create(settings: GeneratorSettings): GeneratorConfiguration {
        validateClientConfigAndPaths(settings)
        val generatorName = GeneratorName.fromConfigurationValue(settings.generatorName, "generatorName")
        val configuredModelType =
            settings.modelConfig?.modelType?.let { ModelType.fromConfigurationValue(it, "modelConfig.modelType") }
        val clientContract = settings.clientConfig?.let { resolveClientContract(it, settings) }
        val warnings = warnings(settings)
        val generatesClient = settings.clientConfig != null && settings.clientPackage != null

        validateProfile(generatorName, settings)
        validateModelPackage(settings)
        settings.schemaPackage?.let { PackageName.fromConfigurationValue(it, "schemaPackage") }

        val modelPackage = settings.modelPackage
        val modelType = modelPackage?.let { ModelTypeResolver.resolve(generatorName, configuredModelType) }
        val protobufModels = protobufModels(generatorName, modelPackage, modelType)
        val configuration =
            GeneratorConfiguration(
                profile = generatorName.profile,
                output = output(settings, generatorName.profile),
                models = sourceModels(modelPackage, modelType, modelAnnotation(settings, modelType)),
                schemas = schemaGeneration(generatorName.profile, settings, modelType, protobufModels),
                client = if (generatesClient) client(settings, checkNotNull(clientContract)) else null,
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

    /**
     * Checks every client setting, including on an execution that generates no
     * client, and returns the contract shape.
     */
    private fun resolveClientContract(
        clientConfig: GeneratorSettings.ClientConfig,
        settings: GeneratorSettings,
    ): ResolvedClientContract {
        ClientType.fromConfigurationValue(clientConfig.clientType, "clientConfig.clientType")
        val clientContract =
            ClientContract.fromConfigurationValue(clientConfig.clientContract, "clientConfig.clientContract")
        validateClientPackages(settings)
        return ResolvedClientContract(clientContract, validationAnnotations(clientConfig.validationAnnotations))
    }

    private fun validateClientPackages(settings: GeneratorSettings) {
        settings.clientPackage?.let { PackageName.fromConfigurationValue(it, "clientPackage") }
        when {
            settings.modelPackage != null -> PackageName.fromConfigurationValue(settings.modelPackage, "modelPackage")
            settings.clientPackage != null ->
                throw IllegalArgumentException("modelPackage is required when clientPackage is configured")
        }
    }

    private fun validationAnnotations(
        annotations: GeneratorSettings.ValidationAnnotationsConfig?,
    ): ClientValidationAnnotations =
        ClientValidationAnnotations(
            clientContract =
                annotations?.clientContract?.let {
                    QualifiedTypeName.fromConfigurationValue(it, "clientConfig.validationAnnotations.clientContract")
                },
            payloadParameter =
                annotations?.payloadParameter?.let {
                    QualifiedTypeName.fromConfigurationValue(it, "clientConfig.validationAnnotations.payloadParameter")
                },
        )

    /** The configuration warnings, without the ones listed in `suppressedWarnings`. */
    private fun warnings(settings: GeneratorSettings): List<ConfigurationWarning> {
        val suppressed =
            settings.suppressedWarnings
                .map { ConfigurationWarningId.fromConfigurationValue(it, "suppressedWarnings") }
                .toSet()
        return listOfNotNull(unusedClientConfigWarning(settings)).filterNot { it.id in suppressed }
    }

    /**
     * Client configuration is usually shared by all executions of a build, so an
     * execution that only generates models or schemas legitimately inherits it.
     * It is ignored there, with a warning so it is never ignored silently.
     */
    private fun unusedClientConfigWarning(settings: GeneratorSettings): ConfigurationWarning? =
        if (settings.clientConfig != null && settings.clientPackage == null) {
            ConfigurationWarning(
                id = ConfigurationWarningId.UNUSED_CLIENT_CONFIG,
                message =
                    "clientConfig is ignored because clientPackage is not configured, so no client is generated. " +
                        "Add '${ConfigurationWarningId.UNUSED_CLIENT_CONFIG.configurationValue}' to " +
                        "suppressedWarnings to silence this warning.",
            )
        } else {
            null
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
                require(
                    settings.schemaPackage == null,
                ) { "schemaPackage cannot be configured when generatorName is $name" }
            }
        }
    }

    private fun rejectModelSettings(
        settings: GeneratorSettings,
        generatorName: String,
    ) {
        require(
            settings.modelPackage == null,
        ) { "modelPackage cannot be configured when generatorName is $generatorName" }
        require(
            settings.modelConfig == null,
        ) { "modelConfig cannot be configured when generatorName is $generatorName" }
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
            sourceOutputDirectory = settings.outputDirectory,
            javaSourceOutputDirectory = settings.outputDirectory,
            resourceOutputDirectory = settings.outputDirectory,
            manifest = settings.manifest,
            document =
                settings.outputFile?.let { outputFile ->
                    DocumentOutput(
                        file = outputFile,
                        format = (profile as? GeneratorProfile.Document)?.format ?: DocumentFormat.YAML,
                    )
                },
        )

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

    private fun sourceModels(
        modelPackage: String?,
        modelType: ModelType?,
        modelAnnotation: QualifiedTypeName?,
    ): ModelGeneration =
        if (modelPackage != null && modelType?.language != null) {
            ModelGeneration.Enabled(packageName = modelPackage, annotation = modelAnnotation, modelType = modelType)
        } else {
            ModelGeneration.Disabled
        }

    private fun protobufModels(
        generatorName: GeneratorName,
        modelPackage: String?,
        modelType: ModelType?,
    ): ProtobufModelGeneration? =
        modelPackage?.takeIf { modelType == ModelType.PROTOBUF_MESSAGE }?.let { packageName ->
            ProtobufModelGeneration(
                packageName = packageName,
                modelType =
                    when (checkNotNull(generatorName.sourceLanguage)) {
                        SourceLanguage.KOTLIN -> ProtobufModelType.KOTLIN
                        SourceLanguage.JAVA -> ProtobufModelType.JAVA
                    },
            )
        }

    private fun schemaGeneration(
        profile: GeneratorProfile,
        settings: GeneratorSettings,
        modelType: ModelType?,
        protobufModels: ProtobufModelGeneration?,
    ): List<SchemaGeneration> =
        when (profile) {
            is GeneratorProfile.Source -> nativeModelSchemas(settings, modelType, protobufModels)
            is GeneratorProfile.Schema -> schemaGeneratorSchemas(profile.type, checkNotNull(settings.schemaPackage))
            is GeneratorProfile.Document -> emptyList()
        }

    /** The schemas behind native Avro or Protobuf payload models of a source generator. */
    private fun nativeModelSchemas(
        settings: GeneratorSettings,
        modelType: ModelType?,
        protobufModels: ProtobufModelGeneration?,
    ): List<SchemaGeneration> =
        buildList {
            if (modelType == ModelType.AVRO_SPECIFIC_RECORD) {
                add(
                    SchemaGeneration.NativeAvro(
                        generateSpecificRecords = true,
                        modelPackageName = settings.modelPackage,
                        schemaPackageName = settings.schemaPackage,
                    ),
                )
            }
            if (protobufModels != null) {
                add(
                    SchemaGeneration.NativeProtobuf(
                        models = protobufModels,
                        schemaPackageName = settings.schemaPackage,
                    ),
                )
            }
        }

    private fun schemaGeneratorSchemas(
        type: SchemaType,
        schemaPackage: String,
    ): List<SchemaGeneration> =
        when (type) {
            SchemaType.AVRO ->
                listOf(
                    SchemaGeneration.AvroProjection(packageName = schemaPackage),
                    SchemaGeneration.NativeAvro(generateSpecificRecords = false, schemaPackageName = schemaPackage),
                )
            SchemaType.PROTOBUF -> listOf(SchemaGeneration.NativeProtobuf(schemaPackageName = schemaPackage))
            SchemaType.JSON_SCHEMA -> listOf(SchemaGeneration.JsonSchema(packageName = schemaPackage))
        }

    private fun client(
        settings: GeneratorSettings,
        contract: ResolvedClientContract,
    ): ClientGeneration {
        val clientConfig = checkNotNull(settings.clientConfig)
        return ClientGeneration(
            packageName = checkNotNull(settings.clientPackage),
            modelPackageName = checkNotNull(settings.modelPackage),
            clientContract = contract.clientContract,
            topicParameterProperties =
                TopicParameterProperties.fromConfigurationValues(
                    values = clientConfig.topicParameterProperties,
                    path = "clientConfig.topicParameterProperties",
                ),
            validationAnnotations = contract.validationAnnotations,
            producer =
                ClientGeneration.Producer(
                    enabled = clientConfig.producer?.enabled ?: true,
                    additionalPayloadTypes =
                        AdditionalProducerPayloadType.fromConfigurationValues(
                            values = clientConfig.producer?.additionalPayloadTypes,
                            path = "clientConfig.producer.additionalPayloadTypes",
                        ),
                ),
            consumer = ClientGeneration.Consumer(enabled = clientConfig.consumer?.enabled ?: true),
        )
    }

    /**
     * The client settings that are checked even when no client is generated.
     *
     * @param clientContract the resolved contract shape.
     * @param validationAnnotations the resolved validation annotations.
     */
    private data class ResolvedClientContract(
        val clientContract: ClientContract,
        val validationAnnotations: ClientValidationAnnotations,
    )
}
