package dev.banking.asyncapi.generator.core.model.servers

import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface

data class Server(
    val host: String,
    val protocol: String,
    val protocolVersion: String? = null,
    val pathName: String? = null,
    val description: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val variables: Map<String, ServerVariableInterface>? = null,
    val security: List<SecuritySchemeInterface>? = null,
    val tags: List<TagInterface>? = null,
    val externalDocs: ExternalDocInterface? = null,
    val bindings: Map<String, BindingInterface>? = null,
)
