package dev.banking.asyncapi.generator.core.generator.avro

import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.NativeAvroSpecificRecordGenerationFailed
import org.apache.avro.Schema
import org.apache.avro.compiler.specific.SpecificCompiler
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Compiles native Avro schemas into Java SpecificRecord sources with the Avro compiler, in a
 * temporary workspace that is removed afterwards. Only records, enums and fixed types have one.
 *
 * @param schemaJson writes a schema as the JSON file the compiler reads
 */
internal class SpecificRecordCompiler(
    private val schemaJson: (Schema) -> String,
) {
    /**
     * The SpecificRecord sources of [schema]; any failure fails generation of the payload.
     *
     * @param payloadName the payload the schema belongs to, for error messages
     * @param schemaFormat the `schemaFormat` of the payload, for error messages
     * @param schema the parsed Avro schema
     */
    fun compile(
        payloadName: String,
        schemaFormat: String,
        schema: Schema,
    ): List<GeneratedArtifact> {
        if (schema.type !in COMPILED_TYPES) {
            return emptyList()
        }
        return failingAsGeneration(payloadName, schemaFormat) {
            val workspaceDirectory = Files.createTempDirectory("asyncapi-native-avro-")
            try {
                compileIn(schema, workspaceDirectory)
            } finally {
                workspaceDirectory.toFile().deleteRecursively()
            }
        }
    }

    private fun compileIn(
        schema: Schema,
        workspaceDirectory: Path,
    ): List<GeneratedArtifact> {
        val sourceSchemaFile = workspaceDirectory.resolve("schema.avsc")
        val destinationDirectory = Files.createDirectory(workspaceDirectory.resolve("generated"))
        Files.writeString(sourceSchemaFile, schemaJson(schema))
        SpecificCompiler(schema).compileToDestination(sourceSchemaFile.toFile(), destinationDirectory.toFile())
        return generatedJavaFiles(destinationDirectory).map { sourceFile ->
            GeneratedArtifact(
                relativePath = destinationDirectory.relativize(sourceFile).toString().replace(File.separatorChar, '/'),
                content = Files.readString(sourceFile).trimEnd() + System.lineSeparator(),
                kind = GeneratedArtifactKind.SOURCE,
            )
        }
    }

    private fun generatedJavaFiles(directory: Path): List<Path> =
        Files.walk(directory).use { paths ->
            paths
                .filter { path -> Files.isRegularFile(path) && path.fileName.toString().endsWith(".java") }
                .sorted()
                .toList()
        }

    // The Avro compiler reports failures with several unchecked exception types, and the workspace
    // with I/O exceptions; each one fails generation of this payload.
    @Suppress("TooGenericExceptionCaught")
    private fun <T> failingAsGeneration(
        payloadName: String,
        schemaFormat: String,
        generation: () -> T,
    ): T =
        try {
            generation()
        } catch (ex: IOException) {
            throw failed(payloadName, schemaFormat, ex)
        } catch (ex: RuntimeException) {
            throw failed(payloadName, schemaFormat, ex)
        }

    private fun failed(
        payloadName: String,
        schemaFormat: String,
        ex: Exception,
    ) = NativeAvroSpecificRecordGenerationFailed(
        payloadName = payloadName,
        schemaFormat = schemaFormat,
        reason = ex.message ?: ex::class.simpleName.orEmpty(),
    )

    private companion object {
        val COMPILED_TYPES = setOf(Schema.Type.RECORD, Schema.Type.ENUM, Schema.Type.FIXED)
    }
}
