package dev.banking.asyncapi.generator.core.generator.plan

import dev.banking.asyncapi.generator.core.generator.configuration.ClientGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.DocumentOutput
import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.SchemaGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind

/**
 * Planned generator work item.
 *
 * A task refers to the configuration value it plans, so a setting is defined
 * once; the planner adds only what it decides itself.
 */
internal sealed interface GenerationTask {
    /**
     * The kinds of artifact the task writes under the output directory, which tell a build tool
     * whether to compile the directory, package it as resources, or both. A bundled document is
     * written to its own file and is no kind.
     */
    val outputKinds: Set<GeneratedArtifactKind>

    /**
     * Writes the bundled AsyncAPI document.
     *
     * @param document the file and format to write.
     */
    data class DocumentArtifact(
        val document: DocumentOutput,
    ) : GenerationTask {
        override val outputKinds: Set<GeneratedArtifactKind>
            get() = emptySet()
    }

    /**
     * Writes source models for the payload schemas.
     *
     * @param models the configured source models.
     */
    data class ModelArtifacts(
        val models: ModelGeneration.Enabled,
    ) : GenerationTask {
        override val outputKinds: Set<GeneratedArtifactKind>
            get() = setOf(GeneratedArtifactKind.SOURCE)

        /** The language whose generator writes the model type. */
        val language: SourceLanguage
            get() =
                checkNotNull(models.modelType.language) {
                    "Model type '${models.modelType.configurationValue}' has no source language"
                }
    }

    /**
     * Writes source models for Kafka message keys next to native payload models.
     *
     * @param language the language of the generated models.
     * @param packageName the package of the generated models.
     */
    data class KafkaKeyModelArtifacts(
        val language: SourceLanguage,
        val packageName: String,
    ) : GenerationTask {
        override val outputKinds: Set<GeneratedArtifactKind>
            get() = setOf(GeneratedArtifactKind.SOURCE)
    }

    /**
     * Writes Spring Kafka producer and consumer contracts.
     *
     * @param language the language of the generated contracts.
     * @param client the configured client.
     */
    data class SpringKafkaClient(
        val language: SourceLanguage,
        val client: ClientGeneration,
    ) : GenerationTask {
        override val outputKinds: Set<GeneratedArtifactKind>
            get() = setOf(GeneratedArtifactKind.SOURCE)
    }

    /**
     * Projects AsyncAPI Schema Objects to `.avsc` files.
     *
     * @param schema the configured projection.
     */
    data class AvroSchemaArtifacts(
        val schema: SchemaGeneration.AvroProjection,
    ) : GenerationTask {
        override val outputKinds: Set<GeneratedArtifactKind>
            get() = setOf(GeneratedArtifactKind.SCHEMA)
    }

    /**
     * Copies native Avro payload schemas, optionally with SpecificRecord models.
     *
     * @param schema the configured native Avro generation.
     */
    data class NativeAvroArtifacts(
        val schema: SchemaGeneration.NativeAvro,
    ) : GenerationTask {
        override val outputKinds: Set<GeneratedArtifactKind>
            get() = withSources(schema.generateSpecificRecords)
    }

    /**
     * Copies native Protobuf payload schemas, optionally with compiled models.
     *
     * @param schema the configured native Protobuf generation.
     */
    data class NativeProtobufArtifacts(
        val schema: SchemaGeneration.NativeProtobuf,
    ) : GenerationTask {
        override val outputKinds: Set<GeneratedArtifactKind>
            get() = withSources(schema.models != null)
    }

    /**
     * Writes Draft 07 JSON Schema files.
     *
     * @param schema the configured JSON Schema generation.
     */
    data class JsonSchemaArtifacts(
        val schema: SchemaGeneration.JsonSchema,
    ) : GenerationTask {
        override val outputKinds: Set<GeneratedArtifactKind>
            get() = setOf(GeneratedArtifactKind.SCHEMA)
    }
}

/** Schemas, plus the sources compiled from them when [compiled]. */
private fun withSources(compiled: Boolean): Set<GeneratedArtifactKind> =
    if (compiled) {
        setOf(GeneratedArtifactKind.SCHEMA, GeneratedArtifactKind.SOURCE)
    } else {
        setOf(GeneratedArtifactKind.SCHEMA)
    }
