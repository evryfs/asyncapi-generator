package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnumTypeAnalyzerTest {

    private val analyzer = EnumTypeAnalyzer()

    @Test
    fun `infers generator string type for top-level and inline all-string enums`() {
        val inlineEnum = Schema(enum = listOf("ACTIVE", "INACTIVE"))
        val root = Schema(
            enum = listOf("CREATE", "UPDATE"),
            properties = mapOf("status" to SchemaInterface.SchemaInline(inlineEnum)),
        )

        val analyzed = analyzer.analyze(mapOf("Action" to root)).getValue("Action")

        assertEquals("string", analyzed.type)
        val analyzedInline = (analyzed.properties?.getValue("status") as SchemaInterface.SchemaInline).schema
        assertEquals("string", analyzedInline.type)
        assertNull(root.type)
        assertNull(inlineEnum.type)
    }

    @Test
    fun `does not infer generator type for an enum containing non-string values`() {
        val schema = Schema(enum = listOf("one", 2))

        val analyzed = analyzer.analyze(mapOf("Mixed" to schema)).getValue("Mixed")

        assertNull(analyzed.type)
    }
}
