package dev.banking.asyncapi.generator.core.validator.bindings

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_CLEANUP_POLICY

/** The rules of the `topicConfiguration` object of a Kafka channel binding. */
internal object KafkaTopicConfigurationRules {
    private const val CLEANUP_POLICY = "cleanup.policy"
    private val CLEANUP_POLICY_VALUES = setOf("delete", "compact")
    private val FIELDS_0_4 =
        setOf(
            CLEANUP_POLICY,
            "retention.ms",
            "retention.bytes",
            "delete.retention.ms",
            "max.message.bytes",
        )
    private val INTEGER_FIELDS =
        setOf(
            "retention.ms",
            "retention.bytes",
            "delete.retention.ms",
            "max.message.bytes",
        )
    private val BOOLEAN_FIELDS =
        setOf(
            "confluent.key.schema.validation",
            "confluent.value.schema.validation",
        )
    private val STRING_FIELDS =
        setOf(
            "confluent.key.subject.name.strategy",
            "confluent.value.subject.name.strategy",
        )

    /**
     * Checks the topic configuration of a channel binding.
     *
     * @param fields the fields of the topic configuration
     * @param version the binding version the fields are checked against, or `null` when it is invalid
     */
    fun check(
        fields: KafkaBindingFields,
        version: String?,
    ) {
        if (version == KafkaBindingVersion.V0_4) {
            fields.checkAllowed(FIELDS_0_4)
        }
        INTEGER_FIELDS.forEach { field ->
            fields.checkType(field, "an integer", KafkaBindingFields::isInteger)
        }
        checkCleanupPolicy(fields)

        if (version == KafkaBindingVersion.V0_5) {
            BOOLEAN_FIELDS.forEach { field -> fields.checkType(field, "a boolean") { it is Boolean } }
            STRING_FIELDS.forEach { field -> fields.checkString(field) }
        }
    }

    private fun checkCleanupPolicy(fields: KafkaBindingFields) {
        if (!fields.properties.containsKey(CLEANUP_POLICY)) return
        val value = fields.properties[CLEANUP_POLICY]
        val valid = value is List<*> && value.all { it is String && it in CLEANUP_POLICY_VALUES }
        if (!valid) {
            fields.error(
                KAFKA_CLEANUP_POLICY,
                "Kafka topic configuration '$CLEANUP_POLICY' must be an array containing only 'delete' " +
                    "and/or 'compact'.",
                CLEANUP_POLICY,
            )
        }
    }
}
