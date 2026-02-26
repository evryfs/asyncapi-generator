package dev.banking.asyncapi.generator.core.generator

import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.generator.model.GeneratorOptions
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import java.io.File

abstract class AbstractJavaGeneratorClass {

    protected val asyncApiContext = AsyncApiContext()
    protected val parser = AsyncApiParser(asyncApiContext)
    protected val bundler = AsyncApiBundler()
    protected val validator = AsyncApiValidator(asyncApiContext)
    protected val generator = AsyncApiGenerator()

    protected fun generateElement(
        yaml: File,
        codegenOutputDirectory: File = File("target/generated-sources/asyncapi"),
        resourceOutputDirectory: File = File("target/generated-resources/asyncapi"),
        generated: String? = null,
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
            logWarnings()
            throwErrors()
        }
        val bundled = bundler.bundle(asyncApi)
        val generatorOptions = GeneratorOptions(
            generatorName = GeneratorName.JAVA,
            modelPackage = modelPackage,
            clientPackage = clientPackage ?: modelPackage,
            schemaPackage = schemaPackage ?: modelPackage,
            codegenOutputDirectory = codegenOutputDirectory,
            resourceOutputDirectory = resourceOutputDirectory,
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
            val output = codegenOutputDirectory
                .resolve(modelPath)
                .resolve(generated)
            return output.readText()
        }
        return ""
    }

    protected fun extractImports(source: String): String {
        return source.lineSequence()
            .filter { it.startsWith("import ") }
            .sorted()
            .joinToString("\n")
            .trimEnd()
    }

    protected fun extractClassBody(source: String): String {
        val classStart = source.indexOf("public class")
            .let { if (it == -1) source.indexOf("public enum") else it }
            .let { if (it == -1) source.indexOf("public interface") else it }

        if (classStart == -1) {
            val packageEnd = source.indexOf("package ")
            if (packageEnd != -1) {
                return source
                    .substring(source.indexOf(';', packageEnd) + 1)
                    .trim()
                    .trimIndent()
            }
            return source
                .trim()
                .trimIndent()
        }
        return source
            .substring(classStart)
            .trim()
            .trimIndent()
    }
}
