package dev.banking.asyncapi.generator.core.generator.normalizer

import dev.banking.asyncapi.generator.core.generator.normalizer.ConditionalNormalizer
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ConditionalNormalizerTest {

    private val normalizer = ConditionalNormalizer()

    @Test
    fun `process should merge properties from then-schema`() {
        val root = Schema(
            type = "object",
            properties = mapOf("type" to SchemaInterface.SchemaInline(Schema(type = "string"))),
            ifSchema = SchemaInterface.SchemaInline(
                Schema(
                    properties = mapOf(
                        "type" to SchemaInterface.SchemaInline(
                            Schema(const = "A")
                        )
                    )
                )
            ),
            thenSchema = SchemaInterface.SchemaInline(
                Schema(
                    properties = mapOf(
                        "extra" to SchemaInterface.SchemaInline(
                            Schema(type = "string")
                        )
                    )
                )
            )
        )

        val input = mapOf("Root" to root)

        val result = normalizer.normalize(input)

        val processed = result["Root"]
        assertNotNull(processed)
        assertNull(processed.ifSchema)
        assertNull(processed.thenSchema)

        val props = processed.properties
        assertEquals(props?.containsKey("type"), true)
        assertEquals(props?.containsKey("extra"), true, "Properties from 'then' schema should be merged")
    }

    @Test
    fun `process should resolve conflicting types to Any`() {
        val root = Schema(
            ifSchema = SchemaInterface.SchemaInline(Schema(properties = mapOf("flag" to SchemaInterface.SchemaInline(Schema(const = true))))),
            thenSchema = SchemaInterface.SchemaInline(Schema(properties = mapOf("value" to SchemaInterface.SchemaInline(Schema(type = "integer"))))),
            elseSchema = SchemaInterface.SchemaInline(Schema(properties = mapOf("value" to SchemaInterface.SchemaInline(Schema(type = "string")))))
        )

        val input = mapOf("Root" to root)

        val result = normalizer.normalize(input)

        val processed = result["Root"]
        val valueProp = (processed?.properties?.get("value") as? SchemaInterface.SchemaInline)?.schema

        assertNotNull(valueProp)
        assertNull(valueProp.type, "Conflicting types should result in a schemaless (Any) definition")
    }
}
