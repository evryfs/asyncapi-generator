package dev.banking.asyncapi.generator.core.generator.avro

import com.fasterxml.jackson.databind.ObjectMapper
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import org.apache.avro.Schema

/**
 * Renders native Avro `schemaFormat` payloads into `.avsc` and SpecificRecord artifacts.
 *
 * @param objectMapper formats the schema JSON
 * @param schemaParser parses the native schemas
 */
internal class NativeAvroGenerator(
    private val objectMapper: ObjectMapper = ObjectMapper(),
    private val schemaParser: NativeAvroSchemaParser = NativeAvroSchemaParser(objectMapper),
) {
    private val specificRecordCompiler = SpecificRecordCompiler(::prettySchemaJson)

    fun render(
        schemas: Map<String, MultiFormatSchema>,
        generateSpecificRecords: Boolean = false,
    ): GenerationResult {
        val parsedSchemas =
            schemas.entries
                .filter { (_, schema) -> schema.format.isNativeAvro }
                .sortedBy(Map.Entry<String, MultiFormatSchema>::key)
                .map { (payloadName, schema) ->
                    ParsedNativeAvroSchema(
                        payloadName = payloadName,
                        schema = schema,
                        avroSchema = schemaParser.parse(payloadName, schema),
                    )
                }

        val schemaArtifacts = parsedSchemas.map(::renderSchemaArtifact)
        val specificRecordArtifacts =
            if (generateSpecificRecords) {
                parsedSchemas.flatMap { parsed ->
                    specificRecordCompiler.compile(parsed.payloadName, parsed.schema.schemaFormat, parsed.avroSchema)
                }
            } else {
                emptyList()
            }

        return GenerationResult(schemaArtifacts + specificRecordArtifacts)
    }

    private fun renderSchemaArtifact(parsedSchema: ParsedNativeAvroSchema): GeneratedArtifact {
        val fileName = "${artifactName(parsedSchema.payloadName, parsedSchema.avroSchema)}.avsc"
        val relativePath =
            GeneratedArtifactPaths.fromNamespace(
                namespace = parsedSchema.avroSchema.namespace.orEmpty(),
                fileName = fileName,
            )

        return GeneratedArtifact(
            relativePath = relativePath,
            content = prettySchemaJson(parsedSchema.avroSchema) + System.lineSeparator(),
            kind = GeneratedArtifactKind.SCHEMA,
        )
    }

    private fun prettySchemaJson(schema: Schema): String =
        objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(objectMapper.readValue(schema.toString(), Any::class.java))

    private fun artifactName(
        payloadName: String,
        schema: Schema,
    ): String =
        when (schema.type) {
            Schema.Type.RECORD,
            Schema.Type.ENUM,
            Schema.Type.FIXED,
            -> schema.name
            else -> payloadName
        }

    /**
     * A native Avro payload and its parsed schema.
     *
     * @property payloadName the payload name
     * @property schema the Multi Format Schema of the payload
     * @property avroSchema the schema parsed by Apache Avro
     */
    private data class ParsedNativeAvroSchema(
        val payloadName: String,
        val schema: MultiFormatSchema,
        val avroSchema: Schema,
    )
}
