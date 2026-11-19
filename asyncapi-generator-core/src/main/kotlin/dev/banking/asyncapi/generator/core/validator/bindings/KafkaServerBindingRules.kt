package dev.banking.asyncapi.generator.core.validator.bindings

import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_SCHEMA_REGISTRY_URL
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_SCHEMA_REGISTRY_VENDOR_URL
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats

/** The rules of the Kafka server binding. */
internal object KafkaServerBindingRules {
    private val FIELDS = setOf("schemaRegistryUrl", "schemaRegistryVendor", "bindingVersion")

    /** Checks the fields of a Kafka server binding. */
    fun check(fields: KafkaBindingFields) {
        val properties = fields.properties
        fields.checkAllowed(FIELDS)
        fields.checkString("schemaRegistryUrl")
        fields.checkString("schemaRegistryVendor")

        val url = properties["schemaRegistryUrl"]
        if (url is String && ValidationFormats.absoluteUri(url) == null) {
            fields.error(
                KAFKA_SCHEMA_REGISTRY_URL,
                "Kafka server binding 'schemaRegistryUrl' must be an absolute URI.",
                "schemaRegistryUrl",
            )
        }
        if (properties.containsKey("schemaRegistryVendor") && !properties.containsKey("schemaRegistryUrl")) {
            fields.error(
                KAFKA_SCHEMA_REGISTRY_VENDOR_URL,
                "Kafka server binding 'schemaRegistryVendor' requires 'schemaRegistryUrl'.",
                "schemaRegistryVendor",
            )
        }
    }
}
