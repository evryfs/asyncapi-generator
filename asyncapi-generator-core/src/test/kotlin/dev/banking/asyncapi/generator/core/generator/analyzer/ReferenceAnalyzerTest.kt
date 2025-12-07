package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.generator.analyzer.ReferenceAnalyzer
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReferenceAnalyzerTest {

    private val analyzer = ReferenceAnalyzer()

    @Test
    fun `analyze should discover schema embedded in reference model`() {
        // Arrange: A schema with a property that refs an external schema (embedded in 'model')
        val externalSchema = Schema(type = "object", title = "External")
        val ref = Reference(ref = "#/components/schemas/External", model = externalSchema)

        val rootSchema = Schema(
            type = "object",
            properties = mapOf("field" to SchemaInterface.SchemaReference(ref))
        )
        val input = mapOf("Root" to rootSchema)

        // Act
        val result = analyzer.analyze(input)

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.containsKey("Root"))
        assertTrue(result.containsKey("External"), "External schema should be discovered from reference")
        assertEquals(externalSchema, result["External"])
    }

    @Test
    fun `analyze should handle nested references`() {
        // Arrange: Root -> Middle (ref) -> End (ref)
        val endSchema = Schema(type = "string")
        val middleSchema = Schema(
            type = "object",
            properties = mapOf(
                "end" to SchemaInterface.SchemaReference(
                    Reference(
                        ref = "#/c/s/End",
                        model = endSchema
                    )
                )
            )
        )
        val rootSchema = Schema(
            type = "object",
            properties = mapOf(
                "middle" to SchemaInterface.SchemaReference(
                    Reference(
                        ref = "#/c/s/Middle",
                        model = middleSchema
                    )
                )
            )
        )
        val input = mapOf("Root" to rootSchema)

        // Act
        val result = analyzer.analyze(input)

        // Assert
        assertEquals(3, result.size)
        assertTrue(result.containsKey("Root"))
        assertTrue(result.containsKey("Middle"))
        assertTrue(result.containsKey("End"), "Deeply nested reference should be discovered")
    }

    @Test
    fun `analyze should ignore references without model`() {
        // Arrange: Reference pointing to internal schema (no model embedded)
        val ref = Reference(ref = "#/components/schemas/Existing")
        val rootSchema = Schema(properties = mapOf("field" to SchemaInterface.SchemaReference(ref)))
        val input = mapOf("Root" to rootSchema, "Existing" to Schema(type = "string"))

        // Act
        val result = analyzer.analyze(input)

        // Assert
        assertEquals(2, result.size, "Should not add any new schemas")
    }
}
