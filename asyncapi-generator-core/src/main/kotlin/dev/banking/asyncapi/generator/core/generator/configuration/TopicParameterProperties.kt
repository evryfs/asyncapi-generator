package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Spring property names used to resolve parameters in generated Kafka topic addresses.
 */
@ConsistentCopyVisibility
data class TopicParameterProperties private constructor(
    val mappings: Map<String, String>,
) {
    operator fun get(parameterName: String): String? = mappings[parameterName]

    companion object {
        val EMPTY = TopicParameterProperties(emptyMap())

        fun fromConfigurationValues(
            values: Map<String, String>,
            path: String,
        ): TopicParameterProperties {
            values.forEach { (parameterName, propertyName) ->
                if (parameterName.isBlank()) {
                    throw IllegalArgumentException("$path cannot contain an empty parameter name")
                }
                if (propertyName.isBlank()) {
                    throw IllegalArgumentException("$path.$parameterName cannot be empty")
                }
                if (propertyName.any(Char::isWhitespace)) {
                    throw IllegalArgumentException("$path.$parameterName cannot contain whitespace")
                }
                if (propertyName.any { character -> character == '$' || character == '{' || character == '}' }) {
                    throw IllegalArgumentException(
                        "$path.$parameterName must be a Spring property name without placeholder syntax, " +
                            "for example kafka.environment",
                    )
                }
            }

            return if (values.isEmpty()) {
                EMPTY
            } else {
                TopicParameterProperties(values.toSortedMap())
            }
        }
    }
}
