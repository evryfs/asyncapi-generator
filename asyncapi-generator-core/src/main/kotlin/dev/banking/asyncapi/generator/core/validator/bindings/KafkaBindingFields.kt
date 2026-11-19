package dev.banking.asyncapi.generator.core.validator.bindings

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.ProtocolBinding
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_BINDING_FIELD
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_BINDING_FIELD_TYPE
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_CHANNEL_POSITIVE_INTEGER
import dev.banking.asyncapi.generator.core.source.SourceLocation
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import java.math.BigDecimal
import java.math.BigInteger

/**
 * The fields of a Kafka binding, or of an object nested in it, together with the checks every Kafka binding
 * location shares.
 *
 * @param asyncApiContext the load the binding belongs to
 * @param binding the Kafka binding being checked
 * @param properties the fields to check: the binding's own, or those of the object nested at [parent]
 * @param path the binding as findings name it
 * @param results the collector receiving the findings
 * @param parent the name of the binding field holding [properties], or `null` when they are the binding's own
 */
internal class KafkaBindingFields(
    private val asyncApiContext: AsyncApiContext,
    private val binding: ProtocolBinding,
    val properties: Map<String, Any?>,
    private val path: ValidationPath,
    private val results: ValidationCollector,
    private val parent: String? = null,
) {
    /** The fields of the object nested in this binding at [parent], which are [properties]. */
    fun nested(
        parent: String,
        properties: Map<String, Any?>,
    ) = KafkaBindingFields(asyncApiContext, binding, properties, path, results, parent)

    /** Reports every field that is not in [allowedFields] and is not an `x-` extension. */
    fun checkAllowed(allowedFields: Set<String>) {
        properties.keys
            .filterNot { it in allowedFields || it.startsWith("x-") }
            .forEach { field ->
                results.error(
                    KAFKA_BINDING_FIELD,
                    "$path Kafka ${binding.location.name.lowercase()} binding does not define property '$field'.",
                    sourceLocation = location(field),
                )
            }
    }

    /** Reports [field] when it is present and is not a string. */
    fun checkString(field: String) = checkType(field, "a string") { it is String }

    /** Reports [field] when it is present and does not satisfy [predicate]; [expected] names what it should be. */
    fun checkType(
        field: String,
        expected: String,
        predicate: (Any?) -> Boolean,
    ) {
        if (properties.containsKey(field) && !predicate(properties[field])) typeError(field, expected)
    }

    /** Reports [field] when it is present and is not a positive integer. */
    fun checkPositiveInteger(field: String) {
        if (properties.containsKey(field) && !isPositiveInteger(properties[field])) {
            results.error(
                KAFKA_CHANNEL_POSITIVE_INTEGER,
                "$path Kafka channel binding '$field' must be a positive integer.",
                sourceLocation = location(field),
            )
        }
    }

    /** Reports [field] when it is present but was not parsed as a Schema Object or Reference Object. */
    fun checkSchemaField(field: String) {
        if (properties.containsKey(field) && field !in binding.schemaFields) {
            typeError(field, "a Schema Object or Reference Object")
        }
    }

    /** Reports [field] as not being [expected]. */
    fun typeError(
        field: String,
        expected: String,
    ) {
        results.error(
            KAFKA_BINDING_FIELD_TYPE,
            "$path Kafka ${binding.location.name.lowercase()} binding '$field' must be $expected.",
            sourceLocation = location(field),
        )
    }

    /** Where [field] is written in the source. */
    fun location(field: String): SourceLocation? =
        if (parent == null) {
            asyncApiContext.modelTracking.getSourceLocation(binding, field)
        } else {
            asyncApiContext.modelTracking.getSourceLocation(binding, parent, field)
        }

    /** Reports an error of [rule] with [message], located at [field] of the binding. */
    fun error(
        rule: ValidationRule,
        message: String,
        field: String,
    ) = results.error(rule, "$path $message", sourceLocation = location(field))

    internal companion object {
        /** Whether [value] is a number without a fractional part. */
        fun isInteger(value: Any?): Boolean = decimal(value)?.stripTrailingZeros()?.scale()?.let { it <= 0 } == true

        private fun isPositiveInteger(value: Any?): Boolean = isInteger(value) && decimal(value)?.signum() == 1

        private fun decimal(value: Any?): BigDecimal? =
            when (value) {
                is BigDecimal -> value
                is BigInteger -> value.toBigDecimal()
                is Byte, is Short, is Int, is Long -> BigDecimal(value.toString())
                is Float -> value.takeIf(Float::isFinite)?.toString()?.toBigDecimalOrNull()
                is Double -> value.takeIf(Double::isFinite)?.toString()?.toBigDecimalOrNull()
                else -> null
            }
    }
}
