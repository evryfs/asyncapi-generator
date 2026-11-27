package dev.banking.asyncapi.generator.core

import dev.banking.asyncapi.generator.core.generator.configuration.AdditionalProducerPayloadType
import dev.banking.asyncapi.generator.core.generator.configuration.ClientContract
import dev.banking.asyncapi.generator.core.generator.configuration.ClientType
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorName
import dev.banking.asyncapi.generator.core.generator.configuration.ModelType
import java.io.File

/**
 * Generator settings as users write them in the CLI, Maven or Gradle.
 *
 * Every field mirrors one documented setting and keeps its raw value, so
 * [AsyncApiGenerator.configure] can check it and report problems under the
 * setting's public name.
 *
 * @param generatorName the generator profile, for example `kotlin` or `avro-schema`.
 * @param outputDirectory the directory generated sources and schemas are written to.
 * @param outputFile the file a bundled AsyncAPI document is written to.
 * @param modelPackage the package of generated payload models.
 * @param clientPackage the package of generated client contracts.
 * @param schemaPackage the package of generated schema artifacts.
 * @param modelConfig the payload model settings, or `null` when none are configured.
 * @param clientConfig the client settings, or `null` when none are configured.
 * @param suppressedWarnings the configuration warnings to silence.
 * @param manifest where generated files are recorded so stale ones are removed, or `null` to keep every file.
 */
data class GeneratorSettings(
    val generatorName: String?,
    val outputDirectory: File,
    val outputFile: File? = null,
    val modelPackage: String? = null,
    val clientPackage: String? = null,
    val schemaPackage: String? = null,
    val modelConfig: ModelConfig? = null,
    val clientConfig: ClientConfig? = null,
    val suppressedWarnings: List<String> = emptyList(),
    val manifest: Manifest? = null,
) {
    /**
     * The `modelConfig` settings.
     *
     * @param modelAnnotation the fully qualified annotation placed on generated models.
     * @param modelType the payload model implementation; the generator profile picks one when `null`.
     */
    data class ModelConfig(
        val modelAnnotation: String? = null,
        val modelType: String? = null,
    )

    /**
     * The `clientConfig` settings.
     *
     * @param clientType the client technology; `spring-kafka` when omitted.
     * @param clientContract the shape of the generated client contracts; `interface` when omitted.
     * @param producer the producer settings, or `null` when none are configured.
     * @param consumer the consumer settings, or `null` when none are configured.
     * @param topicParameterProperties channel parameter names mapped to Spring property names.
     * @param validationAnnotations the validation annotations, or `null` when none are configured.
     */
    data class ClientConfig(
        val clientType: String? = null,
        val clientContract: String? = null,
        val producer: ProducerConfig? = null,
        val consumer: ConsumerConfig? = null,
        val topicParameterProperties: Map<String, String> = emptyMap(),
        val validationAnnotations: ValidationAnnotationsConfig? = null,
    )

    /**
     * The `clientConfig.producer` settings.
     *
     * @param enabled whether producer contracts are generated; `null` means yes.
     * @param additionalPayloadTypes the already serialized payload types that get their own producer method.
     */
    data class ProducerConfig(
        val enabled: Boolean? = null,
        val additionalPayloadTypes: List<String>? = null,
    )

    /**
     * The `clientConfig.consumer` settings.
     *
     * @param enabled whether consumer contracts are generated; `null` means yes.
     */
    data class ConsumerConfig(
        val enabled: Boolean? = null,
    )

    /**
     * The `clientConfig.validationAnnotations` settings.
     *
     * Values must be fully qualified so generated imports do not depend on
     * framework or validation-library aliases maintained by the generator.
     *
     * @param clientContract the annotation placed on generated client contracts.
     * @param payloadParameter the annotation placed on payload parameters.
     */
    data class ValidationAnnotationsConfig(
        val clientContract: String? = null,
        val payloadParameter: String? = null,
    )

    /**
     * The record of the files one execution generated, shared by every execution of a build.
     *
     * @param file the manifest file; created on first use.
     * @param executionName the stable name of the execution, such as the Maven execution id.
     */
    data class Manifest(
        val file: File,
        val executionName: String,
    )

    /** The values each choice setting accepts, as users write them. */
    companion object {
        /** The values of `generatorName`. */
        val generatorNames: List<String> = GeneratorName.supportedConfigurationValues

        /** The values of `modelConfig.modelType`. */
        val modelTypes: List<String> = ModelType.supportedConfigurationValues

        /** The values of `clientConfig.clientType`. */
        val clientTypes: List<String> = ClientType.supportedConfigurationValues

        /** The values of `clientConfig.clientContract`. */
        val clientContracts: List<String> = ClientContract.supportedConfigurationValues

        /** The values of `clientConfig.producer.additionalPayloadTypes`. */
        val additionalPayloadTypes: List<String> = AdditionalProducerPayloadType.supportedConfigurationValues
    }
}
