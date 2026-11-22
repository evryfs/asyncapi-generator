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
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.KAFKA_MESSAGE_SCHEMA_REGISTRY_URL
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import java.util.IdentityHashMap

/** Validates Kafka constraints that depend on the relationship between channels, messages, and servers. */
internal class KafkaSchemaRegistryValidator(
    private val asyncApiContext: AsyncApiContext,
) {
    fun validate(
        document: AsyncApiDocument,
        results: ValidationCollector,
    ) {
        val rootServers =
            document.servers.orEmpty().mapNotNull { (name, server) ->
                server.resolvedOrNull()?.let { ApplicableServer(name, it) }
            }
        val rootServerNames =
            IdentityHashMap<Server, String>().apply {
                rootServers.forEach { put(it.server, it.name) }
            }

        document.channels.orEmpty().forEach channels@{ (channelName, channelInterface) ->
            val channel = channelInterface.resolvedOrNull() ?: return@channels
            val applicableServers = applicableServers(channel, rootServers, rootServerNames)
            val missingRegistry =
                applicableServers
                    .filter {
                        it.server.protocol in KAFKA_PROTOCOLS &&
                            schemaRegistryStatus(it.server) == SchemaRegistryStatus.ABSENT
                    }.map(ApplicableServer::name)
                    .distinct()
            if (missingRegistry.isEmpty()) return@channels

            channel.messages.orEmpty().forEach messages@{ (messageName, messageInterface) ->
                val message = messageInterface.resolvedOrNull() ?: return@messages
                validateMessage(
                    message = message,
                    path = ValidationPath.named("Channel", channelName).named("Message", messageName),
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
            val server = reference.finalModel() as? Server ?: return@mapNotNull null
            ApplicableServer(rootServerNames[server] ?: reference.ref, server)
        }
    }

    private fun validateMessage(
        message: Message,
        path: ValidationPath,
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
                KAFKA_MESSAGE_SCHEMA_REGISTRY_URL,
                "$path Kafka binding '$field' requires 'schemaRegistryUrl' on every applicable " +
                    "Kafka server; missing on ${missingRegistry.joinToString()}.",
                sourceLocation =
                    asyncApiContext.modelTracking.getSourceLocation(binding, field)
                        ?: asyncApiContext.modelTracking.getSourceLocation(binding),
            )
        }
    }

    private fun kafkaMessageBindings(message: Message): Sequence<ProtocolBinding> =
        sequence {
            kafkaMessageBinding(message.bindings?.get(KAFKA_BINDING))?.let { yield(it) }
            message.traits.orEmpty().forEach traits@{ traitInterface ->
                val trait = traitInterface.resolvedOrNull() ?: return@traits
                kafkaMessageBinding(trait.bindings?.get(KAFKA_BINDING))?.let { yield(it) }
            }
        }

    private fun kafkaMessageBinding(bindingInterface: BindingInterface?): ProtocolBinding? =
        bindingInterface
            ?.resolvedOrNull()
            ?.protocolBindings
            ?.firstOrNull { it.protocol == KAFKA_BINDING && it.location == BindingLocation.MESSAGE }

    private fun schemaRegistryStatus(server: Server): SchemaRegistryStatus {
        val bindingInterface =
            server.bindings?.get(KAFKA_BINDING)
                ?: return SchemaRegistryStatus.ABSENT
        val binding =
            bindingInterface?.resolvedOrNull()
                ?: return SchemaRegistryStatus.UNKNOWN
        val kafkaBinding =
            binding
                .protocolBindings
                ?.firstOrNull { it.protocol == KAFKA_BINDING && it.location == BindingLocation.SERVER }
                ?: return SchemaRegistryStatus.UNKNOWN
        val properties =
            kafkaBinding.content as? Map<*, *>
                ?: return SchemaRegistryStatus.UNKNOWN
        return if (properties.containsKey(SCHEMA_REGISTRY_URL)) {
            SchemaRegistryStatus.PRESENT
        } else {
            SchemaRegistryStatus.ABSENT
        }
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
        val SCHEMA_REGISTRY_FIELDS =
            setOf(
                "schemaIdLocation",
                "schemaIdPayloadEncoding",
                "schemaLookupStrategy",
            )
    }
}
