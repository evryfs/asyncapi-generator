package dev.banking.asyncapi.generator.core.validator.bindings

/** The rules of the Kafka channel binding. */
internal object KafkaChannelBindingRules {
    private const val TOPIC_CONFIGURATION = "topicConfiguration"
    private val FIELDS = setOf("topic", "partitions", "replicas", TOPIC_CONFIGURATION, "bindingVersion")

    /**
     * Checks the fields of a Kafka channel binding.
     *
     * @param fields the fields of the binding
     * @param version the binding version the fields are checked against, or `null` when it is invalid
     */
    fun check(
        fields: KafkaBindingFields,
        version: String?,
    ) {
        fields.checkAllowed(FIELDS)
        fields.checkString("topic")
        fields.checkPositiveInteger("partitions")
        fields.checkPositiveInteger("replicas")

        val configuration = fields.properties[TOPIC_CONFIGURATION]
        when {
            !fields.properties.containsKey(TOPIC_CONFIGURATION) -> Unit
            configuration is Map<*, *> && configuration.keys.all { it is String } ->
                KafkaTopicConfigurationRules.check(
                    fields.nested(TOPIC_CONFIGURATION, configuration.mapKeys { it.key as String }),
                    version,
                )
            else -> fields.typeError(TOPIC_CONFIGURATION, "an object")
        }
    }
}
