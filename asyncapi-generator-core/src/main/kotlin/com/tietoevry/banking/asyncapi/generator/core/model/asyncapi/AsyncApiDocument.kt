package com.tietoevry.banking.asyncapi.generator.core.model.asyncapi

import com.tietoevry.banking.asyncapi.generator.core.model.channels.ChannelInterface
import com.tietoevry.banking.asyncapi.generator.core.model.components.ComponentInterface
import com.tietoevry.banking.asyncapi.generator.core.model.info.Info
import com.tietoevry.banking.asyncapi.generator.core.model.operations.OperationInterface
import com.tietoevry.banking.asyncapi.generator.core.model.servers.ServerInterface
import com.tietoevry.banking.asyncapi.generator.core.model.tags.TagInterface

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
