package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.generator.AsyncApiGenerator
import dev.banking.asyncapi.generator.core.generator.configuration.ClientGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.ClientValidationAnnotations
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorOutputConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType
import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.configuration.TopicParameterProperties
import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText

/**
 * Generates complete Spring Kafka client contracts for approval tests.
 *
 * Contracts are read, parsed, validated, bundled, analyzed, and generated through
 * the production entry path. The fixture keeps configuration and output discovery
 * out of approval test classes.
 */
internal class SpringKafkaClientOutputFixtures(
    private val generator: AsyncApiGenerator = AsyncApiGenerator(),
    private val bundlerFixtures: BundlerFixtures = BundlerFixtures(),
) {
    fun generate(
        contractPath: String,
        language: SourceLanguage,
        outputDirectory: Path,
        validationAnnotations: ClientValidationAnnotations = defaultValidationAnnotations,
        topicParameterProperties: TopicParameterProperties = defaultTopicParameterProperties,
        javaModelType: JavaModelType = JavaModelType.CLASS,
    ): GeneratedSpringKafkaContracts {
        generator.generate(
            asyncApiDocument = bundlerFixtures.bundledDocument(contractPath),
            generatorConfiguration =
                GeneratorConfiguration(
                    language = language,
                    output =
                        GeneratorOutputConfiguration(
                            sourceOutputDirectory = outputDirectory.toFile(),
                            resourceOutputDirectory = outputDirectory.resolve("resources").toFile(),
                        ),
                    models =
                        ModelGeneration.Enabled(
                            packageName = MODEL_PACKAGE,
                            javaModelType = javaModelType,
                        ),
                    clients =
                        listOf(
                            ClientGeneration.Kafka(
                                packageName = CLIENT_PACKAGE,
                                modelPackageName = MODEL_PACKAGE,
                                headers = ClientGeneration.Headers(enabled = true),
                                springKafka =
                                    ClientGeneration.SpringKafka(
                                        topicParameterProperties = topicParameterProperties,
                                        validationAnnotations = validationAnnotations,
                                    ),
                            ),
                        ),
                ),
        )

        val extension = if (language == SourceLanguage.KOTLIN) "kt" else "java"
        val packageDirectory = outputDirectory.resolve(CLIENT_PACKAGE.replace('.', '/'))
        return GeneratedSpringKafkaContracts(
            producers = readSources(packageDirectory.resolve("producer"), extension),
            consumers = readSources(packageDirectory.resolve("consumer"), extension),
            models = readNamedSources(outputDirectory.resolve(MODEL_PACKAGE.replace('.', '/')), extension),
        )
    }

    private val defaultValidationAnnotations =
        ClientValidationAnnotations(
            clientContract =
                QualifiedTypeName.fromConfigurationValue(
                    value = "org.springframework.validation.annotation.Validated",
                    path = "clientConfig.validationAnnotations.clientContract",
                ),
            payloadParameter =
                QualifiedTypeName.fromConfigurationValue(
                    value = "jakarta.validation.Valid",
                    path = "clientConfig.validationAnnotations.payloadParameter",
                ),
        )

    private val defaultTopicParameterProperties =
        TopicParameterProperties.fromConfigurationValues(
            values = mapOf("environment" to "kafka.environment"),
            path = "clientConfig.topicParameterProperties",
        )

    private fun readSources(
        directory: Path,
        extension: String,
    ): List<String> =
        directory
            .listDirectoryEntries()
            .filter { path -> path.isRegularFile() && path.extension == extension }
            .sortedBy(Path::toString)
            .map(Path::readText)

    private fun readNamedSources(
        directory: Path,
        extension: String,
    ): Map<String, String> =
        directory
            .listDirectoryEntries()
            .filter { path -> path.isRegularFile() && path.extension == extension }
            .sortedBy(Path::toString)
            .associate { path -> path.nameWithoutExtension to path.readText() }

    internal companion object {
        const val CLIENT_PACKAGE = "com.example.account.client"
        const val MODEL_PACKAGE = "com.example.account.model"
        const val SINGLE_MESSAGE_CONTRACT = "generator/spring-kafka/single-message.yaml"
        const val THREE_MESSAGE_CONTRACT = "generator/spring-kafka/three-messages.yaml"
    }
}

/** Generated producer and consumer source contracts for one approval scenario. */
internal data class GeneratedSpringKafkaContracts(
    val producers: List<String>,
    val consumers: List<String>,
    val models: Map<String, String>,
) {
    fun singleProducer(): String =
        producers.singleOrNull()
            ?: error("Expected one generated producer contract, found ${producers.size}")

    fun singleConsumer(): String =
        consumers.singleOrNull()
            ?: error("Expected one generated consumer contract, found ${consumers.size}")

    fun model(name: String): String =
        models[name]
            ?: error("Expected generated model '$name', found ${models.keys.sorted()}")
}
