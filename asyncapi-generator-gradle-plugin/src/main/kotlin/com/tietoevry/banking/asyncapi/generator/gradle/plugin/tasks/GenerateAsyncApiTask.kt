package com.tietoevry.banking.asyncapi.generator.gradle.plugin.tasks

import com.tietoevry.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.generator.AsyncApiGenerator
import com.tietoevry.banking.asyncapi.generator.core.generator.model.GeneratorName
import com.tietoevry.banking.asyncapi.generator.core.generator.model.GeneratorName.JAVA
import com.tietoevry.banking.asyncapi.generator.core.generator.model.GeneratorName.KOTLIN
import com.tietoevry.banking.asyncapi.generator.core.generator.model.GeneratorOptions
import com.tietoevry.banking.asyncapi.generator.core.parser.AsyncApiParser
import com.tietoevry.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import com.tietoevry.banking.asyncapi.generator.core.validator.AsyncApiValidator
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.util.Locale

abstract class GenerateAsyncApiTask : DefaultTask() {

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
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
    abstract val configuration: MapProperty<String, String>

    @get:Input
    @get:Optional
    abstract val experimental: MapProperty<String, String>

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

        validationErrors.throwWarnings()
        validationErrors.throwErrors()

        val bundled = bundler.bundle(asyncApiDocument)

        if (outputFile.isPresent) {
            val file = outputFile.get().asFile
            logger.lifecycle("Writing bundled AsyncAPI specification to: ${file.absolutePath}")
            AsyncApiRegistry.writeYaml(file, bundled)
        }

        val genNameString = generatorName.get()
        val targetLanguage = try {
            GeneratorName.valueOf(genNameString.uppercase(Locale.getDefault()))
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid generatorName '$genNameString'. Supported values: ${GeneratorName.entries}")
        }

        // Calculate Source Root
        val sourceRootName = when(targetLanguage) {
            KOTLIN -> "src/main/kotlin"
            JAVA -> "src/main/java"
        }
        val sourceRoot = outputDir.get().asFile.resolve(sourceRootName)

        val configMap = configuration.getOrElse(emptyMap())
        val effectiveClientPackage = if (clientPackage.isPresent) clientPackage.get() else modelPackage.get()
        val effectiveSchemaPackage = if (schemaPackage.isPresent) schemaPackage.get() else modelPackage.get()

        val options = GeneratorOptions(
            generatorName = targetLanguage,
            modelPackage = modelPackage.get(),
            clientPackage = effectiveClientPackage,
            schemaPackage = effectiveSchemaPackage,
            outputDir = sourceRoot,

            generateModels = configMap["generateModels"]?.toBoolean() ?: true,
            generateSpringKafkaClient = configMap["generateSpringKafkaClient"]?.toBoolean() ?: false,
            generateQuarkusKafkaClient = configMap["generateQuarkusKafkaClient"]?.toBoolean() ?: false,
            generateAvroSchema = configMap["generateAvroSchema"]?.toBoolean() ?: false,

            experimental = experimental.getOrElse(emptyMap())
        )

        generator.generate(bundled, options)

        logger.lifecycle("asyncapi-generator-gradle-plugin completed")
    }
}
