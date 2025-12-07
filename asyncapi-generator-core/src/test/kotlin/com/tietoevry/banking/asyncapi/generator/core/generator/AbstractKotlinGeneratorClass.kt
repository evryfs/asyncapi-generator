package com.tietoevry.banking.asyncapi.generator.core.generator

import com.tietoevry.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.generator.model.GeneratorName
import com.tietoevry.banking.asyncapi.generator.core.generator.model.GeneratorOptions
import com.tietoevry.banking.asyncapi.generator.core.parser.AsyncApiParser
import com.tietoevry.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import com.tietoevry.banking.asyncapi.generator.core.validator.AsyncApiValidator
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
        outputDir: File = File("target/generated-sources/asyncapi"),
        modelPackage: String,
        clientPackage: String? = null,
        schemaPackage: String? = null,
        generateModels: Boolean = true,
        generateSpringKafkaClient: Boolean = false,
        generateQuarkusKafkaClient: Boolean = false,
    ): String {
        val root = AsyncApiRegistry.readYaml(yaml, asyncApiContext)
        val asyncApi = parser.parse(root)
        validator.validate(asyncApi).apply {
            throwWarnings()
            throwErrors()
        }
        val bundled = bundler.bundle(asyncApi)
        val generatorOptions = GeneratorOptions(
            generatorName = GeneratorName.KOTLIN,
            modelPackage = modelPackage,
            clientPackage = clientPackage ?: modelPackage,
            schemaPackage = schemaPackage ?: modelPackage,
            outputDir = outputDir,
            generateModels = generateModels,
            generateSpringKafkaClient = generateSpringKafkaClient,
            generateQuarkusKafkaClient = generateQuarkusKafkaClient,
        )
        generator.generate(
            asyncApiDocument = bundled,
            generatorOptions = generatorOptions,
        )
        if (generated != null) {
            val modelPath = modelPackage.replace('.', '/')
            val output = outputDir
                .resolve(modelPath)
                .resolve(generated)
            return output.readText()
        }
        return ""
    }

    fun extractImports(source: String): String {
        val lines = source.lineSequence().toList()
        var endIndex = -1
        for ((idx, line) in lines.withIndex()) {
            if (line.startsWith("import ")) {
                endIndex = idx
            } else if (endIndex != -1 && line.isBlank()) {
                endIndex = idx
                break
            }
        }
        val blockLines = lines.subList(0, endIndex + 1)
        return blockLines.joinToString("\n").trimEnd()
    }

    protected fun extractElement(source: String): String {
        val dataIdx = source.indexOf("data class")
        val sealedIdx = source.indexOf("sealed interface")
        val enumIdx = source.indexOf("enum class")
        val candidates = listOf(dataIdx, sealedIdx, enumIdx).filter { it >= 0 }
        if (candidates.isEmpty()) {
            // fallback - just return the whole file trimmed
            return source.trim()
        }
        val startIdx = candidates.minOrNull()!!
        return source.substring(startIdx).trimEnd()
    }

}
