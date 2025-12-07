package com.tietoevry.banking.asyncapi.generator.core.generator.normalizer

import com.tietoevry.banking.asyncapi.generator.core.generator.normalizer.SchemaMerger
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MergeSchemaNormalizerTest {

    private val merger = SchemaMerger()

    @Test
    fun `merge should prioritize override values`() {
        val base = Schema(description = "Base Desc", minLength = 10)
        val override = Schema(description = "Override Desc", minLength = 5)

        val result = merger.merge(base, override)

        assertEquals("Override Desc", result.description)
    }

    @Test
    fun `merge should intersect numeric constraints`() {
        val base = Schema(minimum = 5.toBigDecimal())
        val override = Schema(minimum = 10.toBigDecimal())

        val result = merger.merge(base, override)

        assertEquals(10.toBigDecimal(), result.minimum, "Should take the stricter (larger) minimum")
    }

    @Test
    fun `merge should deep merge properties`() {
        val baseProps = mapOf(
            "shared" to SchemaInterface.SchemaInline(Schema(title = "BaseTitle", type="string")),
            "baseOnly" to SchemaInterface.SchemaInline(Schema(type="integer"))
        )
        val base = Schema(properties = baseProps)

        val overrideProps = mapOf(
            "shared" to SchemaInterface.SchemaInline(Schema(description = "Added Desc"))
        )
        val override = Schema(properties = overrideProps)

        val result = merger.merge(base, override)

        val props = result.properties
        assertNotNull(props)
        assertEquals(2, props.size)

        val shared = (props["shared"] as? SchemaInterface.SchemaInline)?.schema
        assertEquals("BaseTitle", shared?.title, "Should retain base title")
        assertEquals("Added Desc", shared?.description, "Should add override description")
    }
}
