package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Additional payload representation exposed by generated Spring Kafka producer contracts.
 *
 * @param configurationValue the value users write in `clientConfig.producer.additionalPayloadTypes`.
 */
internal enum class AdditionalProducerPayloadType(
    val configurationValue: String,
) {
    BYTE_ARRAY("byte-array"),
    STRING("string"),
    ;

    companion object {
        val supportedConfigurationValues: List<String> = entries.map { it.configurationValue }

        /** The configured [values] in declaration order without duplicates; [path] names the setting in errors. */
        fun fromConfigurationValues(
            values: List<String>?,
            path: String,
        ): Set<AdditionalProducerPayloadType> {
            if (values.isNullOrEmpty()) return emptySet()

            val configured =
                values
                    .map { value ->
                        entries.firstOrNull { it.configurationValue == value }
                            ?: throw IllegalArgumentException(
                                "Invalid $path '$value'. Supported values: " +
                                    supportedConfigurationValues.joinToString(", "),
                            )
                    }.toSet()

            return entries.filterTo(linkedSetOf()) { it in configured }
        }
    }
}
