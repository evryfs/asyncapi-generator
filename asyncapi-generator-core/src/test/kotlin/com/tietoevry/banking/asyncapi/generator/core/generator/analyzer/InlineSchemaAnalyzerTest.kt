package com.tietoevry.banking.asyncapi.generator.core.generator.analyzer

import com.tietoevry.banking.asyncapi.generator.core.generator.analyzer.InlineSchemaAnalyzer
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InlineSchemaAnalyzerTest {

    private val analyzer = InlineSchemaAnalyzer()

    @Test
    fun `analyze should promote inline object to top-level schema`() {
        // Arrange
        val inlineObj = Schema(type = "object", properties = mapOf("a" to SchemaInterface.SchemaInline(Schema(type = "string"))))
        val root = Schema(type = "object", properties = mapOf("details" to SchemaInterface.SchemaInline(inlineObj)))
        val input = mapOf("Root" to root)

        // Act
        val result = analyzer.analyze(input)

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.containsKey("Details"), "Inline object property 'details' should be promoted to 'Details'")

        val rootProps = result["Root"]?.properties
        assertTrue(rootProps!!["details"] is SchemaInterface.SchemaReference, "Original property should be a reference now")
    }

    @Test
    fun `analyze should promote inline enum to top-level schema`() {
        // Arrange
        val inlineEnum = Schema(type = "string", enum = listOf("A", "B"))
        val root = Schema(type = "object", properties = mapOf("status" to SchemaInterface.SchemaInline(inlineEnum)))
        val input = mapOf("Root" to root)

        // Act
        val result = analyzer.analyze(input)

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.containsKey("Status"), "Inline enum property 'status' should be promoted to 'Status'")
    }

    @Test
    fun `analyze should use title for name if available`() {
        // Arrange
        val inlineObj = Schema(type = "object", title = "CustomName", properties = mapOf("a" to SchemaInterface.SchemaInline(Schema(type="string"))))
        val root = Schema(type = "object", properties = mapOf("details" to SchemaInterface.SchemaInline(inlineObj)))
        val input = mapOf("Root" to root)

        // Act
        val result = analyzer.analyze(input)

        // Assert
        assertTrue(result.containsKey("CustomName"), "Should use title 'CustomName' instead of property name 'Details'")
    }

    @Test
    fun `analyze should promote array items`() {
        // Arrange
        val itemSchema = Schema(type = "string", enum = listOf("X", "Y"))
        val arraySchema = Schema(type = "array", items = SchemaInterface.SchemaInline(itemSchema))
        val root = Schema(type = "object", properties = mapOf("tags" to SchemaInterface.SchemaInline(arraySchema)))
        val input = mapOf("Root" to root)

        // Act
        val result = analyzer.analyze(input)

        // Assert
        assertTrue(result.containsKey("Tags"), "Array items 'tags' should be promoted to singular 'Tag'")
    }
}
