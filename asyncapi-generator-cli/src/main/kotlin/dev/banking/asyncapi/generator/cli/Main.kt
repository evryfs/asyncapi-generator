package dev.banking.asyncapi.generator.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.versionOption
import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.generator.AsyncApiGenerator
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationFindingFormatter
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults

fun main(args: Array<String>) = AsyncApiGeneratorCli().main(args)

/**
 * Command-line frontend for one AsyncAPI generation request.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorCliTest`
 * - `CliDiagnosticsTest`
 */
class AsyncApiGeneratorCli : CliktCommand(name = "asyncapi-generator") {
    override val printHelpOnEmptyArgs: Boolean = true

    init {
        versionOption(
            version =
                AsyncApiGeneratorCli::class.java
                    .`package`
                    .implementationVersion
                    ?: "development",
        )
    }

    private val generationOptions by CliGenerationOptions()
    private val outputOptions by CliOutputOptions()
    private val modelOptions by CliModelOptions()
    private val clientOptions by CliClientOptions()

    override fun help(context: Context): String =
        "Generate payload models, client contracts, schema artifacts, or bundled documents from AsyncAPI."

    override fun helpEpilog(context: Context): String =
        """
        Examples:

          Generate Kotlin payload models:
            asyncapi-generator -i asyncapi.yaml -g kotlin \
              --model-package com.example.events.model

          Generate Kotlin models and Spring Kafka contracts:
            asyncapi-generator -i asyncapi.yaml -g kotlin \
              --model-package com.example.events.model \
              --client-package com.example.events.client \
              --client-type spring-kafka \
              --client-contract interface

          Generate Avro schema artifacts:
            asyncapi-generator -i asyncapi.yaml -g avro-schema \
              --schema-package com.example.events.schema
        """.trimIndent()

    override fun run() {
        val generatorConfiguration = generatorConfiguration()
        echo("Generating AsyncAPI output from ${generationOptions.inputSpec}...")

        try {
            val context = AsyncApiContext()
            val root = AsyncApiRegistry.read(generationOptions.inputSpec, context)
            val parser = AsyncApiParser(context)
            val document = parser.parse(root)

            val validator = AsyncApiValidator(context)
            val results = validator.validate(document)
            reportWarnings(results)
            results.throwErrors()

            val bundler = AsyncApiBundler()
            val bundledDoc = bundler.bundle(document)
            AsyncApiGenerator().generate(bundledDoc, generatorConfiguration)
        } catch (exception: Exception) {
            throw CliktError(
                message = exception.message ?: "AsyncAPI generation failed.",
                cause = exception,
            )
        }

        echo("Generation complete.")
    }

    private fun reportWarnings(results: ValidationResults) {
        if (!results.hasWarnings()) {
            return
        }

        echo(
            message =
                ValidationFindingFormatter.format(
                    title = "Validation found ${results.warnings.size} warning(s):",
                    findings = results.warnings,
                    asyncApiContext = results.asyncApiContext,
                ).trimEnd(),
            err = true,
        )
    }

    private fun generatorConfiguration() =
        try {
            CliGeneratorConfigurationMapper.map(
                CliGeneratorConfigurationRequest(
                    generatorName = generationOptions.generatorName.configurationValue,
                    outputDirectory = outputOptions.outputDirectory,
                    outputFile = outputOptions.outputFile,
                    modelPackage = outputOptions.modelPackage,
                    clientPackage = outputOptions.clientPackage,
                    schemaPackage = outputOptions.schemaPackage,
                    modelConfig = modelOptions.toConfiguration(),
                    clientConfig = clientOptions.toConfiguration(),
                ),
            )
        } catch (exception: IllegalArgumentException) {
            throw UsageError(exception.message ?: "Invalid generator configuration")
        }
}
