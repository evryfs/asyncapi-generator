package dev.banking.asyncapi.generator.maven.plugin

import dev.banking.asyncapi.generator.core.GeneratorSettings

/**
 * Maven model generation configuration.
 */
class MavenModelConfiguration {
    var modelAnnotation: String? = null
    var modelType: String? = null

    fun toSettings(): GeneratorSettings.ModelConfig =
        GeneratorSettings.ModelConfig(modelAnnotation = modelAnnotation, modelType = modelType)
}

/**
 * Maven producer generation configuration.
 */
class MavenProducerConfiguration {
    var enabled: Boolean? = null
    var additionalPayloadTypes: List<String>? = null
}

/**
 * Maven consumer generation configuration.
 */
class MavenConsumerConfiguration {
    var enabled: Boolean? = null
}

/**
 * Maven client generation configuration.
 */
class MavenClientConfiguration {
    var clientType: String? = null
    var clientContract: String? = null
    var producer: MavenProducerConfiguration? = null
    var consumer: MavenConsumerConfiguration? = null
    var topicParameterProperties: Map<String, String>? = null
    var validationAnnotations: MavenValidationAnnotationsConfiguration? = null

    fun toSettings(): GeneratorSettings.ClientConfig =
        GeneratorSettings.ClientConfig(
            clientType = clientType,
            clientContract = clientContract,
            producer = producer?.let { GeneratorSettings.ProducerConfig(it.enabled, it.additionalPayloadTypes) },
            consumer = consumer?.let { GeneratorSettings.ConsumerConfig(it.enabled) },
            topicParameterProperties = topicParameterProperties.orEmpty(),
            validationAnnotations =
                validationAnnotations?.let {
                    GeneratorSettings.ValidationAnnotationsConfig(it.clientContract, it.payloadParameter)
                },
        )
}

/**
 * Maven annotation configuration for generated client contracts.
 *
 * Annotation values must be fully qualified so generated imports do not depend on
 * framework or validation-library aliases maintained by the generator.
 */
class MavenValidationAnnotationsConfiguration {
    var clientContract: String? = null
    var payloadParameter: String? = null
}
