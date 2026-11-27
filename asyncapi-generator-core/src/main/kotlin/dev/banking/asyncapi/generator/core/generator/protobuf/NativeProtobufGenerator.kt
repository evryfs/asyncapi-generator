package dev.banking.asyncapi.generator.core.generator.protobuf

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
 * @param protocProvider supplies the `protoc` executable that compiles the models
 * @param protocVersion the `protoc` version to use
 */
internal class NativeProtobufGenerator(
    private val protocProvider: ProtocProvider = MavenRepositoryProtocProvider(),
    private val protocVersion: String = ProtocProvider.DEFAULT_VERSION,
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
        return failingAsModelGeneration(parsedSchema, models) {
            val workspaceDirectory = Files.createTempDirectory("asyncapi-native-protobuf-")
            try {
                compileModels(parsedSchema, models, workspaceDirectory)
            } finally {
                workspaceDirectory.toFile().deleteRecursively()
            }
        }
    }

    /** The Java, and for Kotlin also Kotlin, sources protoc writes for [parsedSchema] in [workspaceDirectory]. */
    private fun compileModels(
        parsedSchema: ParsedNativeProtobufSchema,
        models: ProtobufModelGeneration,
        workspaceDirectory: Path,
    ): List<GeneratedArtifact> {
        val sourceDirectory = Files.createDirectory(workspaceDirectory.resolve("schemas"))
        val sourceSchemaFile = sourceDirectory.resolve("${parsedSchema.payloadName}.proto")
        val destinationDirectory = Files.createDirectory(workspaceDirectory.resolve("java"))
        val kotlinDestinationDirectory =
            workspaceDirectory.resolve("kotlin").takeIf { models.modelType == ProtobufModelType.KOTLIN }
        kotlinDestinationDirectory?.let(Files::createDirectory)
        Files.writeString(sourceSchemaFile, parsedSchema.protobufSchema.content.trimEnd() + System.lineSeparator())
        val compilerArguments =
            listOfNotNull(
                "--proto_path=${sourceDirectory.toAbsolutePath()}",
                "--java_out=${destinationDirectory.toAbsolutePath()}",
                kotlinDestinationDirectory?.let { "--kotlin_out=${it.toAbsolutePath()}" },
                sourceSchemaFile.fileName.toString(),
            )
        val (exitCode, output) = runProtoc(compilerArguments, workspaceDirectory)
        if (exitCode != 0) {
            throw NativeProtobufModelGenerationFailed(
                payloadName = parsedSchema.payloadName,
                schemaFormat = parsedSchema.schema.schemaFormat,
                modelType = models.modelType.name.lowercase(),
                reason = "protoc exited with status code $exitCode: ${output.trim()}",
            )
        }
        return generatedSourceArtifacts(destinationDirectory, ".java") +
            kotlinDestinationDirectory
                ?.let { generatedSourceArtifacts(it, ".kt") }
                .orEmpty()
    }

    // Running protoc reports failures with I/O exceptions and several unchecked exception types; each
    // one fails model generation of this payload, except a failure that already says so.
    @Suppress("TooGenericExceptionCaught")
    private fun <T> failingAsModelGeneration(
        parsedSchema: ParsedNativeProtobufSchema,
        models: ProtobufModelGeneration,
        generation: () -> T,
    ): T =
        try {
            generation()
        } catch (ex: NativeProtobufModelGenerationFailed) {
            throw ex
        } catch (ex: IOException) {
            throw protobufModelGenerationFailed(parsedSchema, models, ex)
        } catch (ex: RuntimeException) {
            throw protobufModelGenerationFailed(parsedSchema, models, ex)
        }

    /** Runs protoc in [workingDirectory]; returns its exit code and combined output. */
    private fun runProtoc(
        arguments: List<String>,
        workingDirectory: Path,
    ): Pair<Int, String> {
        val process =
            ProcessBuilder(listOf(protocProvider.protoc(protocVersion).toString()) + arguments)
                .directory(workingDirectory.resolve("schemas").toFile())
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().readText()
        return process.waitFor() to output
    }

    private fun validateModelGenerationSupport(
        parsedSchema: ParsedNativeProtobufSchema,
        models: ProtobufModelGeneration,
    ) {
        val protobufSchema = parsedSchema.protobufSchema
        val payloadName = parsedSchema.payloadName
        val schemaPackageName = protobufSchema.javaPackageName ?: protobufSchema.protoPackageName
        val reason =
            when {
                schemaPackageName.isNullOrBlank() ->
                    "Protobuf model generation requires either `option java_package = \"...\";` " +
                        "or a `package ...;` declaration."
                schemaPackageName != models.packageName ->
                    "Configured model package '${models.packageName}' must match the Protobuf Java package " +
                        "'$schemaPackageName'."
                protobufSchema.javaMultipleFiles != true ->
                    "Protobuf model generation requires `option java_multiple_files = true;` " +
                        "so the payload message is generated as a top-level Java class."
                payloadName !in protobufSchema.messageNames ->
                    "Protobuf model generation requires a top-level message named '$payloadName'."
                else -> return
            }
        throw InvalidNativeProtobufSchema(
            payloadName = payloadName,
            schemaFormat = parsedSchema.schema.schemaFormat,
            reason = reason,
        )
    }

    private fun generatedSourceArtifacts(
        directory: Path,
        extension: String,
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
                        kind = GeneratedArtifactKind.SOURCE,
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
            modelType = models.modelType.name.lowercase(),
            reason = exception.message ?: exception::class.simpleName.orEmpty(),
        )
}

/**
 * A native Protobuf payload and its parsed declarations.
 *
 * @property payloadName the payload name
 * @property schema the Multi Format Schema of the payload
 * @property protobufSchema the declarations of its `.proto` content
 */
private data class ParsedNativeProtobufSchema(
    val payloadName: String,
    val schema: MultiFormatSchema,
    val protobufSchema: NativeProtobufSchema,
)
