package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationRequest

/**
 * Maven model generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenModelConfiguration {
    var modelAnnotation: String? = null
    var modelType: String? = null

    fun toRequest(modelPackage: String?): GeneratorConfigurationRequest.Models? =
        GeneratorConfigurationRequest.models(
            enabled = true,
            packageName = modelPackage,
            annotation = modelAnnotation,
            modelType = modelType,
        )
}

/**
 * Maven producer generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenProducerConfiguration {
    var enabled: Boolean? = null
}

/**
 * Maven consumer generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenConsumerConfiguration {
    var enabled: Boolean? = null
}

/**
 * Maven client generation configuration.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenClientConfiguration {
    var clientType: String? = null
    var clientContract: String? = null
    var producer: MavenProducerConfiguration? = null
    var consumer: MavenConsumerConfiguration? = null
    var topicParameterProperties: Map<String, String>? = null
    var validationAnnotations: MavenValidationAnnotationsConfiguration? = null

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
            consumerEnabled = consumer?.enabled,
            topicParameterProperties = topicParameterProperties.orEmpty(),
            validationClientContract = validationAnnotations?.clientContract,
            validationPayloadParameter = validationAnnotations?.payloadParameter,
        )
}

/**
 * Maven annotation configuration for generated client contracts.
 *
 * Annotation values must be fully qualified so generated imports do not depend on
 * framework or validation-library aliases maintained by the generator.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorMojoTest`
 */
class MavenValidationAnnotationsConfiguration {
    var clientContract: String? = null
    var payloadParameter: String? = null
}
