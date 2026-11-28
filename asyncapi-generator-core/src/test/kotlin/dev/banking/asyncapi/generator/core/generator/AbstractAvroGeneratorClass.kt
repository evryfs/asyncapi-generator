package dev.banking.asyncapi.generator.core.generator

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.BundlerFixtures
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorOutputConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorProfile
import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.SchemaGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage
import java.io.File

abstract class AbstractAvroGeneratorClass {
    private val asyncApiContext = AsyncApiContext()
    private val bundlerFixtures = BundlerFixtures(asyncApiContext)
    protected val generator = GenerationPipeline()

    protected fun generateAvro(
        yaml: File,
        codegenOutputDirectory: File = File("target/generated-sources/asyncapi"),
        packageName: String,
        schema: String? = null,
    ): String {
        val bundled = bundlerFixtures.bundledDocument(yaml)

        val generatorConfiguration =
            GeneratorConfiguration(
                profile = GeneratorProfile.Source(SourceLanguage.KOTLIN),
                output =
                    GeneratorOutputConfiguration(
                        outputDirectory = codegenOutputDirectory,
                    ),
                models = ModelGeneration.Disabled,
                schemas = listOf(SchemaGeneration.AvroProjection(packageName)),
            )

        generator.generate(
            bundled = bundled,
            generatorConfiguration = generatorConfiguration,
        )

        if (schema != null) {
            val packagePath = packageName.replace('.', '/')
            val output =
                codegenOutputDirectory
                    .resolve(packagePath)
                    .resolve(schema)
            if (output.exists()) return output.readText()
        }
        return ""
    }
}
