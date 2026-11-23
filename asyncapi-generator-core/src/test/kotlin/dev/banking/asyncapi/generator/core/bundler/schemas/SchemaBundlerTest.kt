package dev.banking.asyncapi.generator.core.bundler.schemas

import dev.banking.asyncapi.generator.core.bundler.BundlingRun
import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.BundlingWalk
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
    private val walk = BundlingWalk()

    @Test
    fun `bundle keeps an unvisited component schema reference and bundles its model`() {
        val bindingReference =
            Reference(
                "#/components/schemaBindings/kafka",
                model = Binding(content = emptyMap()),
            )
        val schema =
            Schema(
                type = "object",
                bindings = mapOf("kafka" to BindingInterface.BindingReference(bindingReference)),
            )
        val schemaReference = Reference("#/components/schemas/User", model = schema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)

        val bundled = walk.schemas.bundle(schemaInterface, BundlingRun().rootScope())

        val bundledReference = assertIs<SchemaInterface.SchemaReference>(bundled).reference
        assertFalse(bundledReference.inline, "a component schema reference keeps its \$ref")
        val bundledSchema = assertIs<Schema>(bundledReference.model)
        val bundledBinding = assertIs<BindingInterface.BindingReference>(bundledSchema.bindings!!["kafka"])
        assertTrue(bundledBinding.reference.inline)
        assertFalse(bindingReference.inline, "the input reference is not changed")
        assertSame(schema, schemaReference.model, "the input reference model is not changed")
    }

    @Test
    fun `bundle inlines an unvisited non-component schema reference`() {
        val bindingReference =
            Reference(
                "#/components/schemaBindings/kafka",
                model = Binding(content = emptyMap()),
            )
        val schema =
            Schema(
                type = "object",
                bindings = mapOf("kafka" to BindingInterface.BindingReference(bindingReference)),
            )
        val schemaReference = Reference("schemas.yaml#/shared/User", model = schema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)

        val bundled = walk.schemas.bundle(schemaInterface, BundlingRun().rootScope())

        assertIs<SchemaInterface.SchemaInline>(bundled)
        assertTrue(bundled.schema.bindings!!.containsKey("kafka"))
        assertFalse(schemaReference.inline)
        val bundledBinding = assertIs<BindingInterface.BindingReference>(bundled.schema.bindings!!["kafka"])
        assertTrue(bundledBinding.reference.inline)
        assertFalse(bindingReference.inline, "the input reference is not changed")
    }

    @Test
    fun `bundle does not promote a recursive root component reference`() {
        val nestedReference = Reference("#/components/schemas/Node")
        val schema =
            Schema(
                type = "object",
                properties = mapOf("next" to SchemaInterface.SchemaReference(nestedReference)),
            )
        nestedReference.model = schema
        val rootReference = Reference("#/components/schemas/Node", model = schema)
        val schemaInterface = SchemaInterface.SchemaReference(rootReference)
        val scope = BundlingRun().rootScope()

        val bundled = walk.schemas.bundle(schemaInterface, scope)

        assertEquals(schemaInterface, bundled)
        assertTrue(
            scope.run.schemaPromotions
                .schemas()
                .isEmpty(),
        )
    }

    @Test
    fun `bundle keeps component multi format schema references without casting to schema`() {
        val schema = nativeAvroSchema()
        val schemaReference = Reference("#/components/schemas/UserCreated", model = schema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)

        val bundled = walk.schemas.bundle(schemaInterface, BundlingRun().rootScope())

        assertSame(schemaInterface, bundled)
        assertFalse(schemaReference.inline, "a component schema reference keeps its \$ref")
        assertSame(schema, schemaReference.model)
    }

    @Test
    fun `bundle inlines non-component multi format schema references`() {
        val schema = nativeAvroSchema()
        val schemaReference = Reference("schemas.yaml#/shared/UserCreated", model = schema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)

        val bundled = walk.schemas.bundle(schemaInterface, BundlingRun().rootScope())

        assertEquals(SchemaInterface.MultiFormatSchemaInline(schema), bundled)
        assertFalse(schemaReference.inline)
    }

    @Test
    fun `bundle resolves a component reference to a true Boolean schema`() {
        val booleanSchema = SchemaInterface.BooleanSchema(true)
        val schemaReference = Reference("#/components/schemas/Anything", model = booleanSchema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)

        val bundled = walk.schemas.bundle(schemaInterface, BundlingRun().rootScope())

        assertSame(booleanSchema, bundled)
        assertFalse(schemaReference.inline)
    }

    @Test
    fun `bundle resolves a component reference to a false Boolean schema`() {
        val booleanSchema = SchemaInterface.BooleanSchema(false)
        val schemaReference = Reference("#/components/schemas/Nothing", model = booleanSchema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)

        val bundled = walk.schemas.bundle(schemaInterface, BundlingRun().rootScope())

        assertSame(booleanSchema, bundled)
        assertEquals(SchemaInterface.BooleanSchema(false), bundled)
        assertFalse(schemaReference.inline)
    }

    @Test
    fun `bundle inlines an already visited external reference resolving to a true Boolean schema`() {
        val booleanSchema = SchemaInterface.BooleanSchema(true)
        val schemaReference = Reference("schemas.yaml#/shared/Anything", model = booleanSchema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)
        val scope = BundlingRun().rootScope().enter(schemaReference)

        val bundled = walk.schemas.bundle(schemaInterface, scope)

        assertSame(booleanSchema, bundled)
        assertFalse(schemaReference.inline)
    }

    @Test
    fun `bundle inlines an external reference resolving to a false Boolean schema`() {
        val booleanSchema = SchemaInterface.BooleanSchema(false)
        val schemaReference = Reference("schemas.yaml#/shared/Nothing", model = booleanSchema)
        val schemaInterface = SchemaInterface.SchemaReference(schemaReference)

        val bundled = walk.schemas.bundle(schemaInterface, BundlingRun().rootScope())

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
