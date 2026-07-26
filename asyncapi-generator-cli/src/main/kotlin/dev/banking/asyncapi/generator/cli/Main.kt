package dev.banking.asyncapi.generator.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.generator.AsyncApiGenerator
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator

fun main(args: Array<String>) = AsyncApiGeneratorCli().main(args)

class AsyncApiGeneratorCli : CliktCommand(name = "asyncapi-generator") {
    private val generationOptions by CliGenerationOptions()
    private val outputOptions by CliOutputOptions()
    private val modelOptions by CliModelOptions()
    private val clientOptions by CliClientOptions()

    override fun run() {
        val generatorConfiguration = generatorConfiguration()
        echo("Generating AsyncAPI output from ${generationOptions.inputSpec}...")

        val context = AsyncApiContext()
        val root = AsyncApiRegistry.read(generationOptions.inputSpec, context)
        val parser = AsyncApiParser(context)
        val document = parser.parse(root)

        val validator = AsyncApiValidator(context)
        val results = validator.validate(document)

        if (results.hasWarnings()) {
            results.warnings.forEach { echo("WARN: ${it.message}") }
        }

        if (results.hasErrors()) {
            results.errors.forEach { echo("ERROR: ${it.message}") }
            throw RuntimeException("Validation failed with ${results.errors.size} errors.")
        }

        val bundler = AsyncApiBundler()
        val bundledDoc = bundler.bundle(document)
        AsyncApiGenerator().generate(bundledDoc, generatorConfiguration)
        echo("Generation complete.")
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
