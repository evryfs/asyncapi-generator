package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessageHeaders

/** Prepares contract-defined Kafka headers for language-specific client generation. */
internal object KafkaHeaderPropertyFactory {
    fun create(
        headers: AnalyzedMessageHeaders?,
        messageName: String,
    ): List<KafkaHeaderProperty> {
        if (headers == null) return emptyList()

        val parameterNames =
            KafkaHeaderParameterNames.resolve(
                headerContractName = messageName,
                wireNames = headers.properties.keys,
            )

        return headers.properties.map { (wireName, schema) ->
            val required = wireName in headers.requiredProperties
            val headerType =
                KafkaHeaderTypeResolver.resolve(
                    headerContractName = messageName,
                    wireName = wireName,
                    schema = schema,
                )
            KafkaHeaderProperty(
                wireName = wireName,
                parameterName = parameterNames.getValue(wireName),
                javaTypeName = headerType.javaTypeName,
                kotlinTypeName = headerType.kotlinTypeName,
                importName = headerType.importName,
                description = headerType.description,
                required = required,
                nullable = !required || headerType.schemaNullable,
            )
        }
    }
}
