package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Spring property names used to resolve parameters in generated Kafka topic addresses.
 *
 * @param mappings channel parameter names mapped to Spring property names, sorted by parameter name.
 */
@ConsistentCopyVisibility
internal data class TopicParameterProperties private constructor(
    val mappings: Map<String, String>,
) {
    /** The Spring property name for [parameterName], or `null` when it has none. */
    operator fun get(parameterName: String): String? = mappings[parameterName]

    companion object {
        /** No mappings. */
        val EMPTY = TopicParameterProperties(emptyMap())

        /** Checks [values]; [path] names the setting in errors. */
        fun fromConfigurationValues(
            values: Map<String, String>,
            path: String,
        ): TopicParameterProperties {
            values.forEach { (parameterName, propertyName) ->
                require(parameterName.isNotBlank()) { "$path cannot contain an empty parameter name" }
                require(propertyName.isNotBlank()) { "$path.$parameterName cannot be empty" }
                require(propertyName.none(Char::isWhitespace)) { "$path.$parameterName cannot contain whitespace" }
                require(propertyName.none { it == '$' || it == '{' || it == '}' }) {
                    "$path.$parameterName must be a Spring property name without placeholder syntax, " +
                        "for example kafka.environment"
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
