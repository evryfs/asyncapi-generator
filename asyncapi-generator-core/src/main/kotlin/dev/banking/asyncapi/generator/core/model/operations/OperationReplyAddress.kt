package dev.banking.asyncapi.generator.core.model.operations

import com.fasterxml.jackson.annotation.JsonAnyGetter

data class OperationReplyAddress(
    val location: String,
    val description: String? = null,
    @get:JsonAnyGetter val extensions: Map<String, Any?>? = null,
)
