package dev.banking.asyncapi.generator.gradle.plugin

import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationRequest

/**
 * Gradle model generation configuration.
 *
 * Expected behavior is covered by:
 * - `GradleGeneratorConfigurationMapperTest`
 */
internal data class GradleModelConfiguration(
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
 * Gradle producer generation configuration.
 *
 * Expected behavior is covered by:
 * - `GradleGeneratorConfigurationMapperTest`
 */
internal data class GradleProducerConfiguration(
    val enabled: Boolean? = null,
    val additionalPayloadTypes: List<String>? = null,
)

/**
 * Gradle consumer generation configuration.
 *
 * Expected behavior is covered by:
 * - `GradleGeneratorConfigurationMapperTest`
 */
internal data class GradleConsumerConfiguration(
    val enabled: Boolean? = null,
)

/**
 * Gradle client generation configuration.
 *
 * Expected behavior is covered by:
 * - `GradleGeneratorConfigurationMapperTest`
 */
internal data class GradleClientConfiguration(
    val clientType: String? = null,
    val clientContract: String? = null,
    val producer: GradleProducerConfiguration? = null,
    val consumer: GradleConsumerConfiguration? = null,
    val topicParameterProperties: Map<String, String> = emptyMap(),
    val validationAnnotations: GradleValidationAnnotationsConfiguration? = null,
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
 * Gradle annotation configuration for generated client contracts.
 *
 * Annotation values must be fully qualified so generated imports do not depend
 * on framework or validation-library aliases maintained by the generator.
 *
 * Expected behavior is covered by:
 * - `GradleGeneratorConfigurationMapperTest`
 */
internal data class GradleValidationAnnotationsConfiguration(
    val clientContract: String? = null,
    val payloadParameter: String? = null,
)
