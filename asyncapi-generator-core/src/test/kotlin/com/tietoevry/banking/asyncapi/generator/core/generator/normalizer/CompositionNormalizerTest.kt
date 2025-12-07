package com.tietoevry.banking.asyncapi.generator.core.generator.normalizer

import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompositionNormalizerTest {

    private val normalizer = CompositionNormalizer()

    @Test
    fun `process should flatten simple allOf composition`() {
        val parent = Schema(
            type = "object",
            properties = mapOf("parentProp" to SchemaInterface.SchemaInline(Schema(type = "string")))
        )

        val child = Schema(
            allOf = listOf(
                SchemaInterface.SchemaReference(Reference("#/components/schemas/Parent", model = parent))
            ),
            properties = mapOf("childProp" to SchemaInterface.SchemaInline(Schema(type = "integer")))
        )

        val input = mapOf(
            "Parent" to parent,
            "Child" to child
        )

        val result = normalizer.normalize(input)

        val processedChild = result["Child"]
        assertNotNull(processedChild)
        assertNull(processedChild.allOf, "allOf should be removed after flattening")

        val props = processedChild.properties
        assertNotNull(props)
        assertEquals(2, props.size)
        assertTrue(props.containsKey("parentProp"), "Should inherit property from parent")
        assertTrue(props.containsKey("childProp"), "Should keep own property")
    }

    @Test
    fun `process should override properties from parent`() {
        val parent = Schema(
            properties = mapOf("status" to SchemaInterface.SchemaInline(Schema(type = "string")))
        )

        val child = Schema(
            allOf = listOf(
                SchemaInterface.SchemaReference(Reference("#/components/schemas/Parent", model = parent))
            ),
            properties = mapOf("status" to SchemaInterface.SchemaInline(Schema(type = "string", enum = listOf("A", "B"))))
        )

        val input = mapOf("Parent" to parent, "Child" to child)

        val result = normalizer.normalize(input)

        val processedChild = result["Child"]
        val statusProp = (processedChild?.properties?.get("status") as? SchemaInterface.SchemaInline)?.schema

        assertNotNull(statusProp)
        assertEquals(listOf("A", "B"), statusProp.enum, "Child property should override parent")
    }

    @Test
    fun `process should handle nested inline allOf`() {
        val parent = Schema(type="object", properties=mapOf("a" to SchemaInterface.SchemaInline(Schema(type="string"))))

        val inlineWithAllOf = Schema(
            allOf = listOf(SchemaInterface.SchemaReference(Reference("#/components/schemas/Parent", model=parent)))
        )

        val root = Schema(
            properties = mapOf("inline" to SchemaInterface.SchemaInline(inlineWithAllOf))
        )

        val input = mapOf("Parent" to parent, "Root" to root)

        val result = normalizer.normalize(input)

        val rootProps = result["Root"]?.properties
        val processedInline = (rootProps?.get("inline") as? SchemaInterface.SchemaInline)?.schema

        assertNotNull(processedInline)
        assertNull(processedInline.allOf)
        assertTrue(processedInline.properties?.containsKey("a") == true, "Nested inline schema should be flattened")
    }
}
