package dev.banking.asyncapi.generator.systemtest

/**
 * One generator configuration, expressed once and rendered for every frontend.
 *
 * Names mirror the public option names shared by Maven XML, the Gradle DSL,
 * and the CLI, so a scenario reads like the documentation.
 *
 * @property generatorName Generator profile, such as `kotlin` or `java`.
 * @property inputSpec Spec path relative to the scenario's `spec` directory.
 * @property modelPackage Package for generated payload models.
 * @property clientPackage Package for generated client contracts.
 * @property schemaPackage Package for generated schema artifacts.
 * @property modelType Payload model implementation; the profile default when null.
 * @property modelAnnotation Fully qualified annotation added to generated models.
 * @property client Shared client configuration; no clients are generated when null.
 */
internal data class GenerationConfig(
    val generatorName: String,
    val inputSpec: String = "asyncapi.yaml",
    val modelPackage: String? = null,
    val clientPackage: String? = null,
    val schemaPackage: String? = null,
    val modelType: String? = null,
    val modelAnnotation: String? = null,
    val client: ClientConfig? = null,
)

/**
 * Client generation settings.
 *
 * @property clientType Client technology.
 * @property clientContract Generated client contract shape.
 * @property producerEnabled Whether producer contracts are generated.
 * @property producerAdditionalPayloadTypes Extra producer methods for serialized payload values.
 * @property consumerEnabled Whether consumer contracts are generated.
 * @property topicParameterProperties Channel parameter to Spring property mapping.
 * @property clientContractValidationAnnotation Annotation added to client contracts.
 * @property payloadParameterValidationAnnotation Annotation added to payload parameters.
 */
internal data class ClientConfig(
    val clientType: String = "spring-kafka",
    val clientContract: String = "interface",
    val producerEnabled: Boolean = true,
    val producerAdditionalPayloadTypes: List<String> = emptyList(),
    val consumerEnabled: Boolean = true,
    val topicParameterProperties: Map<String, String> = emptyMap(),
    val clientContractValidationAnnotation: String? = null,
    val payloadParameterValidationAnnotation: String? = null,
)
