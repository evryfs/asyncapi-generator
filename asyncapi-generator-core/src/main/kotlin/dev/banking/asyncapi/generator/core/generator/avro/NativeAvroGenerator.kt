package dev.banking.asyncapi.generator.core.generator.avro

import com.fasterxml.jackson.databind.ObjectMapper
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidNativeAvroSchema
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.NativeAvroSpecificRecordGenerationFailed
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import org.apache.avro.Schema
import org.apache.avro.compiler.specific.SpecificCompiler
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Renders native Avro `schemaFormat` payloads into `.avsc` and SpecificRecord artifacts.
 */
class NativeAvroGenerator(
    private val objectMapper: ObjectMapper = ObjectMapper(),
    private val schemaParser: NativeAvroSchemaParser = NativeAvroSchemaParser(objectMapper),
) {
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
                        avroSchema = parseSchema(payloadName, schema),
                    )
                }

        val schemaArtifacts = parsedSchemas.map(::renderSchemaArtifact)
        val specificRecordArtifacts =
            if (generateSpecificRecords) {
                parsedSchemas.flatMap(::renderSpecificRecordArtifacts)
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

    private fun renderSpecificRecordArtifacts(parsedSchema: ParsedNativeAvroSchema): List<GeneratedArtifact> {
        if (!parsedSchema.avroSchema.supportsSpecificRecordGeneration()) {
            return emptyList()
        }

        val workspaceDirectory =
            try {
                Files.createTempDirectory("asyncapi-native-avro-")
            } catch (ex: IOException) {
                throw specificRecordGenerationFailed(parsedSchema, ex)
            }
        val sourceSchemaFile = workspaceDirectory.resolve("schema.avsc")
        val destinationDirectory = workspaceDirectory.resolve("generated")

        try {
            Files.createDirectory(destinationDirectory)
            Files.writeString(sourceSchemaFile, prettySchemaJson(parsedSchema.avroSchema))
            SpecificCompiler(parsedSchema.avroSchema)
                .compileToDestination(sourceSchemaFile.toFile(), destinationDirectory.toFile())

            return generatedJavaFiles(destinationDirectory)
                .map { sourceFile ->
                    GeneratedArtifact(
                        relativePath = destinationDirectory.relativeUnixPathTo(sourceFile),
                        content = Files.readString(sourceFile).trimEnd() + System.lineSeparator(),
                        kind = GeneratedArtifactKind.JAVA_SOURCE,
                    )
                }
        } catch (ex: IOException) {
            throw specificRecordGenerationFailed(parsedSchema, ex)
        } catch (ex: RuntimeException) {
            throw specificRecordGenerationFailed(parsedSchema, ex)
        } finally {
            workspaceDirectory.toFile().deleteRecursively()
        }
    }

    private fun parseSchema(
        payloadName: String,
        schema: MultiFormatSchema,
    ): Schema = schemaParser.parse(payloadName, schema)

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

    private fun Schema.supportsSpecificRecordGeneration(): Boolean =
        type == Schema.Type.RECORD ||
            type == Schema.Type.ENUM ||
            type == Schema.Type.FIXED

    private fun generatedJavaFiles(directory: Path): List<Path> =
        Files.walk(directory).use { paths ->
            paths
                .filter { path -> Files.isRegularFile(path) && path.fileName.toString().endsWith(".java") }
                .sorted()
                .toList()
        }

    private fun Path.relativeUnixPathTo(file: Path): String =
        relativize(file).toString().replace(File.separatorChar, '/')

    private fun specificRecordGenerationFailed(
        parsedSchema: ParsedNativeAvroSchema,
        ex: Exception,
    ): NativeAvroSpecificRecordGenerationFailed =
        NativeAvroSpecificRecordGenerationFailed(
            payloadName = parsedSchema.payloadName,
            schemaFormat = parsedSchema.schema.schemaFormat,
            reason = ex.message ?: ex::class.simpleName.orEmpty(),
        )

    private data class ParsedNativeAvroSchema(
        val payloadName: String,
        val schema: MultiFormatSchema,
        val avroSchema: Schema,
    )
}
