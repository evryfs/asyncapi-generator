package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.schemas.Schema

/** Maps supported scalar AsyncAPI schemas to Java and Kotlin source types. */
internal object KafkaScalarTypeResolver {
    fun resolve(schema: Schema): KafkaScalarType? =
        when (schema.type.getPrimaryType()) {
            "string" -> resolveString(schema)
            "integer" -> resolveInteger(schema)
            "number" -> resolveNumber(schema)
            "boolean" -> KafkaScalarType(javaTypeName = "Boolean", kotlinTypeName = "Boolean")
            else -> null
        }

    private fun resolveString(schema: Schema): KafkaScalarType =
        when (schema.format?.lowercase()) {
            "uuid" -> KafkaScalarType("UUID", "UUID", "java.util.UUID")
            "date-time" -> KafkaScalarType("OffsetDateTime", "OffsetDateTime", "java.time.OffsetDateTime")
            "date" -> KafkaScalarType("LocalDate", "LocalDate", "java.time.LocalDate")
            "time" -> KafkaScalarType("LocalTime", "LocalTime", "java.time.LocalTime")
            else -> KafkaScalarType(javaTypeName = "String", kotlinTypeName = "String")
        }

    private fun resolveInteger(schema: Schema): KafkaScalarType {
        if (schema.format?.lowercase() == "int64") {
            return KafkaScalarType("Long", "Long")
        }
        if (schema.format?.lowercase() == "int32") {
            return KafkaScalarType("Integer", "Int")
        }

        val fitsInt =
            (schema.minimum == null || schema.minimum.toDouble() >= Int.MIN_VALUE.toDouble()) &&
                (schema.maximum == null || schema.maximum.toDouble() <= Int.MAX_VALUE.toDouble())

        return if (fitsInt) {
            KafkaScalarType("Integer", "Int")
        } else {
            KafkaScalarType("Long", "Long")
        }
    }

    private fun resolveNumber(schema: Schema): KafkaScalarType {
        if (schema.multipleOf != null) {
            return KafkaScalarType("BigDecimal", "BigDecimal", "java.math.BigDecimal")
        }
        return when (schema.format?.lowercase()) {
            "float" -> KafkaScalarType("Float", "Float")
            else -> KafkaScalarType("Double", "Double")
        }
    }
}

internal data class KafkaScalarType(
    val javaTypeName: String,
    val kotlinTypeName: String,
    val importName: String? = null,
)
