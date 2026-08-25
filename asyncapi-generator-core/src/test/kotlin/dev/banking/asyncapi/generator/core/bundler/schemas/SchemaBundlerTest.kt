package dev.banking.asyncapi.generator.core.bundler.schemas

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SchemaBundlerTest {

    private val bundler = SchemaBundler()

    @Test
    fun `bundle keeps an unvisited component schema reference and bundles its model`() {
        val bindingReference = Reference(
            "#/components/schemaBindings/kafka",
            model = Binding(content = emptyMap()),
        )
        val schema = Schema(
            type = "object",
            bindings = mapOf("kafka" to BindingInterface.BindingReference(bindingReference)),
        )
        val schemaReference = Reference("#/components/schemas/User", model = schema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)

        val bundled = bundler.bundle(schemaInterface, BundlingContext.empty())

        assertSame(schemaInterface, bundled)
        assertTrue(schemaReference.inline)
        assertIs<Schema>(schemaReference.model)
        assertTrue((schemaReference.model as Schema).bindings!!.containsKey("kafka"))
        assertTrue(bindingReference.inline)
    }

    @Test
    fun `bundle inlines an unvisited non-component schema reference`() {
        val bindingReference = Reference(
            "#/components/schemaBindings/kafka",
            model = Binding(content = emptyMap()),
        )
        val schema = Schema(
            type = "object",
            bindings = mapOf("kafka" to BindingInterface.BindingReference(bindingReference)),
        )
        val schemaReference = Reference("schemas.yaml#/shared/User", model = schema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)

        val bundled = bundler.bundle(schemaInterface, BundlingContext.empty())

        assertIs<SchemaInterface.SchemaInline>(bundled)
        assertTrue((bundled as SchemaInterface.SchemaInline).schema.bindings!!.containsKey("kafka"))
        assertFalse(schemaReference.inline)
        assertTrue(bindingReference.inline)
    }

    @Test
    fun `bundle does not promote a recursive root component reference`() {
        val nestedReference = Reference("#/components/schemas/Node")
        val schema = Schema(
            type = "object",
            properties = mapOf("next" to SchemaInterface.SchemaReference(nestedReference)),
        )
        nestedReference.model = schema
        val rootReference = Reference("#/components/schemas/Node", model = schema)
        val schemaInterface = SchemaInterface.SchemaReference(rootReference)
        val context = BundlingContext.empty()

        val bundled = bundler.bundle(schemaInterface, context)

        assertSame(schemaInterface, bundled)
        assertTrue(context.schemaPromotions.schemas().isEmpty())
    }

    @Test
    fun `bundle keeps component multi format schema references without casting to schema`() {
        val schema = nativeAvroSchema()
        val schemaReference = Reference("#/components/schemas/UserCreated", model = schema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)

        val bundled = bundler.bundle(schemaInterface, BundlingContext.empty())

        assertSame(schemaInterface, bundled)
        assertTrue(schemaReference.inline)
        assertSame(schema, schemaReference.model)
    }

    @Test
    fun `bundle inlines non-component multi format schema references`() {
        val schema = nativeAvroSchema()
        val schemaReference = Reference("schemas.yaml#/shared/UserCreated", model = schema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)

        val bundled = bundler.bundle(schemaInterface, BundlingContext.empty())

        assertEquals(SchemaInterface.MultiFormatSchemaInline(schema), bundled)
        assertFalse(schemaReference.inline)
    }

    @Test
    fun `bundle resolves a component reference to a true Boolean schema`() {
        val booleanSchema = SchemaInterface.BooleanSchema(true)
        val schemaReference = Reference("#/components/schemas/Anything", model = booleanSchema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)

        val bundled = bundler.bundle(schemaInterface, BundlingContext.empty())

        assertSame(booleanSchema, bundled)
        assertFalse(schemaReference.inline)
    }

    @Test
    fun `bundle resolves a component reference to a false Boolean schema`() {
        val booleanSchema = SchemaInterface.BooleanSchema(false)
        val schemaReference = Reference("#/components/schemas/Nothing", model = booleanSchema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)

        val bundled = bundler.bundle(schemaInterface, BundlingContext.empty())

        assertSame(booleanSchema, bundled)
        assertEquals(SchemaInterface.BooleanSchema(false), bundled)
        assertFalse(schemaReference.inline)
    }

    @Test
    fun `bundle inlines an already visited external reference resolving to a true Boolean schema`() {
        val booleanSchema = SchemaInterface.BooleanSchema(true)
        val schemaReference = Reference("schemas.yaml#/shared/Anything", model = booleanSchema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)
        val context = BundlingContext.empty().enter(schemaReference)

        val bundled = bundler.bundle(schemaInterface, context)

        assertSame(booleanSchema, bundled)
        assertFalse(schemaReference.inline)
    }

    @Test
    fun `bundle inlines an external reference resolving to a false Boolean schema`() {
        val booleanSchema = SchemaInterface.BooleanSchema(false)
        val schemaReference = Reference("schemas.yaml#/shared/Nothing", model = booleanSchema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)

        val bundled = bundler.bundle(schemaInterface, BundlingContext.empty())

        assertSame(booleanSchema, bundled)
        assertEquals(SchemaInterface.BooleanSchema(false), bundled)
        assertFalse(schemaReference.inline)
    }

    private fun nativeAvroSchema(): MultiFormatSchema =
        MultiFormatSchema(
            schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
            schema =
                mapOf(
                    "type" to "record",
                    "name" to "UserCreated",
                    "namespace" to "com.example.avro",
                    "fields" to emptyList<Any>(),
                ),
        )
}
