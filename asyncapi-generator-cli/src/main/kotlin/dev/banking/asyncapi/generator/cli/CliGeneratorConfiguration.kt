package dev.banking.asyncapi.generator.cli

import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationRequest

/**
 * CLI model generation configuration.
 *
 * Expected behavior is covered by:
 * - `CliGeneratorConfigurationMapperTest`
 */
internal data class CliModelConfiguration(
    val modelAnnotation: String? = null,
    val modelType: String? = null,
) {
    fun toRequest(modelPackage: String?): GeneratorConfigurationRequest.Models? =
        GeneratorConfigurationRequest.models(
            enabled = true,
            packageName = modelPackage,
            annotation = modelAnnotation,
            modelType = modelType,
        )
}

/**
 * CLI producer generation configuration.
 *
 * Expected behavior is covered by:
 * - `CliGeneratorConfigurationMapperTest`
 */
internal data class CliProducerConfiguration(
    val enabled: Boolean? = null,
    val additionalPayloadTypes: List<String>? = null,
)

/**
 * CLI consumer generation configuration.
 *
 * Expected behavior is covered by:
 * - `CliGeneratorConfigurationMapperTest`
 */
internal data class CliConsumerConfiguration(
    val enabled: Boolean? = null,
)

/**
 * CLI client generation configuration.
 *
 * Expected behavior is covered by:
 * - `CliGeneratorConfigurationMapperTest`
 */
internal data class CliClientConfiguration(
    val clientType: String? = null,
    val clientContract: String? = null,
    val producer: CliProducerConfiguration? = null,
    val consumer: CliConsumerConfiguration? = null,
    val topicParameterProperties: Map<String, String> = emptyMap(),
    val validationAnnotations: CliValidationAnnotationsConfiguration? = null,
) {
    fun toRequest(
        clientPackage: String?,
        modelPackage: String?,
    ): GeneratorConfigurationRequest.Clients =
        GeneratorConfigurationRequest.clients(
            clientType = clientType,
            clientContract = clientContract,
            clientPackage = clientPackage,
            modelPackage = modelPackage,
            producerEnabled = producer?.enabled,
            producerAdditionalPayloadTypes = producer?.additionalPayloadTypes,
            consumerEnabled = consumer?.enabled,
            topicParameterProperties = topicParameterProperties,
            validationClientContract = validationAnnotations?.clientContract,
            validationPayloadParameter = validationAnnotations?.payloadParameter,
        )
}

/**
 * CLI annotation configuration for generated client contracts.
 *
 * Annotation values must be fully qualified so generated imports do not depend
 * on framework or validation-library aliases maintained by the generator.
 *
 * Expected behavior is covered by:
 * - `CliGeneratorConfigurationMapperTest`
 */
internal data class CliValidationAnnotationsConfiguration(
    val clientContract: String? = null,
    val payloadParameter: String? = null,
)
