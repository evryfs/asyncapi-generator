package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.factory

import com.tietoevry.banking.asyncapi.generator.core.generator.context.GeneratorContext
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultValueFactoryTest {

    @Test
    fun `createDefaultValue should format strings with quotes`() {
        val context = GeneratorContext(emptyMap())
        val factory = DefaultValueFactory(context)
        val schema = Schema(type = "string", default = "hello")

        val result = factory.createDefaultValue(schema, "String", false)

        assertEquals("\"hello\"", result)
    }

    @Test
    fun `createDefaultValue should format numbers without quotes`() {
        val context = GeneratorContext(emptyMap())
        val factory = DefaultValueFactory(context)
        val schema = Schema(type = "integer", default = 42)

        val result = factory.createDefaultValue(schema, "Int", false)

        assertEquals("42", result)
    }

    @Test
    fun `createDefaultValue should format enums as static reference`() {
        val enumSchema = Schema(type = "string", enum = listOf("A", "B"))
        val schemas = mapOf("MyEnum" to enumSchema)
        val context = GeneratorContext(schemas)
        val factory = DefaultValueFactory(context)

        val propSchema = Schema(type = "string", default = "A")

        val result = factory.createDefaultValue(propSchema, "MyEnum", false)

        assertEquals("MyEnum.A", result)
    }

    @Test
    fun `createDefaultValue should return null string if nullable and no default`() {
        val context = GeneratorContext(emptyMap())
        val factory = DefaultValueFactory(context)
        val schema = Schema(type = "string")

        val result = factory.createDefaultValue(schema, "String", true)

        assertEquals("null", result)
    }

    @Test
    fun `createDefaultValue should return null if not nullable and no default`() {
        val context = GeneratorContext(emptyMap())
        val factory = DefaultValueFactory(context)
        val schema = Schema(type = "string")

        val result = factory.createDefaultValue(schema, "String", false)

        assertNull(result)
    }
}
