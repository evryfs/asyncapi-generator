package dev.banking.asyncapi.generator.core.generator.kotlin.factory

import dev.banking.asyncapi.generator.core.generator.context.GeneratorContext
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PropertyFactoryTest {

    @Test
    fun `createProperty should create nullable property if not required`() {
        val context = GeneratorContext(emptyMap())
        val factory = PropertyFactory(context)
        val schema = SchemaInterface.SchemaInline(Schema(type = "string"))

        val result = factory.createProperty("myProp", schema, emptyList())

        assertEquals("String?", result.typeName)
        assertEquals("null", result.defaultValue)
    }

    @Test
    fun `createProperty should create non-nullable property if required`() {
        val context = GeneratorContext(emptyMap())
        val factory = PropertyFactory(context)
        val schema = SchemaInterface.SchemaInline(Schema(type = "string"))

        val result = factory.createProperty("myProp", schema, listOf("myProp"))

        assertEquals("String", result.typeName)
        assertEquals(null, result.defaultValue)
    }

    @Test
    fun `createProperty should include validation annotations for models`() {
        val schemas = mapOf("SomeModel" to Schema(type = "object"))
        val context = GeneratorContext(schemas)
        val factory = PropertyFactory(context)

        val ref = dev.banking.asyncapi.generator.core.model.references.Reference("#/components/schemas/SomeModel")
        val schema = SchemaInterface.SchemaReference(ref)

        val result = factory.createProperty("refProp", schema, listOf("refProp"))

        assertEquals("SomeModel", result.typeName)
        assertTrue(result.annotations.contains("@field:Valid"), "Should add @Valid for model references")
    }
}
