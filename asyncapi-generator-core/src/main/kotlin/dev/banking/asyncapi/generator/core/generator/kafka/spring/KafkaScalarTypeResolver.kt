package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.ir.ModelTypeMapper
import dev.banking.asyncapi.generator.core.generator.ir.importName
import dev.banking.asyncapi.generator.core.generator.java.JavaModelEmitter
import dev.banking.asyncapi.generator.core.generator.kotlin.KotlinModelEmitter
import dev.banking.asyncapi.generator.core.model.schemas.Schema

/** Maps supported scalar AsyncAPI schemas to Java and Kotlin source types, as models map them. */
internal object KafkaScalarTypeResolver {
    fun resolve(schema: Schema): KafkaScalarType? =
        ModelTypeMapper.scalarType(schema)?.let { scalar ->
            KafkaScalarType(
                javaTypeName = JavaModelEmitter.typeName(scalar),
                kotlinTypeName = KotlinModelEmitter.typeName(scalar),
                importName = scalar.importName,
            )
        }
}

internal data class KafkaScalarType(
    val javaTypeName: String,
    val kotlinTypeName: String,
    val importName: String? = null,
)
