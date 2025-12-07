package dev.banking.asyncapi.generator.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.generator.AsyncApiGenerator
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName.JAVA
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName.KOTLIN
import dev.banking.asyncapi.generator.core.generator.model.GeneratorOptions
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import java.io.File

fun main(args: Array<String>) =
    AsyncApiGeneratorCli().main(args)

class AsyncApiGeneratorCli : CliktCommand(name = "asyncapi-generator") {

    private val input by option("--input", "-i", help = "Path to AsyncAPI YAML file")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .required()

    private val output by option("--output", "-o", help = "Output directory")
        .file(canBeFile = false)
        .default(File("./generated"))

    private val generator by option("--generator", "-g", help = "Target language (KOTLIN, JAVA)")
        .choice(
            "kotlin" to KOTLIN,
            "java" to JAVA
        )
        .default(KOTLIN)

    private val modelPackage by option("--model-package", help = "Package for generated models")
        .required()

    private val clientPackage by option(
        "--client-package",
        help = "Package for generated clients (defaults to model-package)"
    )

    private val schemaPackage by option(
        "--schema-package",
        help = "Namespace for Avro schemas (defaults to model-package)"
    )

    private val models by option("--models", help = "Generate data models").flag(default = true)
    private val avro by option("--avro", help = "Generate Avro schemas").flag()
    private val springKafka by option("--spring-kafka", help = "Generate Spring Kafka clients").flag()

    override fun run() {
        echo("Generating AsyncAPI code from $input...")

        val context = AsyncApiContext()
        val root = AsyncApiRegistry.readYaml(input, context)
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

        val effClientPackage = clientPackage ?: modelPackage
        val effSchemaPackage = schemaPackage ?: modelPackage

        val sourceRootName = if (generator == KOTLIN) {
            "src/main/kotlin"
        } else {
            "src/main/java"
        }
        val sourceRoot = output.resolve(sourceRootName)

        val options = GeneratorOptions(
            generatorName = generator,
            modelPackage = modelPackage,
            clientPackage = effClientPackage,
            schemaPackage = effSchemaPackage,
            outputDir = sourceRoot,
            generateModels = models,
            generateSpringKafkaClient = springKafka,
            generateQuarkusKafkaClient = false, // Not exposed in CLI yet
            generateAvroSchema = avro
        )

        val coreGenerator = AsyncApiGenerator()
        coreGenerator.generate(bundledDoc, options)

        echo("Generation complete.")
    }
}
