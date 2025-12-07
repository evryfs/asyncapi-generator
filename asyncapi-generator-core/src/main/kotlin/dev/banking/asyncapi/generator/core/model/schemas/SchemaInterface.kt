package dev.banking.asyncapi.generator.core.model.schemas

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.annotation.JsonValue
import dev.banking.asyncapi.generator.core.model.references.Reference

sealed interface SchemaInterface {

    data class SchemaInline(
        @get:JsonUnwrapped
        val schema: Schema,
    ) : SchemaInterface

    data class MultiFormatSchemaInline(
        @get:JsonUnwrapped
        val multiFormatSchema: MultiFormatSchema,
    ) : SchemaInterface

    data class SchemaReference(
        @get:JsonUnwrapped
        val reference: Reference,
    ) : SchemaInterface

    data class BooleanSchema(
        @get:JsonValue
        val value: Boolean,
    ) : SchemaInterface
}
