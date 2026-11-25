package dev.banking.asyncapi.generator.core.generator.messaging.kafka

import dev.banking.asyncapi.generator.core.generator.schema.linkedSchema
import dev.banking.asyncapi.generator.core.generator.sourcemodel.NameCase
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UnsupportedKafkaKeySchema
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * The Kafka record key a message declares in its Kafka binding.
 *
 * An object key has a generated model. Its name is the referenced schema's name, otherwise the schema
 * title, otherwise the message name with a `Key` suffix.
 *
 * @param messageName Source-code name of the message the key belongs to.
 * @param declaredSchema The key schema as the binding declares it.
 * @param schema The key schema, or `null` when the declaration is not a schema a key can use.
 * @param modelName Name of the generated key model, or `null` when the key is not an object.
 */
@ConsistentCopyVisibility
internal data class KafkaKey private constructor(
    val messageName: String,
    val declaredSchema: SchemaInterface,
    val schema: Schema?,
    val modelName: String?,
) {
    /** The key schema; fails when the declaration is not a schema a key can use. */
    fun requireSchema(): Schema =
        schema
            ?: throw UnsupportedKafkaKeySchema(
                messageName = messageName,
                schemaType =
                    when (declaredSchema) {
                        is SchemaInterface.SchemaReference ->
                            "unresolved schema reference '${declaredSchema.reference.ref}'"
                        is SchemaInterface.BooleanSchema -> "boolean schema"
                        is SchemaInterface.MultiFormatSchemaInline ->
                            "multi-format schema '${declaredSchema.multiFormatSchema.schemaFormat}'"
                        is SchemaInterface.SchemaInline -> error("Inline schemas always resolve")
                    },
            )

    companion object {
        /** The Kafka key of [message], named after [messageName]; `null` when the message declares none. */
        fun of(
            messageName: String,
            message: Message,
        ): KafkaKey? = message.kafkaKeySchema()?.let { declaredSchema -> of(messageName, declaredSchema) }

        /** The Kafka key declared as [declaredSchema] for the message named [messageName]. */
        fun of(
            messageName: String,
            declaredSchema: SchemaInterface,
        ): KafkaKey {
            val schema =
                when (declaredSchema) {
                    is SchemaInterface.SchemaInline -> declaredSchema.schema
                    is SchemaInterface.SchemaReference -> declaredSchema.linkedSchema()
                    is SchemaInterface.BooleanSchema,
                    is SchemaInterface.MultiFormatSchemaInline,
                    -> null
                }
            return KafkaKey(
                messageName = messageName,
                declaredSchema = declaredSchema,
                schema = schema,
                modelName =
                    schema
                        ?.takeIf { it.type?.primary == "object" }
                        ?.let { modelName(messageName, declaredSchema, it) },
            )
        }

        private fun modelName(
            messageName: String,
            declaredSchema: SchemaInterface,
            schema: Schema,
        ): String =
            when (declaredSchema) {
                is SchemaInterface.SchemaReference -> declaredSchema.reference.ref.referencedName()
                else ->
                    schema.title
                        ?.takeIf(String::isNotBlank)
                        ?.let(NameCase::toPascalCase)
                        ?: messageName.withKeySuffix()
            }

        private fun String.referencedName(): String {
            val fragment = substringAfter('#', missingDelimiterValue = "")
            val rawName =
                if (fragment.isNotBlank()) {
                    fragment.substringAfterLast('/')
                } else {
                    substringAfterLast('/').substringBeforeLast('.')
                }
            return NameCase.toPascalCase(rawName)
        }

        private fun String.withKeySuffix(): String =
            NameCase.toPascalCase(this).let { name ->
                if (name.endsWith("Key")) name else "${name}Key"
            }
    }
}

/** The key schema of the message's Kafka binding, or `null` when it declares none. */
internal fun Message.kafkaKeySchema(): SchemaInterface? = bindings?.get("kafka")?.resolvedOrNull()?.kafkaKeySchema
