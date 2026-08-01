package dev.banking.asyncapi.generator.core.reader

import java.math.BigDecimal
import java.math.BigInteger

/** Produces the same runtime number representation from YAML and JSON lexemes. */
internal object DocumentNumberParser {
    private val minInt = BigInteger.valueOf(Int.MIN_VALUE.toLong())
    private val maxInt = BigInteger.valueOf(Int.MAX_VALUE.toLong())
    private val minLong = BigInteger.valueOf(Long.MIN_VALUE)
    private val maxLong = BigInteger.valueOf(Long.MAX_VALUE)

    fun parseInteger(value: String): Number? {
        val integer = value.withoutSeparators().toBigIntegerOrNull() ?: return null
        return when (integer) {
            in minInt..maxInt -> integer.toInt()
            in minLong..maxLong -> integer.toLong()
            else -> integer
        }
    }

    fun parseDecimal(value: String): Number? {
        val normalized = value.withoutSeparators()
        val decimal = normalized.toBigDecimalOrNull() ?: return null
        val double = normalized.toDoubleOrNull()
        return if (
            double != null &&
            double.isFinite() &&
            BigDecimal.valueOf(double).compareTo(decimal) == 0
        ) {
            double
        } else {
            decimal.stripTrailingZeros()
        }
    }

    private fun String.withoutSeparators(): String = replace("_", "")
}
