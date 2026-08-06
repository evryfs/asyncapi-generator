package dev.banking.asyncapi.generator.core.generator.configuration

/** Payload representation exposed by generated Spring Kafka producer contracts. */
enum class ProducerPayloadType(
    val configurationValue: String,
) {
    CONTRACT("contract"),
    BYTE_ARRAY("byte-array"),
    STRING("string"),
    ;

    companion object {
        val DEFAULT: Set<ProducerPayloadType> = setOf(CONTRACT)
        val supportedConfigurationValues: List<String> = entries.map { it.configurationValue }

        fun fromConfigurationValues(
            values: List<String>?,
            path: String,
        ): Set<ProducerPayloadType> {
            if (values == null) return DEFAULT
            if (values.isEmpty()) {
                throw IllegalArgumentException(
                    "$path cannot be empty. Supported values: ${supportedConfigurationValues.joinToString(", ")}",
                )
            }

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
