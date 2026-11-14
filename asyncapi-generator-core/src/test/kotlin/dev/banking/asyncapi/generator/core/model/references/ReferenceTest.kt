package dev.banking.asyncapi.generator.core.model.references

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ReferenceTest {

    @Test
    fun `recursive schemas can be compared, hashed and printed`() {
        val first = recursiveNode()
        val second = recursiveNode()

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertTrue("#/components/schemas/Node" in first.toString())
    }

    @Test
    fun `references are equal when they point at the same target`() {
        val reference = Reference(ref = "#/components/schemas/Node", sourceId = "main")

        assertEquals(reference, reference.copy(model = Schema(type = "string")))
        assertNotEquals(reference, reference.copy(ref = "#/components/schemas/Other"))
        assertNotEquals(reference, reference.copy(sourceId = "external"))
    }

    private fun recursiveNode(): Schema {
        val reference = Reference(ref = "#/components/schemas/Node")
        val node = Schema(type = "object", properties = mapOf("next" to SchemaInterface.SchemaReference(reference)))
        reference.model = node
        return node
    }
}
