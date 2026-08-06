package dev.banking.asyncapi.generator.core.generator.configuration

/** Additional payload representation exposed by generated Spring Kafka producer contracts. */
enum class AdditionalProducerPayloadType(
    val configurationValue: String,
) {
    BYTE_ARRAY("byte-array"),
    STRING("string"),
    ;

    companion object {
        val supportedConfigurationValues: List<String> = entries.map { it.configurationValue }

        fun fromConfigurationValues(
            values: List<String>?,
            path: String,
        ): Set<AdditionalProducerPayloadType> {
            if (values.isNullOrEmpty()) return emptySet()

            val configured =
                values.map { value ->
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
