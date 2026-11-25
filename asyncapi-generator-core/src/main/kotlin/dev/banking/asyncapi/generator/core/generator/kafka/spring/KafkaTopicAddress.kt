package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.configuration.TopicParameterProperties
import dev.banking.asyncapi.generator.core.generator.ir.NameCase
import dev.banking.asyncapi.generator.core.model.parameters.PARAMETER_PLACEHOLDER

/**
 * Spring-resolvable Kafka topic address used by generated client contracts.
 *
 * @param propertyPlaceholderValue The address with channel parameters replaced by Spring property placeholders.
 * @param constantName Name of the constant that holds the address.
 */
@ConsistentCopyVisibility
internal data class KafkaTopicAddress private constructor(
    val propertyPlaceholderValue: String,
    val constantName: String,
) {
    companion object {
        fun from(
            channelName: String,
            value: String,
            topicParameterProperties: TopicParameterProperties,
        ): KafkaTopicAddress {
            val parameterNames =
                PARAMETER_PLACEHOLDER
                    .findAll(value)
                    .map { match -> match.groupValues[1] }
                    .distinct()
                    .toList()
            val missingMappings =
                parameterNames.filter { parameterName -> topicParameterProperties[parameterName] == null }

            require(missingMappings.isEmpty()) {
                "Cannot generate Spring Kafka client for channel '$channelName': " +
                    "topic address '$value' uses channel parameters $missingMappings without matching " +
                    "topicParameterProperties entries. Configured entries: " +
                    topicParameterProperties.mappings.keys.sorted()
            }

            val propertyPlaceholderValue =
                PARAMETER_PLACEHOLDER.replace(value) { match ->
                    val parameterName = match.groupValues[1]
                    "${'$'}{${topicParameterProperties[parameterName]}}"
                }

            return KafkaTopicAddress(
                propertyPlaceholderValue = propertyPlaceholderValue,
                constantName = "${NameCase.toUpperSnakeCase(channelName)}_TOPIC_ADDRESS",
            )
        }
    }
}
