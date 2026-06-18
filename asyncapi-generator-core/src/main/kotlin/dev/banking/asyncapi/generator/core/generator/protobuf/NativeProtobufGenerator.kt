package dev.banking.asyncapi.generator.core.generator.protobuf

import com.github.os72.protocjar.Protoc
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidNativeProtobufSchema
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.NativeProtobufJavaGenerationFailed
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists

/**
 * Renders native Protobuf `schemaFormat` payloads into `.proto` and Java
 * message artifacts.
 *
 * Expected behavior is covered by:
 * - `NativeProtobufGeneratorTest`
 */
class NativeProtobufGenerator(
    private val protocVersion: String = DEFAULT_PROTOC_VERSION,
) {
    private val schemaParser = NativeProtobufSchemaParser()

    fun render(
        schemas: Map<String, MultiFormatSchema>,
        generateJavaMessageTypes: Boolean = false,
    ): GenerationResult {
        val parsedSchemas =
            schemas
                .filter { (_, schema) -> schema.format.isNativeProtobuf }
                .map { (payloadName, schema) ->
                    ParsedNativeProtobufSchema(
                        payloadName = payloadName,
                        schema = schema,
                        protobufSchema = schemaParser.parse(payloadName, schema),
                    )
                }

        val schemaArtifacts = parsedSchemas.map(::renderSchemaArtifact)
        val javaArtifacts =
            if (generateJavaMessageTypes) {
                parsedSchemas.flatMap(::renderJavaMessageArtifacts)
            } else {
                emptyList()
            }

        return GenerationResult(schemaArtifacts + javaArtifacts)
    }

    private fun renderSchemaArtifact(parsedSchema: ParsedNativeProtobufSchema): GeneratedArtifact {
        val namespace = parsedSchema.protobufSchema.protoPackageName.orEmpty()

        return GeneratedArtifact(
            relativePath =
                GeneratedArtifactPaths.fromNamespace(
                    namespace = namespace,
                    fileName = "${parsedSchema.payloadName}.proto",
                ),
            content = parsedSchema.protobufSchema.content.trimEnd() + System.lineSeparator(),
            kind = GeneratedArtifactKind.SCHEMA,
        )
    }

    private fun renderJavaMessageArtifacts(parsedSchema: ParsedNativeProtobufSchema): List<GeneratedArtifact> {
        validateJavaMessageGenerationSupport(parsedSchema)

        val sourceDirectory = Files.createTempDirectory("asyncapi-native-protobuf-schemas-")
        val sourceSchemaFile = sourceDirectory.resolve("${parsedSchema.payloadName}.proto")
        val destinationDirectory = Files.createTempDirectory("asyncapi-native-protobuf-java-")

        try {
            Files.writeString(sourceSchemaFile, parsedSchema.protobufSchema.content.trimEnd() + System.lineSeparator())
            val exitCode =
                Protoc.runProtoc(
                    arrayOf(
                        "-v$protocVersion",
                        "--proto_path=${sourceDirectory.toAbsolutePath()}",
                        "--java_out=${destinationDirectory.toAbsolutePath()}",
                        sourceSchemaFile.fileName.toString(),
                    ),
                )

            if (exitCode != 0) {
                throw NativeProtobufJavaGenerationFailed(
                    payloadName = parsedSchema.payloadName,
                    schemaFormat = parsedSchema.schema.schemaFormat,
                    reason = "protoc exited with status code $exitCode.",
                )
            }

            return generatedJavaFiles(destinationDirectory)
                .map { sourceFile ->
                    GeneratedArtifact(
                        relativePath = destinationDirectory.relativeUnixPathTo(sourceFile),
                        content = Files.readString(sourceFile).trimEnd() + System.lineSeparator(),
                        kind = GeneratedArtifactKind.JAVA_SOURCE,
                    )
                }
        } catch (ex: IOException) {
            throw protobufJavaGenerationFailed(parsedSchema, ex)
        } catch (ex: RuntimeException) {
            throw protobufJavaGenerationFailed(parsedSchema, ex)
        } finally {
            sourceSchemaFile.deleteIfExists()
            sourceDirectory.toFile().deleteRecursively()
            destinationDirectory.toFile().deleteRecursively()
        }
    }

    private fun validateJavaMessageGenerationSupport(parsedSchema: ParsedNativeProtobufSchema) {
        val protobufSchema = parsedSchema.protobufSchema
        val payloadName = parsedSchema.payloadName
        val schemaFormat = parsedSchema.schema.schemaFormat

        if (protobufSchema.javaPackageName.isNullOrBlank() && protobufSchema.protoPackageName.isNullOrBlank()) {
            throw InvalidNativeProtobufSchema(
                payloadName = payloadName,
                schemaFormat = schemaFormat,
                reason = "Java Protobuf message generation requires either `option java_package = \"...\";` or a `package ...;` declaration.",
            )
        }

        if (protobufSchema.javaMultipleFiles != true) {
            throw InvalidNativeProtobufSchema(
                payloadName = payloadName,
                schemaFormat = schemaFormat,
                reason = "Java Protobuf message generation requires `option java_multiple_files = true;` so the payload message is generated as a top-level Java class.",
            )
        }

        if (payloadName !in protobufSchema.messageNames) {
            throw InvalidNativeProtobufSchema(
                payloadName = payloadName,
                schemaFormat = schemaFormat,
                reason = "Java Protobuf message generation requires a top-level message named '$payloadName'.",
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

    private fun Path.relativeUnixPathTo(file: Path): String =
        relativize(file).toString().replace(File.separatorChar, '/')

    private fun protobufJavaGenerationFailed(
        parsedSchema: ParsedNativeProtobufSchema,
        exception: Exception,
    ): NativeProtobufJavaGenerationFailed =
        NativeProtobufJavaGenerationFailed(
            payloadName = parsedSchema.payloadName,
            schemaFormat = parsedSchema.schema.schemaFormat,
            reason = exception.message ?: exception::class.simpleName.orEmpty(),
        )

    private companion object {
        const val DEFAULT_PROTOC_VERSION = "3.25.5"
    }
}

private data class ParsedNativeProtobufSchema(
    val payloadName: String,
    val schema: MultiFormatSchema,
    val protobufSchema: NativeProtobufSchema,
)
