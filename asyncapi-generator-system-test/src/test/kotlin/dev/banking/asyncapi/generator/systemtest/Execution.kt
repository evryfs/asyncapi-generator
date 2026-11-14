package dev.banking.asyncapi.generator.systemtest

/**
 * One generator execution, expressed once and rendered for every frontend.
 *
 * Names mirror the public option names shared by Maven XML, the Gradle DSL,
 * and the CLI, so a scenario reads like the documentation. Paths are relative
 * to the consumer project and are passed to every frontend as relative paths,
 * so relative path resolution is exercised everywhere.
 *
 * @property id Maven execution id and Gradle execution name.
 * @property generatorName Generator profile, such as `kotlin` or `java`.
 * @property inputSpec Spec path relative to the scenario's `spec` directory.
 * @property outputDirectory Output directory; the frontend's default when null.
 * @property outputFile Bundled AsyncAPI document to write, if any.
 * @property modelPackage Package for generated payload models.
 * @property clientPackage Package for generated client contracts.
 * @property schemaPackage Package for generated schema artifacts.
 * @property modelType Payload model implementation; the profile default when null.
 * @property modelAnnotation Fully qualified annotation added to generated models.
 */
internal data class Execution(
    val id: String,
    val generatorName: String,
    val inputSpec: String = "asyncapi.yaml",
    val outputDirectory: String? = null,
    val outputFile: String? = null,
    val modelPackage: String? = null,
    val clientPackage: String? = null,
    val schemaPackage: String? = null,
    val modelType: String? = null,
    val modelAnnotation: String? = null,
) {
    /** True when the execution asks for generated sources or schemas, not only a bundled document. */
    val generatesArtifacts: Boolean
        get() = modelPackage != null || clientPackage != null || schemaPackage != null
}

/**
 * Client generation settings, shared by the executions of a scenario that set a client package.
 *
 * Gradle only has them in the extension, for every execution. Maven and the CLI
 * reject them on an execution without a client package, so Maven renders them per
 * execution and the CLI passes them only to client executions.
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
