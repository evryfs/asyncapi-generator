package com.tietoevry.banking.asyncapi.generator.core.generator.analyzer

import com.tietoevry.banking.asyncapi.generator.core.generator.analyzer.SchemaAnalyzer
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SchemaAnalyzerTest {

    private val analyzer = SchemaAnalyzer()

    @Test
    fun `analyze should orchestrate discovery and polymorphism analysis`() {
        // Arrange: A scenario combining all features
        // 1. Root has a ref to External (embedded)
        // 2. External has an inline enum property
        // 3. Root has a oneOf pointing to External

        val inlineEnum = Schema(type = "string", enum = listOf("A", "B"))
        val externalSchema = Schema(
            type = "object",
            properties = mapOf("status" to SchemaInterface.SchemaInline(inlineEnum))
        )

        val rootSchema = Schema(
            oneOf = listOf(
                SchemaInterface.SchemaReference(Reference(ref = "#/c/s/External", model = externalSchema))
            )
        )

        val input = mapOf("Root" to rootSchema)

        // Act
        val (schemas, relationships) = analyzer.analyze(input)

        // Assert
        // 1. ReferenceAnalyzer found "External"
        assertTrue(schemas.containsKey("External"))

        // 2. InlineSchemaAnalyzer found "Status" (from External.status)
        assertTrue(schemas.containsKey("Status"))

        // 3. PolymorphicAnalyzer linked External to Root
        assertEquals(listOf("Root"), relationships["External"])

        assertEquals(3, schemas.size, "Should have Root, External, and Status")
    }
}
