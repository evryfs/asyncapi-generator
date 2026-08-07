package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.analyzer.AnalyzedMessage
import dev.banking.asyncapi.generator.core.generator.schema.isOpenPayload
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType

/**
 * Prepares message payload contracts shared by Java and Kotlin Spring Kafka generation.
 *
 * Language-specific rendering remains responsible for syntax, while payload identity,
 * model imports, keys, headers, and native-schema resolution are derived once here.
 */
internal class KafkaPayloadFactory(
    private val modelPackage: String,
    private val nativeKafkaPayloadResolver: NativeKafkaPayloadResolver = NativeKafkaPayloadResolver(),
) {
    fun create(channel: AnalyzedChannel): List<KafkaPayload> =
        channel.messages.map(::create) +
            channel.multiFormatMessages.mapNotNull { message ->
                nativeKafkaPayloadResolver.resolve(message)?.copy(
                    headerProperties = KafkaHeaderPropertyFactory.create(message.headers, message.messageName),
                )
            }

    private fun create(message: AnalyzedMessage): KafkaPayload {
        val javaTypeName = resolveJavaTypeName(message)
        val kotlinTypeName = resolveKotlinTypeName(message)
        return KafkaPayload(
            messageName = message.messageName,
            javaTypeName = javaTypeName,
            kotlinTypeName = kotlinTypeName,
            payloadDescription = message.schema?.description,
            javaImportName = modelImport(javaTypeName, JAVA_BUILT_IN_TYPES),
            kotlinImportName = modelImport(kotlinTypeName, KOTLIN_BUILT_IN_TYPES),
            keySchema = message.keySchema,
            headerProperties = KafkaHeaderPropertyFactory.create(message.headers, message.messageName),
        )
    }

    private fun resolveJavaTypeName(message: AnalyzedMessage): String? {
        val schema = message.schema ?: return null
        if (schema.isOpenPayload()) return "Object"

        return when (schema.type.getPrimaryType()) {
            "string" -> "String"
            "integer" -> "Integer"
            "number" -> "java.math.BigDecimal"
            "boolean" -> "Boolean"
            else -> message.payloadTypeName
        }
    }

    private fun resolveKotlinTypeName(message: AnalyzedMessage): String? {
        val schema = message.schema ?: return null
        return when (schema.type.getPrimaryType()) {
            "string" -> "String"
            "integer" -> "Int"
            "number" -> "java.math.BigDecimal"
            "boolean" -> "Boolean"
            else -> message.payloadTypeName
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
