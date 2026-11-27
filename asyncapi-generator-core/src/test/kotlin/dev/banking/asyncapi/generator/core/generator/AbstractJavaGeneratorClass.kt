package dev.banking.asyncapi.generator.core.generator

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.BundlerFixtures
import dev.banking.asyncapi.generator.core.generator.configuration.ClientGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorOutputConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorProfile
import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.ModelType
import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage
import java.io.File

internal abstract class AbstractJavaGeneratorClass {
    private val asyncApiContext = AsyncApiContext()
    private val bundlerFixtures = BundlerFixtures(asyncApiContext)
    protected val generator = GenerationPipeline()

    protected fun generateElement(
        yaml: File,
        codegenOutputDirectory: File = File("target/generated-sources/asyncapi"),
        generated: String? = null,
        modelPackage: String,
        clientPackage: String? = null,
        schemaPackage: String? = null,
        generateModels: Boolean = true,
        generateSpringKafkaClient: Boolean = false,
        modelAnnotation: String? = null,
        modelType: ModelType = ModelType.JAVA_CLASS,
    ): String {
        val bundled = bundlerFixtures.bundledDocument(yaml)
        val effectiveClientPackage = clientPackage ?: modelPackage
        val generatorConfiguration =
            GeneratorConfiguration(
                profile = GeneratorProfile.Source(SourceLanguage.JAVA),
                output =
                    GeneratorOutputConfiguration(
                        outputDirectory = codegenOutputDirectory,
                    ),
                models =
                    if (generateModels) {
                        ModelGeneration.Enabled(
                            packageName = modelPackage,
                            annotation =
                                modelAnnotation?.let { value ->
                                    QualifiedTypeName.fromConfigurationValue(
                                        value = value,
                                        path = "modelConfig.modelAnnotation",
                                    )
                                },
                            modelType = modelType,
                        )
                    } else {
                        ModelGeneration.Disabled
                    },
                client =
                    if (generateSpringKafkaClient) {
                        ClientGeneration(
                            packageName = effectiveClientPackage,
                            modelPackageName = modelPackage,
                        )
                    } else {
                        null
                    },
            )
        generator.generate(
            bundled = bundled,
            generatorConfiguration = generatorConfiguration,
        )

        return loadGeneratedClassContent(codegenOutputDirectory, generated, modelPackage)
    }

    fun loadGeneratedClassContent(
        codegenOutputDirectory: File = File("target/generated-sources/asyncapi"),
        generated: String? = null,
        modelPackage: String,
    ): String {
        if (generated != null) {
            val modelPath = modelPackage.replace('.', '/')
            val output =
                codegenOutputDirectory
                    .resolve(modelPath)
                    .resolve(generated)
            return output.readText()
        }
        return ""
    }

    protected fun extractImports(source: String): String =
        source
            .lineSequence()
            .filter { it.startsWith("import ") }
            .sorted()
            .joinToString("\n")
            .trimEnd()

    protected fun extractClassBody(source: String): String {
        val classStart =
            listOf("public class", "public enum", "public interface")
                .map(source::indexOf)
                .firstOrNull { it != -1 }
        val packageStart = source.indexOf("package ")
        val body =
            when {
                classStart != null -> source.substring(classStart)
                packageStart != -1 -> source.substring(source.indexOf(';', packageStart) + 1)
                else -> source
            }
        return body.trim().trimIndent()
    }
}
