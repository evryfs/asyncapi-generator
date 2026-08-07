package dev.banking.asyncapi.generator.core.generator.protobuf

import com.github.os72.protocjar.Protoc
import dev.banking.asyncapi.generator.core.generator.configuration.ProtobufModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.ProtobufModelType
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidNativeProtobufSchema
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.NativeProtobufModelGenerationFailed
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Renders native Protobuf `schemaFormat` payloads into `.proto` artifacts and
 * optional Java or Kotlin model APIs.
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
        models: ProtobufModelGeneration? = null,
    ): GenerationResult {
        val parsedSchemas =
            schemas.entries
                .filter { (_, schema) -> schema.format.isNativeProtobuf }
                .sortedBy(Map.Entry<String, MultiFormatSchema>::key)
                .map { (payloadName, schema) ->
                    ParsedNativeProtobufSchema(
                        payloadName = payloadName,
                        schema = schema,
                        protobufSchema = schemaParser.parse(payloadName, schema),
                    )
                }

        val schemaArtifacts = parsedSchemas.map(::renderSchemaArtifact)
        val modelArtifacts =
            if (models != null) {
                parsedSchemas.flatMap { parsedSchema -> renderModelArtifacts(parsedSchema, models) }
            } else {
                emptyList()
            }

        return GenerationResult(schemaArtifacts + modelArtifacts)
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

    private fun renderModelArtifacts(
        parsedSchema: ParsedNativeProtobufSchema,
        models: ProtobufModelGeneration,
    ): List<GeneratedArtifact> {
        validateModelGenerationSupport(parsedSchema, models)

        val workspaceDirectory =
            try {
                Files.createTempDirectory("asyncapi-native-protobuf-")
            } catch (ex: IOException) {
                throw protobufModelGenerationFailed(parsedSchema, models, ex)
            }
        val sourceDirectory = workspaceDirectory.resolve("schemas")
        val sourceSchemaFile = sourceDirectory.resolve("${parsedSchema.payloadName}.proto")
        val destinationDirectory = workspaceDirectory.resolve("java")
        val kotlinDestinationDirectory =
            models.modelType
                .takeIf { it == ProtobufModelType.KOTLIN }
                ?.let { workspaceDirectory.resolve("kotlin") }

        try {
            Files.createDirectory(sourceDirectory)
            Files.createDirectory(destinationDirectory)
            kotlinDestinationDirectory?.let(Files::createDirectory)
            Files.writeString(sourceSchemaFile, parsedSchema.protobufSchema.content.trimEnd() + System.lineSeparator())
            val compilerArguments =
                buildList {
                    add("-v$protocVersion")
                    add("--proto_path=${sourceDirectory.toAbsolutePath()}")
                    add("--java_out=${destinationDirectory.toAbsolutePath()}")
                    kotlinDestinationDirectory?.let { add("--kotlin_out=${it.toAbsolutePath()}") }
                    add(sourceSchemaFile.fileName.toString())
                }
            val exitCode =
                Protoc.runProtoc(compilerArguments.toTypedArray())

            if (exitCode != 0) {
                throw NativeProtobufModelGenerationFailed(
                    payloadName = parsedSchema.payloadName,
                    schemaFormat = parsedSchema.schema.schemaFormat,
                    modelType = models.modelType.configurationValue,
                    reason = "protoc exited with status code $exitCode.",
                )
            }

            return buildList {
                addAll(generatedSourceArtifacts(destinationDirectory, ".java", GeneratedArtifactKind.JAVA_SOURCE))
                kotlinDestinationDirectory?.let { kotlinDirectory ->
                    addAll(generatedSourceArtifacts(kotlinDirectory, ".kt", GeneratedArtifactKind.SOURCE))
                }
            }
        } catch (ex: NativeProtobufModelGenerationFailed) {
            throw ex
        } catch (ex: IOException) {
            throw protobufModelGenerationFailed(parsedSchema, models, ex)
        } catch (ex: RuntimeException) {
            throw protobufModelGenerationFailed(parsedSchema, models, ex)
        } finally {
            workspaceDirectory.toFile().deleteRecursively()
        }
    }

    private fun validateModelGenerationSupport(
        parsedSchema: ParsedNativeProtobufSchema,
        models: ProtobufModelGeneration,
    ) {
        val protobufSchema = parsedSchema.protobufSchema
        val payloadName = parsedSchema.payloadName
        val schemaFormat = parsedSchema.schema.schemaFormat
        val schemaPackageName = protobufSchema.javaPackageName ?: protobufSchema.protoPackageName

        if (schemaPackageName.isNullOrBlank()) {
            throw InvalidNativeProtobufSchema(
                payloadName = payloadName,
                schemaFormat = schemaFormat,
                reason = "Protobuf model generation requires either `option java_package = \"...\";` or a `package ...;` declaration.",
            )
        }

        if (schemaPackageName != models.packageName) {
            throw InvalidNativeProtobufSchema(
                payloadName = payloadName,
                schemaFormat = schemaFormat,
                reason =
                    "Configured model package '${models.packageName}' must match the Protobuf Java package " +
                        "'$schemaPackageName'.",
            )
        }

        if (protobufSchema.javaMultipleFiles != true) {
            throw InvalidNativeProtobufSchema(
                payloadName = payloadName,
                schemaFormat = schemaFormat,
                reason = "Protobuf model generation requires `option java_multiple_files = true;` so the payload message is generated as a top-level Java class.",
            )
        }

        if (payloadName !in protobufSchema.messageNames) {
            throw InvalidNativeProtobufSchema(
                payloadName = payloadName,
                schemaFormat = schemaFormat,
                reason = "Protobuf model generation requires a top-level message named '$payloadName'.",
            )
        }
    }

    private fun generatedSourceArtifacts(
        directory: Path,
        extension: String,
        kind: GeneratedArtifactKind,
    ): List<GeneratedArtifact> =
        Files.walk(directory).use { paths ->
            paths
                .filter { path -> Files.isRegularFile(path) && path.fileName.toString().endsWith(extension) }
                .sorted()
                .toList()
                .map { sourceFile ->
                    GeneratedArtifact(
                        relativePath = directory.relativeUnixPathTo(sourceFile),
                        content = Files.readString(sourceFile).trimEnd() + System.lineSeparator(),
                        kind = kind,
                    )
                }
        }

    private fun Path.relativeUnixPathTo(file: Path): String =
        relativize(file).toString().replace(File.separatorChar, '/')

    private fun protobufModelGenerationFailed(
        parsedSchema: ParsedNativeProtobufSchema,
        models: ProtobufModelGeneration,
        exception: Exception,
    ): NativeProtobufModelGenerationFailed =
        NativeProtobufModelGenerationFailed(
            payloadName = parsedSchema.payloadName,
            schemaFormat = parsedSchema.schema.schemaFormat,
            modelType = models.modelType.configurationValue,
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
