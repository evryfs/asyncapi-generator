package dev.banking.asyncapi.generator.core.model.tags

import com.fasterxml.jackson.annotation.JsonAnyGetter
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface

internal data class Tag(
    val name: String,
    val description: String? = null,
    val externalDocs: ExternalDocInterface? = null,
    @get:JsonAnyGetter val extensions: Map<String, Any?>? = null,
)
