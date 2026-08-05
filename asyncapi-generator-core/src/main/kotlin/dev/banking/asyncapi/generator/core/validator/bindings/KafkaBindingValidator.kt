package dev.banking.asyncapi.generator.core.validator.bindings

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation
import dev.banking.asyncapi.generator.core.model.bindings.ProtocolBinding
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_BINDING_FIELD
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_BINDING_FIELD_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_BINDING_VERSION_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_BINDING_VERSION_UNSUPPORTED
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_CHANNEL_POSITIVE_INTEGER
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_CLEANUP_POLICY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_SCHEMA_REGISTRY_URL_FORMAT
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_SCHEMA_REGISTRY_VENDOR_REQUIRES_URL
import dev.banking.asyncapi.generator.core.validator.schemas.SchemaValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats
import java.math.BigDecimal
import java.math.BigInteger

internal class KafkaBindingValidator(
    private val asyncApiContext: AsyncApiContext,
) {
    private val schemaValidator by lazy { SchemaValidator(asyncApiContext) }

    fun validate(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        results: ValidationCollector,
    ) {
        val version = validateVersion(binding, properties, results)
        validateSchemaFields(binding, results)

        when (binding.location) {
            BindingLocation.SERVER -> validateServer(binding, properties, results)
            BindingLocation.CHANNEL -> validateChannel(binding, properties, version, results)
            BindingLocation.OPERATION -> validateOperation(binding, properties, results)
            BindingLocation.MESSAGE -> validateMessage(binding, properties, results)
            BindingLocation.SCHEMA,
            BindingLocation.UNKNOWN,
            -> Unit
        }
    }

    private fun validateVersion(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        results: ValidationCollector,
    ): String? {
        if (!properties.containsKey("bindingVersion")) return LATEST_SUPPORTED_VERSION
        val value = binding.bindingVersion
        if (value !is String) {
            results.error(
                KAFKA_BINDING_VERSION_TYPE,
                "Kafka binding 'bindingVersion' must be a string.",
                sourceLocation = location(binding, "bindingVersion"),
            )
            return null
        }
        if (value !in SUPPORTED_VERSIONS) {
            results.error(
                KAFKA_BINDING_VERSION_UNSUPPORTED,
                "Kafka binding version '$value' is not supported; supported versions are " +
                    SUPPORTED_VERSIONS.joinToString(),
                sourceLocation = location(binding, "bindingVersion"),
            )
            return null
        }
        return value
    }

    private fun validateSchemaFields(binding: ProtocolBinding, results: ValidationCollector) {
        binding.schemaFields.forEach { (fieldName, schema) ->
            schemaValidator.validateInterface(schema, "Kafka binding '$fieldName'", results)
        }
    }

    private fun validateServer(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        results: ValidationCollector,
    ) {
        validateAllowedFields(binding, properties, SERVER_FIELDS, results)
        validateString(binding, properties, "schemaRegistryUrl", results)
        validateString(binding, properties, "schemaRegistryVendor", results)

        val url = properties["schemaRegistryUrl"]
        if (url is String && ValidationFormats.absoluteUri(url) == null) {
            results.error(
                KAFKA_SCHEMA_REGISTRY_URL_FORMAT,
                "Kafka server binding 'schemaRegistryUrl' must be an absolute URI.",
                sourceLocation = location(binding, "schemaRegistryUrl"),
            )
        }
        if (properties.containsKey("schemaRegistryVendor") && !properties.containsKey("schemaRegistryUrl")) {
            results.error(
                KAFKA_SCHEMA_REGISTRY_VENDOR_REQUIRES_URL,
                "Kafka server binding 'schemaRegistryVendor' requires 'schemaRegistryUrl'.",
                sourceLocation = location(binding, "schemaRegistryVendor"),
            )
        }
    }

    private fun validateChannel(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        version: String?,
        results: ValidationCollector,
    ) {
        validateAllowedFields(binding, properties, CHANNEL_FIELDS, results)
        validateString(binding, properties, "topic", results)
        validatePositiveInteger(binding, properties, "partitions", results)
        validatePositiveInteger(binding, properties, "replicas", results)

        if (!properties.containsKey("topicConfiguration")) return
        val configuration = properties["topicConfiguration"]
        if (configuration !is Map<*, *> || configuration.keys.any { it !is String }) {
            typeError(binding, "topicConfiguration", "an object", results)
            return
        }
        @Suppress("UNCHECKED_CAST")
        validateTopicConfiguration(binding, configuration as Map<String, Any?>, version, results)
    }

    private fun validateTopicConfiguration(
        binding: ProtocolBinding,
        configuration: Map<String, Any?>,
        version: String?,
        results: ValidationCollector,
    ) {
        val sourceModel = configuration
        if (version == VERSION_0_4) {
            validateAllowedFields(binding, configuration, TOPIC_CONFIGURATION_0_4_FIELDS, results, sourceModel)
        }
        TOPIC_CONFIGURATION_INTEGER_FIELDS.forEach { field ->
            validateInteger(binding, configuration, field, results, sourceModel)
        }
        validateCleanupPolicy(binding, configuration, results, sourceModel)

        if (version == VERSION_0_5) {
            TOPIC_CONFIGURATION_BOOLEAN_FIELDS.forEach { field ->
                validateType(binding, configuration, field, "a boolean", results, sourceModel) { it is Boolean }
            }
            TOPIC_CONFIGURATION_STRING_FIELDS.forEach { field ->
                validateType(binding, configuration, field, "a string", results, sourceModel) { it is String }
            }
        }
    }

    private fun validateCleanupPolicy(
        binding: ProtocolBinding,
        configuration: Map<String, Any?>,
        results: ValidationCollector,
        sourceModel: Any,
    ) {
        if (!configuration.containsKey(CLEANUP_POLICY)) return
        val value = configuration[CLEANUP_POLICY]
        val valid = value is List<*> && value.all { it is String && it in CLEANUP_POLICY_VALUES }
        if (!valid) {
            results.error(
                KAFKA_CLEANUP_POLICY,
                "Kafka topic configuration '$CLEANUP_POLICY' must be an array containing only 'delete' and/or 'compact'.",
                sourceLocation = location(sourceModel, binding, CLEANUP_POLICY),
            )
        }
    }

    private fun validateOperation(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        results: ValidationCollector,
    ) {
        validateAllowedFields(binding, properties, OPERATION_FIELDS, results)
        validateParsedSchemaField(binding, properties, "groupId", results)
        validateParsedSchemaField(binding, properties, "clientId", results)
    }

    private fun validateMessage(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        results: ValidationCollector,
    ) {
        validateAllowedFields(binding, properties, MESSAGE_FIELDS, results)
        validateParsedSchemaField(binding, properties, "key", results)
        validateString(binding, properties, "schemaIdLocation", results)
        validateString(binding, properties, "schemaIdPayloadEncoding", results)
        validateString(binding, properties, "schemaLookupStrategy", results)
    }

    private fun validateParsedSchemaField(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        field: String,
        results: ValidationCollector,
    ) {
        if (properties.containsKey(field) && field !in binding.schemaFields) {
            typeError(binding, field, "a Schema Object or Reference Object", results)
        }
    }

    private fun validateAllowedFields(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        allowedFields: Set<String>,
        results: ValidationCollector,
        sourceModel: Any = binding,
    ) {
        properties.keys
            .filterNot { it in allowedFields || it.startsWith("x-") }
            .forEach { field ->
                results.error(
                    KAFKA_BINDING_FIELD,
                    "Kafka ${binding.location.name.lowercase()} binding does not define property '$field'.",
                    sourceLocation = location(sourceModel, binding, field),
                )
            }
    }

    private fun validateString(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        field: String,
        results: ValidationCollector,
    ) = validateType(binding, properties, field, "a string", results) { it is String }

    private fun validatePositiveInteger(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        field: String,
        results: ValidationCollector,
    ) {
        if (!properties.containsKey(field)) return
        if (!isInteger(properties[field]) || decimal(properties[field])?.signum() != 1) {
            results.error(
                KAFKA_CHANNEL_POSITIVE_INTEGER,
                "Kafka channel binding '$field' must be a positive integer.",
                sourceLocation = location(binding, field),
            )
        }
    }

    private fun validateInteger(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        field: String,
        results: ValidationCollector,
        sourceModel: Any,
    ) = validateType(binding, properties, field, "an integer", results, sourceModel, ::isInteger)

    private fun validateType(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        field: String,
        expected: String,
        results: ValidationCollector,
        sourceModel: Any = binding,
        predicate: (Any?) -> Boolean,
    ) {
        if (properties.containsKey(field) && !predicate(properties[field])) {
            results.error(
                KAFKA_BINDING_FIELD_TYPE,
                "Kafka ${binding.location.name.lowercase()} binding '$field' must be $expected.",
                sourceLocation = location(sourceModel, binding, field),
            )
        }
    }

    private fun typeError(
        binding: ProtocolBinding,
        field: String,
        expected: String,
        results: ValidationCollector,
    ) {
        results.error(
            KAFKA_BINDING_FIELD_TYPE,
            "Kafka ${binding.location.name.lowercase()} binding '$field' must be $expected.",
            sourceLocation = location(binding, field),
        )
    }

    private fun location(binding: ProtocolBinding, field: String) =
        asyncApiContext.getSourceLocation(binding, field)
            ?: asyncApiContext.getSourceLocation(binding)

    private fun location(sourceModel: Any, binding: ProtocolBinding, field: String) =
        asyncApiContext.getSourceLocation(sourceModel, field)
            ?: location(binding, "topicConfiguration")

    private fun isInteger(value: Any?): Boolean =
        decimal(value)?.stripTrailingZeros()?.scale()?.let { it <= 0 } == true

    private fun decimal(value: Any?): BigDecimal? =
        when (value) {
            is BigDecimal -> value
            is BigInteger -> value.toBigDecimal()
            is Byte, is Short, is Int, is Long -> BigDecimal(value.toString())
            is Float -> value.takeIf(Float::isFinite)?.toString()?.toBigDecimalOrNull()
            is Double -> value.takeIf(Double::isFinite)?.toString()?.toBigDecimalOrNull()
            else -> null
        }

    private companion object {
        const val VERSION_0_4 = "0.4.0"
        const val VERSION_0_5 = "0.5.0"
        const val LATEST_SUPPORTED_VERSION = VERSION_0_5
        val SUPPORTED_VERSIONS = linkedSetOf(VERSION_0_4, VERSION_0_5)

        val SERVER_FIELDS = setOf("schemaRegistryUrl", "schemaRegistryVendor", "bindingVersion")
        val CHANNEL_FIELDS = setOf("topic", "partitions", "replicas", "topicConfiguration", "bindingVersion")
        val OPERATION_FIELDS = setOf("groupId", "clientId", "bindingVersion")
        val MESSAGE_FIELDS = setOf(
            "key",
            "schemaIdLocation",
            "schemaIdPayloadEncoding",
            "schemaLookupStrategy",
            "bindingVersion",
        )
        val TOPIC_CONFIGURATION_0_4_FIELDS = setOf(
            "cleanup.policy",
            "retention.ms",
            "retention.bytes",
            "delete.retention.ms",
            "max.message.bytes",
        )
        val TOPIC_CONFIGURATION_INTEGER_FIELDS = setOf(
            "retention.ms",
            "retention.bytes",
            "delete.retention.ms",
            "max.message.bytes",
        )
        val TOPIC_CONFIGURATION_BOOLEAN_FIELDS = setOf(
            "confluent.key.schema.validation",
            "confluent.value.schema.validation",
        )
        val TOPIC_CONFIGURATION_STRING_FIELDS = setOf(
            "confluent.key.subject.name.strategy",
            "confluent.value.subject.name.strategy",
        )
        const val CLEANUP_POLICY = "cleanup.policy"
        val CLEANUP_POLICY_VALUES = setOf("delete", "compact")
    }
}
