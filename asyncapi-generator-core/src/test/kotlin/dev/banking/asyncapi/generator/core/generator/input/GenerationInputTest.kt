package dev.banking.asyncapi.generator.core.generator.input

import dev.banking.asyncapi.generator.core.generator.schema.SchemaDeclarationCatalog
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class GenerationInputTest {

    @Test
    fun `schema context exposes analyzed schemas`() {
        val userSchema = Schema(type = "object")
        val input =
            GenerationInput(
                schemas = mapOf("User" to userSchema),
                polymorphicRelationships = emptyMap(),
                channels = emptyList(),
            )

        assertSame(userSchema, input.schemaContext.findSchemaByName("User"))
    }

    @Test
    fun `schema context with additional schemas includes original and additional schemas`() {
        val userSchema = Schema(type = "object")
        val headerSchema = Schema(type = "object")
        val input =
            GenerationInput(
                schemas = mapOf("User" to userSchema),
                polymorphicRelationships = emptyMap(),
                channels = emptyList(),
            )

        val context = input.schemaContextWith(mapOf("UserHeader" to headerSchema))

        assertSame(userSchema, context.findSchemaByName("User"))
        assertSame(headerSchema, context.findSchemaByName("UserHeader"))
    }

    @Test
    fun `schema declaration catalog backs contract compatibility views`() {
        val declaredSchema = Schema(type = "object")
        val multiFormatSchema =
            MultiFormatSchema(
                schemaFormat = "application/schema+json;version=draft-07",
                schema = mapOf("type" to "object"),
            )
        val input =
            GenerationInput(
                schemas = emptyMap(),
                schemaDeclarations =
                    SchemaDeclarationCatalog(
                        asyncApiSchemas = mapOf("Declared" to declaredSchema),
                        multiFormatSchemas = mapOf("Native" to multiFormatSchema),
                        booleanSchemas = mapOf("Allowed" to true),
                    ),
                polymorphicRelationships = emptyMap(),
                channels = emptyList(),
            )

        assertSame(declaredSchema, input.schemaDeclarations.asyncApiSchemas["Declared"])
        assertSame(multiFormatSchema, input.multiFormatSchemas["Native"])
        assertEquals(mapOf("Allowed" to true), input.schemaDeclarations.booleanSchemas)
    }
}
