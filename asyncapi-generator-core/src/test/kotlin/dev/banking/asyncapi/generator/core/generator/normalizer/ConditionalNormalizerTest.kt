package dev.banking.asyncapi.generator.core.generator.normalizer

import dev.banking.asyncapi.generator.core.model.references.Reference
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

    @Test
    fun `includes properties from referenced conditional branches`() {
        val thenBranch =
            Schema(
                properties =
                    mapOf(
                        "thenValue" to SchemaInterface.SchemaInline(Schema(type = "string")),
                    ),
            )
        val elseBranch =
            Schema(
                properties =
                    mapOf(
                        "elseValue" to SchemaInterface.SchemaInline(Schema(type = "integer")),
                    ),
            )
        val root =
            Schema(
                ifSchema = SchemaInterface.SchemaInline(Schema()),
                thenSchema =
                    SchemaInterface.SchemaReference(
                        Reference("#/components/schemas/ThenBranch", model = thenBranch),
                    ),
                elseSchema =
                    SchemaInterface.SchemaReference(
                        Reference("#/components/schemas/ElseBranch", model = elseBranch),
                    ),
            )

        val result = normalizer.normalize(mapOf("Root" to root)).getValue("Root")

        assertNull(result.ifSchema)
        assertNull(result.thenSchema)
        assertNull(result.elseSchema)
        assertEquals(setOf("thenValue", "elseValue"), result.properties?.keys)
    }

    @Test
    fun `normalizes conditionals inside nested object array and map schemas`() {
        val conditionalValue =
            Schema(
                ifSchema = SchemaInterface.SchemaInline(Schema()),
                thenSchema =
                    SchemaInterface.SchemaInline(
                        Schema(
                            properties =
                                mapOf(
                                    "conditionalValue" to
                                        SchemaInterface.SchemaInline(Schema(type = "string")),
                                ),
                        ),
                    ),
            )
        val root =
            Schema(
                properties =
                    mapOf(
                        "nested" to SchemaInterface.SchemaInline(conditionalValue),
                        "values" to
                            SchemaInterface.SchemaInline(
                                Schema(
                                    type = "array",
                                    items = SchemaInterface.SchemaInline(conditionalValue),
                                ),
                            ),
                        "valuesByName" to
                            SchemaInterface.SchemaInline(
                                Schema(
                                    type = "object",
                                    additionalProperties = SchemaInterface.SchemaInline(conditionalValue),
                                ),
                            ),
                    ),
            )

        val result = normalizer.normalize(mapOf("Root" to root)).getValue("Root")

        val nested = (result.properties?.get("nested") as SchemaInterface.SchemaInline).schema
        assertNull(nested.thenSchema)
        assertEquals(setOf("conditionalValue"), nested.properties?.keys)

        val values = (result.properties?.get("values") as SchemaInterface.SchemaInline).schema
        val itemSchema = (values.items as SchemaInterface.SchemaInline).schema
        assertNull(itemSchema.thenSchema)
        assertEquals(setOf("conditionalValue"), itemSchema.properties?.keys)

        val valuesByName = (result.properties?.get("valuesByName") as SchemaInterface.SchemaInline).schema
        val valueSchema = (valuesByName.additionalProperties as SchemaInterface.SchemaInline).schema
        assertNull(valueSchema.thenSchema)
        assertEquals(setOf("conditionalValue"), valueSchema.properties?.keys)
    }

    @Test
    fun `does not apply one branch constraints to every generated value`() {
        val root =
            Schema(
                ifSchema = SchemaInterface.SchemaInline(Schema()),
                thenSchema =
                    SchemaInterface.SchemaInline(
                        Schema(
                            properties =
                                mapOf(
                                    "value" to
                                        SchemaInterface.SchemaInline(
                                            Schema(
                                                type = "string",
                                                pattern = "A+",
                                                description = "Conditional value.",
                                            ),
                                        ),
                                ),
                        ),
                    ),
                elseSchema =
                    SchemaInterface.SchemaInline(
                        Schema(
                            properties =
                                mapOf(
                                    "value" to
                                        SchemaInterface.SchemaInline(
                                            Schema(
                                                type = "string",
                                                pattern = "B+",
                                            ),
                                        ),
                                ),
                        ),
                    ),
            )

        val result = normalizer.normalize(mapOf("Root" to root)).getValue("Root")
        val value = (result.properties?.get("value") as SchemaInterface.SchemaInline).schema

        assertEquals("string", value.type)
        assertEquals("Conditional value.", value.description)
        assertNull(value.pattern)
    }
}
