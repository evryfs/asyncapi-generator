package dev.banking.asyncapi.generator.core.validator.bindings

/** The rules of the Kafka message binding. */
internal object KafkaMessageBindingRules {
    private val FIELDS =
        setOf(
            "key",
            "schemaIdLocation",
            "schemaIdPayloadEncoding",
            "schemaLookupStrategy",
            "bindingVersion",
        )

    /** Checks the fields of a Kafka message binding. */
    fun check(fields: KafkaBindingFields) {
        fields.checkAllowed(FIELDS)
        fields.checkSchemaField("key")
        fields.checkString("schemaIdLocation")
        fields.checkString("schemaIdPayloadEncoding")
        fields.checkString("schemaLookupStrategy")
    }
}
