package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.SourceIdentifierMapper
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidKafkaHeaderName
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.KafkaHeaderParameterNameCollision

/** Converts Kafka wire-header names into camelCase source identifiers shared by Java and Kotlin output. */
internal object KafkaHeaderParameterNames {
    /** Parameters every consumer or producer method declares itself, which a header must not reuse. */
    private val fixedParameterNames = setOf("payload", "receivedTopic", "receivedKey", "messageKey")

    fun resolve(
        headerContractName: String,
        wireNames: Collection<String>,
    ): Map<String, String> {
        val parameterNames =
            wireNames.associateWith { wireName ->
                normalize(
                    headerContractName = headerContractName,
                    wireName = wireName,
                )
            }

        parameterNames
            .entries
            .groupBy(
                keySelector = Map.Entry<String, String>::value,
                valueTransform = Map.Entry<String, String>::key,
            ).entries
            .firstOrNull { (_, matchingWireNames) -> matchingWireNames.size > 1 }
            ?.let { (parameterName, matchingWireNames) ->
                throw KafkaHeaderParameterNameCollision(
                    headerContractName = headerContractName,
                    wireNames = matchingWireNames,
                    parameterName = parameterName,
                )
            }

        parameterNames
            .entries
            .firstOrNull { (_, parameterName) -> parameterName in fixedParameterNames }
            ?.let { (wireName, parameterName) ->
                throw KafkaHeaderParameterNameCollision(
                    headerContractName = headerContractName,
                    wireNames = listOf(wireName),
                    parameterName = parameterName,
                    fixedParameter = true,
                )
            }

        return parameterNames
    }

    private fun normalize(
        headerContractName: String,
        wireName: String,
    ): String {
        if (wireName.isBlank()) {
            throw InvalidKafkaHeaderName(
                headerContractName = headerContractName,
                wireName = wireName,
                reason = "The wire-header name is blank.",
            )
        }

        val normalized = MapperUtil.toCamelCase(wireName)
        if (normalized.isEmpty()) {
            throw InvalidKafkaHeaderName(
                headerContractName = headerContractName,
                wireName = wireName,
                reason = "The wire-header name does not contain an ASCII letter or digit.",
            )
        }

        val identifier =
            if (normalized.first().isDigit()) {
                "_$normalized"
            } else {
                normalized
            }

        return if (SourceIdentifierMapper.isReserved(identifier)) {
            "${identifier}_"
        } else {
            identifier
        }
    }
}
