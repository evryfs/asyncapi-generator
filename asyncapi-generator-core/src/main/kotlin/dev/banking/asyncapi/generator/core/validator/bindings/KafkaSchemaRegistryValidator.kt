package dev.banking.asyncapi.generator.core.validator.bindings

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation
import dev.banking.asyncapi.generator.core.model.bindings.ProtocolBinding
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_MESSAGE_SCHEMA_REGISTRY_REQUIRED
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import java.util.Collections
import java.util.IdentityHashMap

/** Validates Kafka constraints that depend on the relationship between channels, messages, and servers. */
internal class KafkaSchemaRegistryValidator(
    private val asyncApiContext: AsyncApiContext,
) {

    fun validate(document: AsyncApiDocument, results: ValidationCollector) {
        val rootServers = document.servers.orEmpty().mapNotNull { (name, server) ->
            resolveServer(server)?.let { ApplicableServer(name, it) }
        }
        val rootServerNames = IdentityHashMap<Server, String>().apply {
            rootServers.forEach { put(it.server, it.name) }
        }

        document.channels.orEmpty().forEach channels@{ (channelName, channelInterface) ->
            val channel = resolveChannel(channelInterface) ?: return@channels
            val applicableServers = applicableServers(channel, rootServers, rootServerNames)
            val missingRegistry = applicableServers
                .filter {
                    it.server.protocol in KAFKA_PROTOCOLS &&
                        schemaRegistryStatus(it.server) == SchemaRegistryStatus.ABSENT
                }
                .map(ApplicableServer::name)
                .distinct()
            if (missingRegistry.isEmpty()) return@channels

            channel.messages.orEmpty().forEach messages@{ (messageName, messageInterface) ->
                val message = resolveMessage(messageInterface) ?: return@messages
                validateMessage(
                    message = message,
                    contextString = "Channel '$channelName' Message '$messageName'",
                    missingRegistry = missingRegistry,
                    results = results,
                )
            }
        }
    }

    private fun applicableServers(
        channel: Channel,
        rootServers: List<ApplicableServer>,
        rootServerNames: IdentityHashMap<Server, String>,
    ): List<ApplicableServer> {
        val selectedServers = channel.servers
        if (selectedServers.isNullOrEmpty()) return rootServers

        return selectedServers.mapNotNull { reference ->
            val server = resolveReference(reference) as? Server ?: return@mapNotNull null
            ApplicableServer(rootServerNames[server] ?: reference.ref, server)
        }
    }

    private fun validateMessage(
        message: Message,
        contextString: String,
        missingRegistry: List<String>,
        results: ValidationCollector,
    ) {
        val effectiveFields = linkedMapOf<String, ProtocolBinding>()
        kafkaMessageBindings(message).forEach bindings@{ binding ->
            val properties = binding.content as? Map<*, *> ?: return@bindings
            SCHEMA_REGISTRY_FIELDS.forEach { field ->
                if (properties.containsKey(field)) {
                    effectiveFields.putIfAbsent(field, binding)
                }
            }
        }

        effectiveFields.forEach { (field, binding) ->
            results.error(
                KAFKA_MESSAGE_SCHEMA_REGISTRY_REQUIRED,
                "$contextString Kafka binding '$field' requires 'schemaRegistryUrl' on every applicable " +
                    "Kafka server; missing on ${missingRegistry.joinToString()}.",
                sourceLocation = asyncApiContext.getSourceLocation(binding, field)
                    ?: asyncApiContext.getSourceLocation(binding),
            )
        }
    }

    private fun kafkaMessageBindings(message: Message): Sequence<ProtocolBinding> = sequence {
        kafkaMessageBinding(message.bindings?.get(KAFKA_BINDING))?.let { yield(it) }
        message.traits.orEmpty().forEach traits@{ traitInterface ->
            val trait = resolveMessageTrait(traitInterface) ?: return@traits
            kafkaMessageBinding(trait.bindings?.get(KAFKA_BINDING))?.let { yield(it) }
        }
    }

    private fun kafkaMessageBinding(bindingInterface: BindingInterface?): ProtocolBinding? =
        resolveBinding(bindingInterface)
            ?.protocolBindings
            ?.firstOrNull { it.protocol == KAFKA_BINDING && it.location == BindingLocation.MESSAGE }

    private fun schemaRegistryStatus(server: Server): SchemaRegistryStatus {
        val bindingInterface = server.bindings?.get(KAFKA_BINDING)
            ?: return SchemaRegistryStatus.ABSENT
        val binding = resolveBinding(bindingInterface)
            ?: return SchemaRegistryStatus.UNKNOWN
        val kafkaBinding = binding
            .protocolBindings
            ?.firstOrNull { it.protocol == KAFKA_BINDING && it.location == BindingLocation.SERVER }
            ?: return SchemaRegistryStatus.UNKNOWN
        val properties = kafkaBinding.content as? Map<*, *>
            ?: return SchemaRegistryStatus.UNKNOWN
        return if (properties.containsKey(SCHEMA_REGISTRY_URL)) {
            SchemaRegistryStatus.PRESENT
        } else {
            SchemaRegistryStatus.ABSENT
        }
    }

    private fun resolveChannel(channel: ChannelInterface): Channel? =
        when (channel) {
            is ChannelInterface.ChannelInline -> channel.channel
            is ChannelInterface.ChannelReference -> resolveReference(channel.reference) as? Channel
        }

    private fun resolveMessage(message: MessageInterface): Message? =
        when (message) {
            is MessageInterface.MessageInline -> message.message
            is MessageInterface.MessageReference -> resolveReference(message.reference) as? Message
        }

    private fun resolveMessageTrait(trait: MessageTraitInterface): MessageTrait? =
        when (trait) {
            is MessageTraitInterface.InlineMessageTrait -> trait.trait
            is MessageTraitInterface.ReferenceMessageTrait -> resolveReference(trait.reference) as? MessageTrait
        }

    private fun resolveServer(server: ServerInterface): Server? =
        when (server) {
            is ServerInterface.ServerInline -> server.server
            is ServerInterface.ServerReference -> resolveReference(server.reference) as? Server
        }

    private fun resolveBinding(binding: BindingInterface?): Binding? =
        when (binding) {
            is BindingInterface.BindingInline -> binding.binding
            is BindingInterface.BindingReference -> resolveReference(binding.reference) as? Binding
            null -> null
        }

    private fun resolveReference(reference: Reference): Any? {
        val visited = Collections.newSetFromMap(IdentityHashMap<Reference, Boolean>())
        var target: Any? = reference
        while (target is Reference && visited.add(target)) {
            target = target.model ?: asyncApiContext.findReference(target)
        }
        return target?.takeUnless { it is Reference }
    }

    private data class ApplicableServer(
        val name: String,
        val server: Server,
    )

    private enum class SchemaRegistryStatus {
        PRESENT,
        ABSENT,
        UNKNOWN,
    }

    private companion object {
        const val KAFKA_BINDING = "kafka"
        const val SCHEMA_REGISTRY_URL = "schemaRegistryUrl"
        val KAFKA_PROTOCOLS = setOf("kafka", "kafka-secure")
        val SCHEMA_REGISTRY_FIELDS = setOf(
            "schemaIdLocation",
            "schemaIdPayloadEncoding",
            "schemaLookupStrategy",
        )
    }
}
