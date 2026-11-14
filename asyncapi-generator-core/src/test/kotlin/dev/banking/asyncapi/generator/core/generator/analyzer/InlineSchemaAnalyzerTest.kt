package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.generator.analyzer.InlineSchemaAnalyzer
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InlineSchemaNameCollision
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

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

    @Test
    fun `analyze shares one name between identical inline schemas`() {
        val address = Schema(type = "object", properties = mapOf("street" to SchemaInterface.SchemaInline(Schema(type = "string"))))
        val input = mapOf(
            "Customer" to Schema(type = "object", properties = mapOf("address" to SchemaInterface.SchemaInline(address))),
            "Supplier" to Schema(type = "object", properties = mapOf("address" to SchemaInterface.SchemaInline(address))),
        )

        val result = analyzer.analyze(input)

        assertEquals(address, result["Address"])
    }

    @Test
    fun `analyze rejects different inline schemas that would get the same name`() {
        fun withAddress(field: String) = Schema(
            type = "object",
            properties = mapOf(
                "address" to SchemaInterface.SchemaInline(
                    Schema(type = "object", properties = mapOf(field to SchemaInterface.SchemaInline(Schema(type = "string")))),
                ),
            ),
        )
        val input = mapOf("Customer" to withAddress("street"), "Supplier" to withAddress("zip"))

        val failure = assertFailsWith<InlineSchemaNameCollision> { analyzer.analyze(input) }

        val message = failure.message.orEmpty()
        assertTrue("'Address'" in message, message)
        assertTrue("components.schemas.Customer.properties.address" in message, message)
        assertTrue("components.schemas.Supplier.properties.address" in message, message)
    }

    @Test
    fun `analyze rejects an inline schema that would replace a component schema`() {
        val input = mapOf(
            "Address" to Schema(type = "object", properties = mapOf("street" to SchemaInterface.SchemaInline(Schema(type = "string")))),
            "Customer" to Schema(
                type = "object",
                properties = mapOf(
                    "address" to SchemaInterface.SchemaInline(
                        Schema(type = "object", properties = mapOf("zip" to SchemaInterface.SchemaInline(Schema(type = "integer")))),
                    ),
                ),
            ),
        )

        assertFailsWith<InlineSchemaNameCollision> { analyzer.analyze(input) }
    }

    @Test
    fun `analyze names inline items of a named array after the array`() {
        val item = Schema(type = "object", properties = mapOf("id" to SchemaInterface.SchemaInline(Schema(type = "string"))))
        val input = mapOf("OrderList" to Schema(type = "array", items = SchemaInterface.SchemaInline(item)))

        val result = analyzer.analyze(input)

        assertEquals(item, result["OrderListItem"])
        assertEquals("array", result["OrderList"]?.type)
    }
}
