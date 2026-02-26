package dev.banking.asyncapi.generator.core.generator

import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.generator.model.GeneratorOptions
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import java.io.File

abstract class AbstractKotlinGeneratorClass {

    val asyncApiContext = AsyncApiContext()
    val parser = AsyncApiParser(asyncApiContext)
    val bundler = AsyncApiBundler()
    val validator = AsyncApiValidator(asyncApiContext)
    val generator = AsyncApiGenerator()

    fun generateElement(
        yaml: File,
        generated: String? = null,
        codegenOutputDirectory: File = File("target/generated-sources/asyncapi"),
        resourceOutputDirectory: File = File("target/generated-resources/asyncapi"),
        modelPackage: String,
        clientPackage: String? = null,
        schemaPackage: String? = null,
        generateModels: Boolean = true,
        generateSpringKafkaClient: Boolean = false,
        generateQuarkusKafkaClient: Boolean = false,
        configOptions: Map<String, String> = emptyMap(),
    ): String {
        val root = AsyncApiRegistry.readYaml(yaml, asyncApiContext)
        val asyncApi = parser.parse(root)
        validator.validate(asyncApi).apply {
            logWarnings()
            throwErrors()
        }
        val bundled = bundler.bundle(asyncApi)
        val generatorOptions = GeneratorOptions(
            generatorName = GeneratorName.KOTLIN,
            modelPackage = modelPackage,
            clientPackage = clientPackage ?: modelPackage,
            schemaPackage = schemaPackage ?: modelPackage,
            codegenOutputDirectory = codegenOutputDirectory,
            resourceOutputDirectory = resourceOutputDirectory,
            generateModels = generateModels,
            generateSpringKafkaClient = generateSpringKafkaClient,
            generateQuarkusKafkaClient = generateQuarkusKafkaClient,
            configOptions = configOptions,
        )
        generator.generate(
            asyncApiDocument = bundled,
            generatorOptions = generatorOptions,
        )
        if (generated != null) {
            val modelPath = modelPackage.replace('.', '/')
            val output = codegenOutputDirectory
                .resolve(modelPath)
                .resolve(generated)
            return output.readText()
        }
        return ""
    }

    protected fun extractElement(source: String): String {
        val dataIdx = source.indexOf("data class")
        val sealedIdx = source.indexOf("sealed interface")
        val enumIdx = source.indexOf("enum class")
        val candidates = listOf(dataIdx, sealedIdx, enumIdx).filter { it >= 0 }
        if (candidates.isEmpty()) {
            return source
                .trim()
                .trimIndent()
        }
        val startIdx = candidates.minOrNull()!!
        return source
            .substring(startIdx)
            .trim()
            .trimIndent()
    }
}
