package dev.banking.asyncapi.generator.cli

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.nullableFlag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType
import dev.banking.asyncapi.generator.core.generator.configuration.ClientContract
import dev.banking.asyncapi.generator.core.generator.configuration.ClientType
import dev.banking.asyncapi.generator.core.generator.configuration.ModelType
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import java.io.File

/**
 * Selects the AsyncAPI contract and generator profile for one CLI invocation.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorCliTest`
 */
internal class CliGenerationOptions : OptionGroup(name = "Generation") {
    val inputSpec by option(
        "--input-spec",
        "-i",
        help = "AsyncAPI YAML or JSON input file.",
    ).file(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true,
    ).required()

    val generatorName by option(
        "--generator-name",
        "-g",
        help = "Generator profile.",
    ).choice(
        *GeneratorName.entries
            .map { generatorName -> generatorName.configurationValue to generatorName }
            .toTypedArray(),
    ).required()
}

/**
 * Selects generated outputs and their packages.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorCliTest`
 */
internal class CliOutputOptions : OptionGroup(name = "Outputs") {
    val outputDirectory by option(
        "--output-directory",
        "-o",
        help = "Directory for generated sources and schemas.",
    ).file(canBeFile = false)
        .default(File("./generated/asyncapi"))

    val outputFile by option(
        "--output-file",
        help = "File for a bundled AsyncAPI document.",
    ).file(canBeDir = false)

    val modelPackage by option(
        "--model-package",
        help = "Package for generated payload models.",
    )

    val clientPackage by option(
        "--client-package",
        help = "Package for generated client contracts.",
    )

    val schemaPackage by option(
        "--schema-package",
        help = "Package for generated schema artifacts.",
    )
}

/**
 * Configures generated payload models.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorCliTest`
 */
internal class CliModelOptions : OptionGroup(name = "Models") {
    val modelAnnotation by option(
        "--model-annotation",
        help = "Fully qualified annotation added to generated models.",
    )

    val modelType by option(
        "--model-type",
        help = "Payload model implementation; defaults from the generator profile.",
    ).choice(
        *ModelType.entries
            .map { modelType -> modelType.configurationValue to modelType }
            .toTypedArray(),
    )

    fun toConfiguration(): CliModelConfiguration? =
        if (modelAnnotation != null || modelType != null) {
            CliModelConfiguration(
                modelAnnotation = modelAnnotation,
                modelType = modelType?.configurationValue,
            )
        } else {
            null
        }
}

/**
 * Configures generated client contracts.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorCliTest`
 */
internal class CliClientOptions : OptionGroup(name = "Clients") {
    private val clientType by option(
        "--client-type",
        help = "Client technology.",
    ).choice(
        *ClientType.entries
            .map { clientType -> clientType.configurationValue to clientType }
            .toTypedArray(),
    )

    private val clientContract by option(
        "--client-contract",
        help = "Generated client contract shape.",
    ).choice(
        *ClientContract.entries
            .map { clientContract -> clientContract.configurationValue to clientContract }
            .toTypedArray(),
    )

    private val producerEnabled by option(
        "--generate-producer",
        help = "Generate producer contracts; enabled by default.",
    ).nullableFlag("--no-generate-producer")

    private val producerAdditionalPayloadTypes by option(
        "--producer-additional-payload-type",
        help = "Add a producer method for an already serialized payload value.",
    ).choice(
        *AdditionalProducerPayloadType.entries
            .map { payloadType -> payloadType.configurationValue to payloadType.configurationValue }
            .toTypedArray(),
    ).multiple()

    private val consumerEnabled by option(
        "--generate-consumer",
        help = "Generate consumer contracts; enabled by default.",
    ).nullableFlag("--no-generate-consumer")

    private val topicParameterProperties by option(
        "--topic-parameter-property",
        help = "Map a channel parameter to a Spring property: PARAMETER=PROPERTY.",
    ).associate()

    private val clientContractValidationAnnotation by option(
        "--client-contract-validation-annotation",
        help = "Fully qualified validation annotation added to client contracts.",
    )

    private val payloadParameterValidationAnnotation by option(
        "--payload-parameter-validation-annotation",
        help = "Fully qualified validation annotation added to payload parameters.",
    )

    fun toConfiguration(): CliClientConfiguration? {
        if (!isConfigured()) {
            return null
        }

        return CliClientConfiguration(
            clientType = clientType?.configurationValue,
            clientContract = clientContract?.configurationValue,
            producer =
                if (producerEnabled != null || producerAdditionalPayloadTypes.isNotEmpty()) {
                    CliProducerConfiguration(
                        enabled = producerEnabled,
                        additionalPayloadTypes = producerAdditionalPayloadTypes.takeIf(List<String>::isNotEmpty),
                    )
                } else {
                    null
                },
            consumer = consumerEnabled?.let(::CliConsumerConfiguration),
            topicParameterProperties = topicParameterProperties,
            validationAnnotations =
                if (
                    clientContractValidationAnnotation != null ||
                    payloadParameterValidationAnnotation != null
                ) {
                    CliValidationAnnotationsConfiguration(
                        clientContract = clientContractValidationAnnotation,
                        payloadParameter = payloadParameterValidationAnnotation,
                    )
                } else {
                    null
                },
        )
    }

    private fun isConfigured(): Boolean =
        clientType != null ||
            clientContract != null ||
            producerEnabled != null ||
            producerAdditionalPayloadTypes.isNotEmpty() ||
            consumerEnabled != null ||
            topicParameterProperties.isNotEmpty() ||
            clientContractValidationAnnotation != null ||
            payloadParameterValidationAnnotation != null
}
