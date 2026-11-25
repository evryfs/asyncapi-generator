package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.ir.ModelTypeMapper
import dev.banking.asyncapi.generator.core.generator.ir.TypeRef
import dev.banking.asyncapi.generator.core.generator.ir.imports
import dev.banking.asyncapi.generator.core.generator.java.JavaModelEmitter
import dev.banking.asyncapi.generator.core.generator.kotlin.KotlinModelEmitter
import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.schema.isOpenPayload
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType

/**
 * Prepares message payload contracts shared by Java and Kotlin Spring Kafka generation.
 *
 * Language-specific rendering remains responsible for syntax, while payload identity,
 * model imports, headers, and native-schema resolution are derived once here.
 */
internal class KafkaPayloadFactory(
    private val modelPackage: String,
    private val nativeKafkaPayloadResolver: NativeKafkaPayloadResolver = NativeKafkaPayloadResolver(),
) {
    fun create(channel: AnalyzedChannel): List<KafkaPayload> =
        channel.messages.mapNotNull { message ->
            when (val payload = message.nativePayload) {
                null -> create(message)
                else ->
                    nativeKafkaPayloadResolver.resolve(message, payload)?.copy(
                        headerProperties = KafkaHeaderPropertyFactory.create(message.headers, message.messageName),
                    )
            }
        }

    private val typeMapper = ModelTypeMapper(emptyMap())

    private fun create(message: AnalyzedMessage): KafkaPayload {
        val javaTypeName = resolveJavaTypeName(message)
        val kotlinTypeName = resolveKotlinTypeName(message)
        val collectionType = collectionType(message)
        return KafkaPayload(
            messageName = message.messageName,
            javaTypeName = javaTypeName,
            kotlinTypeName = kotlinTypeName,
            payloadDescription = message.schema?.description,
            javaImportName = modelImport(javaTypeName, JAVA_BUILT_IN_TYPES).takeIf { collectionType == null },
            kotlinImportName = modelImport(kotlinTypeName, KOTLIN_BUILT_IN_TYPES).takeIf { collectionType == null },
            javaImports = collectionType?.imports(modelPackage, "", listImport = "java.util.List").orEmpty(),
            kotlinImports = collectionType?.imports(modelPackage, "").orEmpty(),
            kafkaKey = message.kafkaKey,
            headerProperties = KafkaHeaderPropertyFactory.create(message.headers, message.messageName),
        )
    }

    /** The list type of an array payload, which has no generated model of its own; null for any other payload. */
    private fun collectionType(message: AnalyzedMessage): TypeRef? =
        message.schema
            ?.takeIf { it.type.getPrimaryType() == "array" }
            ?.let { typeMapper.map(message.messageName, it) }

    private fun resolveJavaTypeName(message: AnalyzedMessage): String? {
        val schema = message.schema ?: return null
        if (schema.isOpenPayload()) return "Object"
        collectionType(message)?.let { return JavaModelEmitter.typeName(it) }

        return when (schema.type.getPrimaryType()) {
            "string" -> "String"
            "integer" -> JavaModelEmitter.typeName(typeMapper.map(message.messageName, schema))
            "number" -> "java.math.BigDecimal"
            "boolean" -> "Boolean"
            else -> message.payload?.typeName
        }
    }

    private fun resolveKotlinTypeName(message: AnalyzedMessage): String? {
        val schema = message.schema ?: return null
        collectionType(message)?.let { return KotlinModelEmitter.typeName(it) }
        return when (schema.type.getPrimaryType()) {
            "string" -> "String"
            "integer" -> KotlinModelEmitter.typeName(typeMapper.map(message.messageName, schema))
            "number" -> "java.math.BigDecimal"
            "boolean" -> "Boolean"
            else -> message.payload?.typeName
        }
    }

    private fun modelImport(
        typeName: String?,
        builtInTypes: Set<String>,
    ): String? =
        typeName
            ?.takeUnless { type -> type in builtInTypes }
            ?.let { type -> "$modelPackage.$type" }

    private companion object {
        val JAVA_BUILT_IN_TYPES =
            setOf("String", "Integer", "Long", "Boolean", "Double", "java.math.BigDecimal", "Object")
        val KOTLIN_BUILT_IN_TYPES =
            setOf("String", "Int", "Long", "Boolean", "java.math.BigDecimal")
    }
}
