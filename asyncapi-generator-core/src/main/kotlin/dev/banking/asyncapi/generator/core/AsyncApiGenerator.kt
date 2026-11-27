package dev.banking.asyncapi.generator.core

import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import dev.banking.asyncapi.generator.core.generator.GenerationPipeline
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationFactory
import dev.banking.asyncapi.generator.core.loader.AsyncApiDocumentLoader
import java.io.File

/**
 * Generates code, schemas and bundled documents from an AsyncAPI contract. This is the entry point
 * of the CLI and the build plugins; everything else in core is internal.
 *
 * A generation has two steps. [configure] checks the settings without reading the contract or the
 * file system, so configuration mistakes are reported first and a build plugin can register its output
 * directories while it configures. [generate] then checks the output paths on disk, reads, validates and
 * bundles the contract, and writes the output.
 *
 * Every failure is an [AsyncApiGenerationException] whose message is written for the user.
 *
 * @param protocProvider supplies `protoc` for native Protobuf models
 */
class AsyncApiGenerator(
    protocProvider: ProtocProvider = ProtocProvider.fromMavenRepository(),
) {
    private val loader = AsyncApiDocumentLoader()
    private val pipeline = GenerationPipeline(protocProvider)

    /** Fails with [AsyncApiGenerationException.Failed] unless [inputSpec] is an existing, readable file. */
    fun requireInputFile(inputSpec: File) {
        failingAsGeneration { loader.requireInputFile(inputSpec) }
    }

    /** Checks [settings]; fails with [AsyncApiGenerationException.InvalidSettings] at the first invalid setting. */
    fun configure(settings: GeneratorSettings): ConfiguredGeneration =
        failingAsInvalidSettings { ConfiguredGeneration(GeneratorConfigurationFactory.create(settings)) }

    /**
     * Reads the contract at [inputSpec], validates and bundles it, and writes what [generation] configures.
     * Contract warnings, formatted for the user, go to [reportWarnings].
     *
     * Fails with [AsyncApiGenerationException.InvalidSettings] before reading the contract when an existing
     * output path has the wrong kind: a file as `outputDirectory`, or a directory as `outputFile`.
     */
    fun generate(
        inputSpec: File,
        generation: ConfiguredGeneration,
        reportWarnings: (String) -> Unit,
    ) {
        failingAsInvalidSettings { requireOutputPaths(generation) }
        failingAsGeneration {
            val loaded = loader.load(inputSpec)
            if (loaded.warnings.isNotEmpty()) {
                reportWarnings(loaded.formatWarnings().trimEnd())
            }
            pipeline.generate(AsyncApiBundler().bundle(loaded), generation.configuration)
        }
    }

    /** The files the contract at [inputSpec] consists of: the root document, external documents and native schemas. */
    fun inputFiles(inputSpec: File): List<File> = failingAsGeneration { loader.load(inputSpec).sourceFiles.toList() }

    private fun requireOutputPaths(generation: ConfiguredGeneration) {
        val output = generation.configuration.output
        require(!output.outputDirectory.exists() || output.outputDirectory.isDirectory) {
            "outputDirectory must be a directory: ${output.outputDirectory}"
        }
        require(output.document?.file?.isDirectory != true) {
            "outputFile must be a file: ${output.document?.file}"
        }
    }

    private inline fun <T> failingAsInvalidSettings(action: () -> T): T =
        try {
            action()
        } catch (exception: IllegalArgumentException) {
            throw AsyncApiGenerationException.InvalidSettings(
                exception.message ?: "Invalid generator configuration",
                exception,
            )
        }

    // Loading, validating, bundling and writing fail with exception types from every stage, checked and
    // unchecked; each one carries the message the user should see, so all of them become one failure type.
    @Suppress("TooGenericExceptionCaught")
    private inline fun <T> failingAsGeneration(action: () -> T): T =
        try {
            action()
        } catch (exception: Exception) {
            throw AsyncApiGenerationException.Failed(exception.message ?: "AsyncAPI generation failed.", exception)
        }
}
