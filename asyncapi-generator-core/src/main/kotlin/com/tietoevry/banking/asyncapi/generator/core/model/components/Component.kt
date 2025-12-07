package com.tietoevry.banking.asyncapi.generator.core.model.components

import com.tietoevry.banking.asyncapi.generator.core.model.channels.ChannelInterface
import com.tietoevry.banking.asyncapi.generator.core.model.messages.MessageInterface
import com.tietoevry.banking.asyncapi.generator.core.model.bindings.BindingInterface
import com.tietoevry.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import com.tietoevry.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import com.tietoevry.banking.asyncapi.generator.core.model.tags.TagInterface
import com.tietoevry.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import com.tietoevry.banking.asyncapi.generator.core.model.operations.OperationInterface
import com.tietoevry.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import com.tietoevry.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import com.tietoevry.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import com.tietoevry.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import com.tietoevry.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import com.tietoevry.banking.asyncapi.generator.core.model.servers.ServerInterface
import com.tietoevry.banking.asyncapi.generator.core.model.servers.ServerVariableInterface

data class Component(
    val schemas: Map<String, SchemaInterface>? = null,
    val servers: Map<String, ServerInterface>? = null,
    val channels: Map<String, ChannelInterface>? = null,
    val operations: Map<String, OperationInterface>? = null,
    val messages: Map<String, MessageInterface>? = null,
    val securitySchemes: Map<String, SecuritySchemeInterface>? = null,
    val serverVariables: Map<String, ServerVariableInterface>? = null,
    val parameters: Map<String, ParameterInterface>? = null,
    val correlationIds: Map<String, CorrelationIdInterface>? = null,
    val replies: Map<String, OperationReplyInterface>? = null,
    val replyAddresses: Map<String, OperationReplyAddressInterface>? = null,
    val externalDocs: Map<String, ExternalDocInterface>? = null,
    val tags: Map<String, TagInterface>? = null,
    val operationTraits: Map<String, OperationTraitInterface>? = null,
    val messageTraits: Map<String, MessageTraitInterface>? = null,
    val serverBindings: Map<String, BindingInterface>? = null,
    val channelBindings: Map<String, BindingInterface>? = null,
    val operationBindings: Map<String, BindingInterface>? = null,
    val messageBindings: Map<String, BindingInterface>? = null,
)
