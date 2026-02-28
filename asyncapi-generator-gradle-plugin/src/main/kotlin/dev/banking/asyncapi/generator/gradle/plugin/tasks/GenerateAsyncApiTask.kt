package dev.banking.asyncapi.generator.gradle.plugin.tasks

import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.generator.AsyncApiGenerator
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName.JAVA
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName.KOTLIN
import dev.banking.asyncapi.generator.core.generator.model.GeneratorOptions
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.util.Locale
import kotlin.text.get

abstract class GenerateAsyncApiTask : DefaultTask() {
    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty

    @get:OutputDirectory
    abstract val codegenOutputDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val resourceOutputDirectory: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val modelPackage: Property<String>

    @get:Input
    @get:Optional
    abstract val clientPackage: Property<String>

    @get:Input
    @get:Optional
    abstract val schemaPackage: Property<String>

    @get:Input
    abstract val generatorName: Property<String>

    @get:Input
    @get:Optional
    abstract val configOptions: MapProperty<String, String>

    @TaskAction
    fun generate() {
        logger.lifecycle("asyncapi-generator-gradle-plugin started")

        val context = AsyncApiContext()
        val parser = AsyncApiParser(context)
        val validator = AsyncApiValidator(context)
        val bundler = AsyncApiBundler()
        val generator = AsyncApiGenerator()

        val root = AsyncApiRegistry.readYaml(inputFile.get().asFile, context)
        val asyncApiDocument = parser.parse(root)
        val validationErrors = validator.validate(asyncApiDocument)

        validationErrors.logWarnings()
        validationErrors.throwErrors()

        val bundled = bundler.bundle(asyncApiDocument)

        if (outputFile.isPresent) {
            val file = outputFile.get().asFile
            logger.lifecycle("Writing bundled AsyncAPI specification to: ${file.absolutePath}")
            AsyncApiRegistry.writeYaml(file, bundled)
        }

        val genNameString = generatorName.get()
        val targetLanguage =
            try {
                GeneratorName.valueOf(genNameString.uppercase(Locale.getDefault()))
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid generatorName '$genNameString'. Supported values: ${GeneratorName.entries}")
            }

        // Calculate Source Root
        val sourceRootName =
            when (targetLanguage) {
                KOTLIN -> "src/main/kotlin"
                JAVA -> "src/main/java"
            }
        val codegenSourceRoot = codegenOutputDirectory.get().asFile.resolve(sourceRootName)
        val configMap = configOptions.getOrElse(emptyMap())

        val clientType = configMap["client.type"]
        val schemaType = configMap["schema.type"]
        val modelAnnotation = configMap["model.annotation"]
        val prefixOverride = configMap["kafka.topics.property.prefix"]
        val suffixOverride = configMap["kafka.topics.property.suffix"]

        val hasModelPackage = modelPackage.isPresent
        val hasClientPackage = clientPackage.isPresent
        val hasSchemaPackage = schemaPackage.isPresent

        if (clientType != null && !hasClientPackage) {
            throw IllegalArgumentException("client.type requires clientPackage")
        }

        if (schemaType != null && !hasSchemaPackage) {
            throw IllegalArgumentException("schema.type requires schemaPackage")
        }

        if (modelAnnotation != null && !hasModelPackage) {
            throw IllegalArgumentException("model.annotation requires modelPackage")
        }

        if (prefixOverride != null && prefixOverride.isBlank()) {
            throw IllegalArgumentException("kafka.topics.property.prefix cannot be empty")
        }

        if (hasModelPackage || hasClientPackage || hasSchemaPackage) {
            val effectiveModelPackage = if (hasModelPackage) modelPackage.get() else "unused"
            val effectiveClientPackage = if (hasClientPackage) clientPackage.get() else "unused"
            val effectiveSchemaPackage = if (hasSchemaPackage) schemaPackage.get() else "unused"

            val options =
                GeneratorOptions(
                    generatorName = targetLanguage,
                    modelPackage = effectiveModelPackage,
                    clientPackage = effectiveClientPackage,
                    schemaPackage = effectiveSchemaPackage,
                    codegenOutputDirectory = codegenSourceRoot,
                    resourceOutputDirectory = resourceOutputDirectory.get().asFile,
                    kafkaTopicsPropertyPrefix = prefixOverride ?: "kafka.topics",
                    kafkaTopicsPropertySuffix = suffixOverride ?: "topic",
                    generateModels = hasModelPackage,
                    generateSpringKafkaClient = hasClientPackage && clientType == "spring-kafka",
                    generateQuarkusKafkaClient = hasClientPackage && clientType == "quarkus-kafka",
                    generateAvroSchema = hasSchemaPackage && schemaType == "avro",
                    configOptions = configMap,
                )
            generator.generate(bundled, options)
        }
        logger.lifecycle("asyncapi-generator-gradle-plugin completed")
    }
}
