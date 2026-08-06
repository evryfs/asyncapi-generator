package dev.banking.asyncapi.generator.core.generator.loader

import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema

/** Schema categories collected from an AsyncAPI document for generation. */
data class LoadedSchemas(
    val schemas: Map<String, Schema>,
    val multiFormatSchemas: Map<String, MultiFormatSchema>,
)
