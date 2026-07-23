package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessageHeaders
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Prepares contract-defined Kafka headers for language-specific client generation.
 */
internal object KafkaHeaderPropertyFactory {
    fun create(headers: AnalyzedMessageHeaders?): List<KafkaHeaderProperty> {
        if (headers == null) return emptyList()

        val parameterNames =
            KafkaHeaderParameterNames.resolve(
                headerContractName = headers.typeName,
                wireNames = headers.properties.keys,
            )

        return headers.properties.map { (wireName, schema) ->
            KafkaHeaderProperty(
                wireName = wireName,
                parameterName = parameterNames.getValue(wireName),
                description = schema.description(),
                required = wireName in headers.requiredProperties,
            )
        }
    }

    private fun SchemaInterface.description(): String? = resolvedSchema()?.description

    private fun SchemaInterface.resolvedSchema(): Schema? =
        when (this) {
            is SchemaInterface.SchemaInline -> schema
            is SchemaInterface.SchemaReference -> reference.model as? Schema
            else -> null
        }
}
