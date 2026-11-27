package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.GeneratorSettings

/**
 * Selects the schema artifacts a configuration generates.
 *
 * A schema generator selects them by its schema type. A source generator
 * generates schemas only behind native Avro or Protobuf payload models.
 */
internal object SchemaConfigurationResolver {
    /** The schema artifacts for [profile], given the resolved [modelType] of a source generator. */
    fun schemas(
        profile: GeneratorProfile,
        settings: GeneratorSettings,
        modelType: ModelType?,
    ): List<SchemaGeneration> =
        when (profile) {
            is GeneratorProfile.Source -> nativeModelSchemas(profile.language, settings, modelType)
            is GeneratorProfile.Schema -> schemaGeneratorSchemas(profile.type, checkNotNull(settings.schemaPackage))
            is GeneratorProfile.Document -> emptyList()
        }

    private fun nativeModelSchemas(
        language: SourceLanguage,
        settings: GeneratorSettings,
        modelType: ModelType?,
    ): List<SchemaGeneration> =
        when (modelType) {
            ModelType.AVRO_SPECIFIC_RECORD ->
                listOf(
                    SchemaGeneration.NativeAvro(
                        generateSpecificRecords = true,
                        modelPackageName = settings.modelPackage,
                        schemaPackageName = settings.schemaPackage,
                    ),
                )
            ModelType.PROTOBUF_MESSAGE ->
                listOf(
                    SchemaGeneration.NativeProtobuf(
                        models =
                            ProtobufModelGeneration(
                                checkNotNull(settings.modelPackage),
                                protobufModelType(language),
                            ),
                        schemaPackageName = settings.schemaPackage,
                    ),
                )
            ModelType.KOTLIN_DATA_CLASS,
            ModelType.JAVA_CLASS,
            ModelType.JAVA_RECORD,
            null,
            -> emptyList()
        }

    private fun protobufModelType(language: SourceLanguage): ProtobufModelType =
        when (language) {
            SourceLanguage.KOTLIN -> ProtobufModelType.KOTLIN
            SourceLanguage.JAVA -> ProtobufModelType.JAVA
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
}
