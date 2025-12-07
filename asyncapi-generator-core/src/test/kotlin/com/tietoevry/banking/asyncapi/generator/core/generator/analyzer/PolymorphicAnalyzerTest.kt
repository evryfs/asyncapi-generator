package com.tietoevry.banking.asyncapi.generator.core.generator.analyzer

import com.tietoevry.banking.asyncapi.generator.core.generator.analyzer.PolymorphicAnalyzer
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PolymorphicAnalyzerTest {

    private val analyzer = PolymorphicAnalyzer()

    @Test
    fun `analyze should map oneOf children to parent`() {
        // Arrange
        val parent = Schema(
            oneOf = listOf(
                SchemaInterface.SchemaReference(Reference("#/c/s/ChildA")),
                SchemaInterface.SchemaReference(Reference("#/c/s/ChildB"))
            )
        )
        val input = mapOf("Parent" to parent, "ChildA" to Schema(), "ChildB" to Schema())

        // Act
        val result = analyzer.analyze(input)

        // Assert
        assertEquals(2, result.size)
        assertEquals(listOf("Parent"), result["ChildA"])
        assertEquals(listOf("Parent"), result["ChildB"])
    }

    @Test
    fun `analyze should map anyOf children to parent`() {
        // Arrange
        val parent = Schema(
            anyOf = listOf(
                SchemaInterface.SchemaReference(Reference("#/c/s/ChildA"))
            )
        )
        val input = mapOf("Parent" to parent, "ChildA" to Schema())

        // Act
        val result = analyzer.analyze(input)

        // Assert
        assertEquals(listOf("Parent"), result["ChildA"])
    }

    @Test
    fun `analyze should handle child implementing multiple parents`() {
        // Arrange
        val parent1 = Schema(oneOf = listOf(SchemaInterface.SchemaReference(Reference("#/c/s/Child"))))
        val parent2 = Schema(oneOf = listOf(SchemaInterface.SchemaReference(Reference("#/c/s/Child"))))
        val input = mapOf("Parent1" to parent1, "Parent2" to parent2, "Child" to Schema())

        // Act
        val result = analyzer.analyze(input)

        // Assert
        val parents = result["Child"]
        assertEquals(2, parents?.size)
        assertTrue(parents!!.contains("Parent1"))
        assertTrue(parents.contains("Parent2"))
    }
}
