package dev.banking.asyncapi.generator.core.validator.schemas

import java.math.BigDecimal
import java.math.BigInteger

/** Exact JSON-compatible value operations shared by Schema Object rules. */
internal object SchemaValueSemantics {

    fun decimal(value: Number): BigDecimal? =
        when (value) {
            is BigDecimal -> value
            is BigInteger -> BigDecimal(value)
            is Byte, is Short, is Int, is Long -> BigDecimal.valueOf(value.toLong())
            is Float -> value.takeIf(Float::isFinite)?.toString()?.toBigDecimalOrNull()
            is Double -> value.takeIf(Double::isFinite)?.let(BigDecimal::valueOf)
            else -> value.toString().toBigDecimalOrNull()
        }

    fun hasDuplicates(values: List<Any?>): Boolean =
        values.indices.any { index ->
            (index + 1 until values.size).any { otherIndex ->
                equal(values[index], values[otherIndex])
            }
        }

    fun isCompatible(value: Any?, type: Any?): Boolean {
        if (type == null) return true
        if (type is List<*>) {
            val declaredTypes = type.filterIsInstance<String>()
            return declaredTypes.isEmpty() || declaredTypes.any { isCompatible(value, it) }
        }
        return when (type) {
            "string" -> value is String
            "number" -> value is Number
            "integer" -> value is Number && decimal(value)?.stripTrailingZeros()?.scale()?.let { it <= 0 } == true
            "boolean" -> value is Boolean
            "array" -> value is List<*>
            "object" -> value is Map<*, *>
            "null" -> value == null
            else -> true
        }
    }

    private fun equal(left: Any?, right: Any?): Boolean =
        when {
            left is Number && right is Number -> {
                val leftDecimal = decimal(left)
                val rightDecimal = decimal(right)
                leftDecimal != null && rightDecimal != null && leftDecimal.compareTo(rightDecimal) == 0
            }

            left is List<*> && right is List<*> ->
                left.size == right.size && left.indices.all { equal(left[it], right[it]) }

            left is Map<*, *> && right is Map<*, *> ->
                left.keys == right.keys && left.keys.all { key -> equal(left[key], right[key]) }

            else -> left == right
        }
}
