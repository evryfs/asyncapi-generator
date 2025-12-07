package dev.banking.asyncapi.generator.core.generator

import dev.banking.asyncapi.generator.core.bundler.AsyncApiBundler
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import dev.banking.asyncapi.generator.core.generator.model.GeneratorOptions
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import java.io.File

abstract class AbstractAvroGeneratorClass {

    protected val asyncApiContext = AsyncApiContext()
    protected val parser = AsyncApiParser(asyncApiContext)
    protected val bundler = AsyncApiBundler()
    protected val validator = AsyncApiValidator(asyncApiContext)
    protected val generator = AsyncApiGenerator()

    protected fun generateAvro(
        yaml: File,
        outputDir: File = File("target/generated-sources/asyncapi"),
        packageName: String,
        schema: String? = null,
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
            modelPackage = packageName,
            clientPackage = packageName,
            schemaPackage = packageName,
            outputDir = outputDir,
            generateModels = false,
            generateSpringKafkaClient = false,
            generateAvroSchema = true,
        )

        generator.generate(
            asyncApiDocument = bundled,
            generatorOptions = generatorOptions,
        )

        if (schema != null) {
            val packagePath = packageName.replace('.', '/')
            val output = outputDir
                .resolve(packagePath)
                .resolve(schema)
            if (output.exists()) return output.readText()
        }
        return ""
    }
}
