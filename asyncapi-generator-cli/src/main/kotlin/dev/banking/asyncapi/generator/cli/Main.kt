package dev.banking.asyncapi.generator.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
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
    private val codegenOutputDirectory by option("--codegen-output", help = "Codegen output directory")
        .file(canBeFile = false)
        .default(File("./generated-sources/asyncapi"))
    private val resourceOutputDirectory by option("--resource-output", help = "Resource output directory")
        .file(canBeFile = false)
        .default(File("./generated-resources/asyncapi"))
    private val outputFile by option("--output-file", help = "Write bundled AsyncAPI YAML to file")
        .file(canBeDir = false)

    private val generator by option("--generator", "-g", help = "Target language (KOTLIN, JAVA)")
        .choice(
            "kotlin" to KOTLIN,
            "java" to JAVA
        )
        .default(KOTLIN)

    private val modelPackage by option("--model-package", help = "Package for generated models")

    private val clientPackage by option(
        "--client-package",
        help = "Package for generated clients (defaults to model-package)"
    )

    private val schemaPackage by option(
        "--schema-package",
        help = "Namespace for Avro schemas (defaults to model-package)"
    )
    private val configOptionsRaw by option(
        "--config-option",
        help = "Additional generator options (key=value). Repeatable."
    ).multiple()

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
        outputFile?.let { file ->
            AsyncApiRegistry.writeYaml(file, bundledDoc)
        }
        val configOptions = parseConfigOptions(configOptionsRaw)

        val clientType = configOptions["client.type"]
        val schemaType = configOptions["schema.type"]
        val modelAnnotation = configOptions["model.annotation"]

        val hasModelPackage = modelPackage != null
        val hasClientPackage = clientPackage != null
        val hasSchemaPackage = schemaPackage != null

        if (clientType != null && !hasClientPackage) {
            throw IllegalArgumentException("client.type requires --client-package")
        }

        if (schemaType != null && !hasSchemaPackage) {
            throw IllegalArgumentException("schema.type requires --schema-package")
        }

        if (modelAnnotation != null && !hasModelPackage) {
            throw IllegalArgumentException("model.annotation requires --model-package")
        }

        if (hasModelPackage || hasClientPackage || hasSchemaPackage) {
            val effectiveModelPackage = modelPackage ?: "unused"
            val effectiveClientPackage = clientPackage ?: "unused"
            val effectiveSchemaPackage = schemaPackage ?: "unused"
            val sourceRootName = if (generator == KOTLIN) {
                "src/main/kotlin"
            } else {
                "src/main/java"
            }
            val sourceRoot = codegenOutputDirectory.resolve(sourceRootName)
            val options = GeneratorOptions(
                generatorName = generator,
                modelPackage = effectiveModelPackage,
                clientPackage = effectiveClientPackage,
                schemaPackage = effectiveSchemaPackage,
                codegenOutputDirectory = sourceRoot,
                resourceOutputDirectory = resourceOutputDirectory,
                generateModels = hasModelPackage,
                generateSpringKafkaClient = hasClientPackage && clientType == "spring-kafka",
                generateQuarkusKafkaClient = hasClientPackage && clientType == "quarkus-kafka",
                generateAvroSchema = hasSchemaPackage && schemaType == "avro",
                configOptions = configOptions
            )
            val coreGenerator = AsyncApiGenerator()
            coreGenerator.generate(bundledDoc, options)
        }
        echo("Generation complete.")
    }

    private fun parseConfigOptions(raw: List<String>): Map<String, String> {
        if (raw.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, String>()
        raw.forEach { entry ->
            val idx = entry.indexOf("=")
            require(idx > 0 && idx < entry.length - 1) {
                "Invalid --config-option '$entry'. Expected key=value."
            }
            val key = entry.take(idx).trim()
            val value = entry.substring(idx + 1).trim()
            require(key.isNotEmpty() && value.isNotEmpty()) {
                "Invalid --config-option '$entry'. Expected key=value."
            }
            result[key] = value
        }
        return result
    }
}
