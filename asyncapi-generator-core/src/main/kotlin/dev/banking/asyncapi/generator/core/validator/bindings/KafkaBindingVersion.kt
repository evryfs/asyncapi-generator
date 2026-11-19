package dev.banking.asyncapi.generator.core.validator.bindings

import dev.banking.asyncapi.generator.core.model.bindings.ProtocolBinding
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_BINDING_VERSION
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_BINDING_VERSION_TYPE

/** The Kafka binding specification versions the generator supports. */
internal object KafkaBindingVersion {
    /** The oldest supported version. */
    const val V0_4 = "0.4.0"

    /** The newest supported version, which a binding without `bindingVersion` is checked against. */
    const val V0_5 = "0.5.0"

    private val SUPPORTED = linkedSetOf(V0_4, V0_5)

    /**
     * Reads the version [binding] declares and reports one that is not a string or not supported.
     *
     * @return the declared version, the newest one when none is declared, or `null` when it is invalid
     */
    fun resolve(
        binding: ProtocolBinding,
        fields: KafkaBindingFields,
    ): String? {
        if (!fields.properties.containsKey("bindingVersion")) return V0_5
        val value = binding.bindingVersion
        return when {
            value !is String -> {
                fields.error(
                    KAFKA_BINDING_VERSION_TYPE,
                    "Kafka binding 'bindingVersion' must be a string.",
                    "bindingVersion",
                )
                null
            }
            value !in SUPPORTED -> {
                fields.error(
                    KAFKA_BINDING_VERSION,
                    "Kafka binding version '$value' is not supported; supported versions are " +
                        SUPPORTED.joinToString(),
                    "bindingVersion",
                )
                null
            }
            else -> value
        }
    }
}
