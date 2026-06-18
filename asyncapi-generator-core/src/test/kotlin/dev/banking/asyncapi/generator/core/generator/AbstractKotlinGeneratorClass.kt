package dev.banking.asyncapi.generator.core.generator

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.BundlerFixtures
import dev.banking.asyncapi.generator.core.generator.configuration.ClientGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorOutputConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import java.io.File

abstract class AbstractKotlinGeneratorClass {
    val asyncApiContext = AsyncApiContext()
    private val bundlerFixtures = BundlerFixtures(asyncApiContext)
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
        generateKafkaHeaders: Boolean = true,
        generateQuarkusKafkaClient: Boolean = false,
        modelAnnotation: String? = null,
    ): String {
        val bundled = bundlerFixtures.bundledDocument(yaml)
        val effectiveClientPackage = clientPackage ?: modelPackage
        val generatorConfiguration =
            GeneratorConfiguration(
                language = GeneratorName.KOTLIN,
                output =
                    GeneratorOutputConfiguration(
                        sourceOutputDirectory = codegenOutputDirectory,
                        resourceOutputDirectory = resourceOutputDirectory,
                    ),
                models =
                    if (generateModels) {
                        ModelGeneration.Enabled(
                            packageName = modelPackage,
                            annotation = modelAnnotation,
                        )
                    } else {
                        ModelGeneration.Disabled
                    },
                clients =
                    buildList {
                        if (generateSpringKafkaClient) {
                            add(
                                ClientGeneration.Kafka(
                                    packageName = effectiveClientPackage,
                                    modelPackageName = modelPackage,
                                    headers = ClientGeneration.Headers(enabled = generateKafkaHeaders),
                                    springKafka = ClientGeneration.SpringKafka(),
                                ),
                            )
                        }
                        if (generateQuarkusKafkaClient) {
                            add(
                                ClientGeneration.QuarkusKafka(
                                    packageName = effectiveClientPackage,
                                    modelPackageName = modelPackage,
                                ),
                            )
                        }
                    },
            )
        generator.generate(
            asyncApiDocument = bundled,
            generatorConfiguration = generatorConfiguration,
        )
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
