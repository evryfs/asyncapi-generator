package dev.banking.asyncapi.generator.core.validator.bindings

/** The rules of the Kafka operation binding. */
internal object KafkaOperationBindingRules {
    private val FIELDS = setOf("groupId", "clientId", "bindingVersion")

    /** Checks the fields of a Kafka operation binding. */
    fun check(fields: KafkaBindingFields) {
        fields.checkAllowed(FIELDS)
        fields.checkSchemaField("groupId")
        fields.checkSchemaField("clientId")
    }
}
