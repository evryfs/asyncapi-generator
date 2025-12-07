package dev.banking.asyncapi.generator.core.model.asyncapi

import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface

data class AsyncApiDocument(
    val asyncapi: String,
    val id: String? = null,
    val info: Info,
    val servers: Map<String, ServerInterface>? = null,
    val defaultContentType: String? = null,
    val channels: Map<String, ChannelInterface>? = null,
    val operations: Map<String, OperationInterface>? = null,
    val components: ComponentInterface? = null,
)
