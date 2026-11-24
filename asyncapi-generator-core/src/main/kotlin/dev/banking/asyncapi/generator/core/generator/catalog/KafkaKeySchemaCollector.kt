package dev.banking.asyncapi.generator.core.generator.catalog

import dev.banking.asyncapi.generator.core.generator.catalog.MessageSchemaCollector.CollectedMessage
import dev.banking.asyncapi.generator.core.generator.messaging.MessageNameResolver
import dev.banking.asyncapi.generator.core.generator.messaging.kafka.KafkaKey
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Declares a source model for the object Kafka key of a message.
 *
 * Kafka keys are messaging knowledge; the schema catalog only collects their models.
 */
internal object KafkaKeySchemaCollector {
    /** Declares the Kafka key model of [message], when its key schema needs one. */
    fun collect(
        collectedMessage: CollectedMessage,
        message: Message,
        declarations: SchemaDeclarations,
    ) {
        val kafkaKey = KafkaKey.of(MessageNameResolver.resolve(message, collectedMessage.messageKey), message) ?: return
        val modelName = kafkaKey.modelName ?: return

        declarations.declareSourceModel(
            generatedName = modelName,
            schema = kafkaKey.requireSchema(),
            identity = kafkaKeyDeclarationIdentity(collectedMessage, kafkaKey.declaredSchema),
            origin = kafkaKeyOrigin(collectedMessage.origin, kafkaKey.declaredSchema),
        )
    }

    private fun kafkaKeyDeclarationIdentity(
        collectedMessage: CollectedMessage,
        keySchema: SchemaInterface,
    ): ContractDeclarationIdentity? {
        val keyReference = (keySchema as? SchemaInterface.SchemaReference)?.reference
        if (keyReference != null) return MessageSchemaCollector.referencedSchemaIdentity(keyReference, collectedMessage)
        return when (val messageIdentity = collectedMessage.payloadIdentity) {
            is ContractDeclarationIdentity.ComponentMessagePayload ->
                ContractDeclarationIdentity.ComponentMessageKafkaKey(messageIdentity.name)
            is ContractDeclarationIdentity.ReferencedMessagePayload ->
                ContractDeclarationIdentity.ReferencedMessageKafkaKey(messageIdentity.sourceId, messageIdentity.ref)
            else -> null
        }
    }

    private fun kafkaKeyOrigin(
        messageOrigin: String,
        keySchema: SchemaInterface,
    ): String =
        when (keySchema) {
            is SchemaInterface.SchemaReference ->
                "$messageOrigin.bindings.kafka.key (reference '${keySchema.reference.ref}')"
            else -> "$messageOrigin.bindings.kafka.key"
        }
}
