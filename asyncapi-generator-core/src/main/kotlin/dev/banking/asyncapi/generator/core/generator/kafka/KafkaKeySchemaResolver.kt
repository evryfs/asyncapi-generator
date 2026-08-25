package dev.banking.asyncapi.generator.core.generator.kafka

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil.getPrimaryType
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedKafkaKeySchema
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Resolves Kafka binding key schemas independently of generated client language.
 */
internal object KafkaKeySchemaResolver {
    fun resolve(
        messageName: String,
        schema: SchemaInterface,
    ): ResolvedKafkaKeySchema {
        val resolvedSchema = schema.resolve(messageName)
        return ResolvedKafkaKeySchema(
            schema = resolvedSchema,
            modelName =
                resolvedSchema
                    .takeIf { it.type.getPrimaryType() == "object" }
                    ?.let { objectModelName(messageName, schema, it) },
        )
    }

    fun resolveObjectModelOrNull(
        messageName: String,
        schema: SchemaInterface,
    ): KafkaKeyObjectModel? {
        val resolvedSchema = schema.resolveOrNull() ?: return null
        if (resolvedSchema.type.getPrimaryType() != "object") return null
        return KafkaKeyObjectModel(
            name = objectModelName(messageName, schema, resolvedSchema),
            schema = resolvedSchema,
        )
    }

    private fun SchemaInterface.resolve(messageName: String): Schema =
        resolveOrNull()
            ?: when (this) {
                is SchemaInterface.SchemaReference ->
                    throw UnsupportedKafkaKeySchema(
                        messageName = messageName,
                        schemaType = "unresolved schema reference '${reference.ref}'",
                    )
                is SchemaInterface.BooleanSchema ->
                    throw UnsupportedKafkaKeySchema(
                        messageName = messageName,
                        schemaType = "boolean schema",
                    )
                is SchemaInterface.MultiFormatSchemaInline ->
                    throw UnsupportedKafkaKeySchema(
                        messageName = messageName,
                        schemaType = "multi-format schema '${multiFormatSchema.schemaFormat}'",
                    )
                is SchemaInterface.SchemaInline -> error("Inline schemas always resolve")
            }

    private fun SchemaInterface.resolveOrNull(): Schema? =
        when (this) {
            is SchemaInterface.SchemaInline -> schema
            is SchemaInterface.SchemaReference -> reference.model as? Schema
            is SchemaInterface.BooleanSchema,
            is SchemaInterface.MultiFormatSchemaInline,
            -> null
        }

    private fun objectModelName(
        messageName: String,
        source: SchemaInterface,
        schema: Schema,
    ): String =
        when (source) {
            is SchemaInterface.SchemaReference -> source.reference.ref.modelName()
            is SchemaInterface.SchemaInline ->
                schema.title
                    ?.takeIf(String::isNotBlank)
                    ?.let(MapperUtil::toPascalCase)
                    ?: messageName.withKeySuffix()
            else -> messageName.withKeySuffix()
        }

    private fun String.modelName(): String {
        val fragment = substringAfter('#', missingDelimiterValue = "")
        val rawName =
            if (fragment.isNotBlank()) {
                fragment.substringAfterLast('/')
            } else {
                substringAfterLast('/').substringBeforeLast('.')
            }
        return MapperUtil.toPascalCase(rawName)
    }

    private fun String.withKeySuffix(): String =
        MapperUtil.toPascalCase(this).let { name ->
            if (name.endsWith("Key")) name else "${name}Key"
        }
}

internal data class ResolvedKafkaKeySchema(
    val schema: Schema,
    val modelName: String?,
)

internal data class KafkaKeyObjectModel(
    val name: String,
    val schema: Schema,
)

internal fun Message.kafkaKeySchema(): SchemaInterface? {
    val binding =
        when (val kafkaBinding = bindings?.get("kafka")) {
            is BindingInterface.BindingInline -> kafkaBinding.binding
            is BindingInterface.BindingReference -> kafkaBinding.reference.model as? Binding
            null -> null
        } ?: return null

    return binding.kafkaKeySchema
}
