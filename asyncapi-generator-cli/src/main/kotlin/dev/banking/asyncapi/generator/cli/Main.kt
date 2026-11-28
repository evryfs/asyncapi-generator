package dev.banking.asyncapi.generator.cli

import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.versionOption
import dev.banking.asyncapi.generator.core.AsyncApiGenerationException
import dev.banking.asyncapi.generator.core.AsyncApiGenerator
import dev.banking.asyncapi.generator.core.ConfiguredGeneration
import dev.banking.asyncapi.generator.core.GeneratorSettings
import dev.banking.asyncapi.generator.core.ProtocProvider

fun main(args: Array<String>) = AsyncApiGeneratorCli().main(args)

/**
 * Command-line frontend for one AsyncAPI generation request.
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
        completionOption(
            help = "Generate a completion script for Bash, Zsh, or Fish.",
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

          Run a repeatable generation request from an argument file:
            asyncapi-generator @generation.args
        """.trimIndent()

    override fun run() {
        val generator = AsyncApiGenerator(protocProvider())
        val generation = configure(generator)
        if (generation.warnings.isNotEmpty()) {
            echo(message = generation.warnings, err = true)
        }
        echo("Generating AsyncAPI output from ${generationOptions.inputSpec}...")

        try {
            generator.generate(generationOptions.inputSpec, generation) { warnings ->
                echo(message = warnings, err = true)
            }
        } catch (exception: AsyncApiGenerationException) {
            throw CliktError(message = exception.message, cause = exception)
        }

        echo("Generation complete.")
    }

    private fun protocProvider(): ProtocProvider =
        generationOptions.protocExecutable?.let { ProtocProvider.executable(it.toPath()) }
            ?: ProtocProvider.fromMavenRepository()

    // A configuration mistake is a usage error, which Clikt shows with the usage hint; UsageError has no
    // cause, and the message is the whole diagnostic.
    @Suppress("SwallowedException")
    private fun configure(generator: AsyncApiGenerator): ConfiguredGeneration =
        try {
            generator.configure(
                GeneratorSettings(
                    generatorName = generationOptions.generatorName,
                    outputDirectory = outputOptions.outputDirectory,
                    outputFile = outputOptions.outputFile,
                    modelPackage = outputOptions.modelPackage,
                    clientPackage = outputOptions.clientPackage,
                    schemaPackage = outputOptions.schemaPackage,
                    modelConfig = modelOptions.toSettings(),
                    clientConfig = clientOptions.toSettings(),
                    suppressedWarnings = generationOptions.suppressedWarnings,
                ),
            )
        } catch (exception: AsyncApiGenerationException.InvalidSettings) {
            throw UsageError(exception.message)
        }
}
