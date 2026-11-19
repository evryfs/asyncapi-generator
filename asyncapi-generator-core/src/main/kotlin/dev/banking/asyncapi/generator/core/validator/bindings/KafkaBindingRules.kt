package dev.banking.asyncapi.generator.core.validator.bindings

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation
import dev.banking.asyncapi.generator.core.model.bindings.ProtocolBinding
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_BINDING_FIELD
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_BINDING_FIELD_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_BINDING_VERSION
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_BINDING_VERSION_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_CHANNEL_POSITIVE_INTEGER
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_CLEANUP_POLICY
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_SCHEMA_REGISTRY_URL
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_SCHEMA_REGISTRY_VENDOR_URL
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk
import java.math.BigDecimal
import java.math.BigInteger

/**
 * The rules of Kafka bindings, by the Kafka binding specification versions 0.4.0 and 0.5.0.
 *
 * @param asyncApiContext the load whose models are checked
 */
internal class KafkaBindingRules(
    private val asyncApiContext: AsyncApiContext,
) : ProtocolBindingRules {
    override val protocol: String = "kafka"

    override fun check(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        val results = walk.results
        val version = validateVersion(binding, properties, path, results)
        validateSchemaFields(binding, path, walk)

        when (binding.location) {
            BindingLocation.SERVER -> validateServer(binding, properties, path, results)
            BindingLocation.CHANNEL -> validateChannel(binding, properties, version, path, results)
            BindingLocation.OPERATION -> validateOperation(binding, properties, path, results)
            BindingLocation.MESSAGE -> validateMessage(binding, properties, path, results)
            BindingLocation.SCHEMA,
            BindingLocation.UNKNOWN,
            -> Unit
        }
    }

    private fun validateVersion(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        path: ValidationPath,
        results: ValidationCollector,
    ): String? {
        if (!properties.containsKey("bindingVersion")) return LATEST_SUPPORTED_VERSION
        val value = binding.bindingVersion
        if (value !is String) {
            results.error(
                KAFKA_BINDING_VERSION_TYPE,
                "$path Kafka binding 'bindingVersion' must be a string.",
                sourceLocation = location(binding, "bindingVersion"),
            )
            return null
        }
        if (value !in SUPPORTED_VERSIONS) {
            results.error(
                KAFKA_BINDING_VERSION,
                "$path Kafka binding version '$value' is not supported; supported versions are " +
                    SUPPORTED_VERSIONS.joinToString(),
                sourceLocation = location(binding, "bindingVersion"),
            )
            return null
        }
        return value
    }

    private fun validateSchemaFields(
        binding: ProtocolBinding,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        binding.schemaFields.forEach { (fieldName, schema) ->
            walk.schema(schema, path.named("Kafka binding", fieldName))
        }
    }

    private fun validateServer(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        validateAllowedFields(binding, properties, SERVER_FIELDS, path, results)
        validateString(binding, properties, "schemaRegistryUrl", path, results)
        validateString(binding, properties, "schemaRegistryVendor", path, results)

        val url = properties["schemaRegistryUrl"]
        if (url is String && ValidationFormats.absoluteUri(url) == null) {
            results.error(
                KAFKA_SCHEMA_REGISTRY_URL,
                "$path Kafka server binding 'schemaRegistryUrl' must be an absolute URI.",
                sourceLocation = location(binding, "schemaRegistryUrl"),
            )
        }
        if (properties.containsKey("schemaRegistryVendor") && !properties.containsKey("schemaRegistryUrl")) {
            results.error(
                KAFKA_SCHEMA_REGISTRY_VENDOR_URL,
                "$path Kafka server binding 'schemaRegistryVendor' requires 'schemaRegistryUrl'.",
                sourceLocation = location(binding, "schemaRegistryVendor"),
            )
        }
    }

    private fun validateChannel(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        version: String?,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        validateAllowedFields(binding, properties, CHANNEL_FIELDS, path, results)
        validateString(binding, properties, "topic", path, results)
        validatePositiveInteger(binding, properties, "partitions", path, results)
        validatePositiveInteger(binding, properties, "replicas", path, results)

        if (!properties.containsKey("topicConfiguration")) return
        val configuration = properties["topicConfiguration"]
        if (configuration !is Map<*, *> || configuration.keys.any { it !is String }) {
            typeError(binding, "topicConfiguration", "an object", path, results)
            return
        }
        @Suppress("UNCHECKED_CAST")
        validateTopicConfiguration(binding, configuration as Map<String, Any?>, version, path, results)
    }

    private fun validateTopicConfiguration(
        binding: ProtocolBinding,
        configuration: Map<String, Any?>,
        version: String?,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        val parent = "topicConfiguration"
        if (version == VERSION_0_4) {
            validateAllowedFields(binding, configuration, TOPIC_CONFIGURATION_0_4_FIELDS, path, results, parent)
        }
        TOPIC_CONFIGURATION_INTEGER_FIELDS.forEach { field ->
            validateType(binding, configuration, field, "an integer", path, results, parent, ::isInteger)
        }
        validateCleanupPolicy(binding, configuration, path, results, parent)

        if (version == VERSION_0_5) {
            TOPIC_CONFIGURATION_BOOLEAN_FIELDS.forEach { field ->
                validateType(binding, configuration, field, "a boolean", path, results, parent) { it is Boolean }
            }
            TOPIC_CONFIGURATION_STRING_FIELDS.forEach { field ->
                validateType(binding, configuration, field, "a string", path, results, parent) { it is String }
            }
        }
    }

    private fun validateCleanupPolicy(
        binding: ProtocolBinding,
        configuration: Map<String, Any?>,
        path: ValidationPath,
        results: ValidationCollector,
        parent: String,
    ) {
        if (!configuration.containsKey(CLEANUP_POLICY)) return
        val value = configuration[CLEANUP_POLICY]
        val valid = value is List<*> && value.all { it is String && it in CLEANUP_POLICY_VALUES }
        if (!valid) {
            results.error(
                KAFKA_CLEANUP_POLICY,
                "$path Kafka topic configuration '$CLEANUP_POLICY' must be an array containing only 'delete' " +
                    "and/or 'compact'.",
                sourceLocation = location(binding, parent, CLEANUP_POLICY),
            )
        }
    }

    private fun validateOperation(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        validateAllowedFields(binding, properties, OPERATION_FIELDS, path, results)
        validateParsedSchemaField(binding, properties, "groupId", path, results)
        validateParsedSchemaField(binding, properties, "clientId", path, results)
    }

    private fun validateMessage(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        validateAllowedFields(binding, properties, MESSAGE_FIELDS, path, results)
        validateParsedSchemaField(binding, properties, "key", path, results)
        validateString(binding, properties, "schemaIdLocation", path, results)
        validateString(binding, properties, "schemaIdPayloadEncoding", path, results)
        validateString(binding, properties, "schemaLookupStrategy", path, results)
    }

    private fun validateParsedSchemaField(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        field: String,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        if (properties.containsKey(field) && field !in binding.schemaFields) {
            typeError(binding, field, "a Schema Object or Reference Object", path, results)
        }
    }

    private fun validateAllowedFields(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        allowedFields: Set<String>,
        path: ValidationPath,
        results: ValidationCollector,
        parent: String? = null,
    ) {
        properties.keys
            .filterNot { it in allowedFields || it.startsWith("x-") }
            .forEach { field ->
                results.error(
                    KAFKA_BINDING_FIELD,
                    "$path Kafka ${binding.location.name.lowercase()} binding does not define property '$field'.",
                    sourceLocation = location(binding, parent, field),
                )
            }
    }

    private fun validateString(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        field: String,
        path: ValidationPath,
        results: ValidationCollector,
    ) = validateType(binding, properties, field, "a string", path, results) { it is String }

    private fun validatePositiveInteger(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        field: String,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        if (!properties.containsKey(field)) return
        if (!isInteger(properties[field]) || decimal(properties[field])?.signum() != 1) {
            results.error(
                KAFKA_CHANNEL_POSITIVE_INTEGER,
                "$path Kafka channel binding '$field' must be a positive integer.",
                sourceLocation = location(binding, field),
            )
        }
    }

    private fun validateType(
        binding: ProtocolBinding,
        properties: Map<String, Any?>,
        field: String,
        expected: String,
        path: ValidationPath,
        results: ValidationCollector,
        parent: String? = null,
        predicate: (Any?) -> Boolean,
    ) {
        if (properties.containsKey(field) && !predicate(properties[field])) {
            results.error(
                KAFKA_BINDING_FIELD_TYPE,
                "$path Kafka ${binding.location.name.lowercase()} binding '$field' must be $expected.",
                sourceLocation = location(binding, parent, field),
            )
        }
    }

    private fun typeError(
        binding: ProtocolBinding,
        field: String,
        expected: String,
        path: ValidationPath,
        results: ValidationCollector,
    ) {
        results.error(
            KAFKA_BINDING_FIELD_TYPE,
            "$path Kafka ${binding.location.name.lowercase()} binding '$field' must be $expected.",
            sourceLocation = location(binding, field),
        )
    }

    private fun location(
        binding: ProtocolBinding,
        field: String,
    ) = location(binding, null, field)

    private fun location(
        binding: ProtocolBinding,
        parent: String?,
        field: String,
    ) = if (parent == null) {
        asyncApiContext.modelTracking.getSourceLocation(binding, field)
    } else {
        asyncApiContext.modelTracking.getSourceLocation(binding, parent, field)
    }

    private fun isInteger(value: Any?): Boolean = decimal(value)?.stripTrailingZeros()?.scale()?.let { it <= 0 } == true

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
        val MESSAGE_FIELDS =
            setOf(
                "key",
                "schemaIdLocation",
                "schemaIdPayloadEncoding",
                "schemaLookupStrategy",
                "bindingVersion",
            )
        val TOPIC_CONFIGURATION_0_4_FIELDS =
            setOf(
                "cleanup.policy",
                "retention.ms",
                "retention.bytes",
                "delete.retention.ms",
                "max.message.bytes",
            )
        val TOPIC_CONFIGURATION_INTEGER_FIELDS =
            setOf(
                "retention.ms",
                "retention.bytes",
                "delete.retention.ms",
                "max.message.bytes",
            )
        val TOPIC_CONFIGURATION_BOOLEAN_FIELDS =
            setOf(
                "confluent.key.schema.validation",
                "confluent.value.schema.validation",
            )
        val TOPIC_CONFIGURATION_STRING_FIELDS =
            setOf(
                "confluent.key.subject.name.strategy",
                "confluent.value.subject.name.strategy",
            )
        const val CLEANUP_POLICY = "cleanup.policy"
        val CLEANUP_POLICY_VALUES = setOf("delete", "compact")
    }
}
