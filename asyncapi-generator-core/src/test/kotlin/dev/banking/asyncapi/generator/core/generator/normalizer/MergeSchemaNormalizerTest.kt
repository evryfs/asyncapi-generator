package dev.banking.asyncapi.generator.core.generator.normalizer

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        val base =
            Schema(
                properties =
                    mapOf(
                        "shared" to SchemaInterface.SchemaInline(Schema(title = "BaseTitle", type = "string")),
                        "baseOnly" to SchemaInterface.SchemaInline(Schema(type = "integer")),
                    ),
            )
        val override =
            Schema(
                properties =
                    mapOf(
                        "shared" to SchemaInterface.SchemaInline(Schema(description = "Added Desc")),
                    ),
            )

        val result = merger.merge(base, override)

        val props = result.properties
        assertNotNull(props)
        assertEquals(2, props.size)

        val shared = (props["shared"] as? SchemaInterface.SchemaInline)?.schema
        assertEquals("BaseTitle", shared?.title, "Should retain base title")
        assertEquals("Added Desc", shared?.description, "Should add override description")
    }

    @Test
    fun `merge preserves generator property shapes from the base schema`() {
        val stringItems = SchemaInterface.SchemaInline(Schema(type = "string"))
        val stringValues = SchemaInterface.SchemaInline(Schema(type = "string"))
        val base =
            Schema(
                properties =
                    mapOf(
                        "tags" to
                            SchemaInterface.SchemaInline(
                                Schema(
                                    type = "array",
                                    items = stringItems,
                                ),
                            ),
                        "attributes" to
                            SchemaInterface.SchemaInline(
                                Schema(
                                    type = "object",
                                    additionalProperties = stringValues,
                                ),
                            ),
                    ),
            )
        val override =
            Schema(
                properties =
                    mapOf(
                        "tags" to SchemaInterface.SchemaInline(Schema(description = "Tags.")),
                        "attributes" to SchemaInterface.SchemaInline(Schema(description = "Attributes.")),
                    ),
            )

        val result = merger.merge(base, override)

        val tags = (result.properties?.get("tags") as SchemaInterface.SchemaInline).schema
        assertEquals("array", tags.type)
        assertEquals(stringItems, tags.items)
        assertEquals("Tags.", tags.description)

        val attributes = (result.properties?.get("attributes") as SchemaInterface.SchemaInline).schema
        assertEquals("object", attributes.type)
        assertEquals(stringValues, attributes.additionalProperties)
        assertEquals("Attributes.", attributes.description)
    }

    @Test
    fun `merge preserves generator constraints and explicit null values`() {
        val base =
            Schema(
                type = listOf("string", "null"),
                pattern = "[A-Z]+",
                default = null,
                defaultSet = true,
                const = null,
                constSet = true,
            )

        val result = merger.merge(base, Schema(description = "Override description."))
        val numericResult = merger.merge(Schema(type = "number", multipleOf = 2), Schema())

        assertEquals(listOf("string", "null"), result.type)
        assertEquals("[A-Z]+", result.pattern)
        assertNull(result.default)
        assertTrue(result.defaultSet)
        assertNull(result.const)
        assertTrue(result.constSet)
        assertEquals(2, numericResult.multipleOf)
    }

    @Test
    fun `merge lets explicit null values override inherited exact values`() {
        val base =
            Schema(
                default = "inherited",
                defaultSet = true,
                const = "inherited",
                constSet = true,
            )
        val override =
            Schema(
                default = null,
                defaultSet = true,
                const = null,
                constSet = true,
            )

        val result = merger.merge(base, override)

        assertNull(result.default)
        assertTrue(result.defaultSet)
        assertNull(result.const)
        assertTrue(result.constSet)
    }

    @Test
    fun `merge preserves inherited polymorphic model metadata`() {
        val alternatives =
            listOf(
                SchemaInterface.SchemaReference(Reference("#/components/schemas/CardPayment")),
                SchemaInterface.SchemaReference(Reference("#/components/schemas/BankPayment")),
            )
        val base =
            Schema(
                oneOf = alternatives,
                discriminator = "paymentType",
            )

        val result = merger.merge(base, Schema(description = "Payment."))

        assertEquals(alternatives, result.oneOf)
        assertEquals("paymentType", result.discriminator)
        assertEquals("Payment.", result.description)
    }

    @Test
    fun `merge selects the strictest inclusive or exclusive numeric bounds`() {
        val base =
            Schema(
                type = "number",
                minimum = 5,
                exclusiveMaximum = 100,
            )
        val override =
            Schema(
                exclusiveMinimum = 5,
                maximum = 80,
            )

        val result = merger.merge(base, override)

        assertNull(result.minimum)
        assertEquals(5, result.exclusiveMinimum)
        assertEquals(80, result.maximum)
        assertNull(result.exclusiveMaximum)
    }
}
