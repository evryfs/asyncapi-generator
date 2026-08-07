package dev.banking.asyncapi.generator.core.generator.normalizer

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompositionNormalizerTest {

    private val normalizer = CompositionNormalizer()

    @Test
    fun `process should flatten simple allOf composition`() {
        val parent = Schema(
            type = "object",
            properties = mapOf("parentProp" to SchemaInterface.SchemaInline(Schema(type = "string")))
        )

        val child = Schema(
            allOf = listOf(
                SchemaInterface.SchemaReference(Reference("#/components/schemas/Parent", model = parent))
            ),
            properties = mapOf("childProp" to SchemaInterface.SchemaInline(Schema(type = "integer")))
        )

        val input = mapOf(
            "Parent" to parent,
            "Child" to child
        )

        val result = normalizer.normalize(input)

        val processedChild = result["Child"]
        assertNotNull(processedChild)
        assertNull(processedChild.allOf, "allOf should be removed after flattening")

        val props = processedChild.properties
        assertNotNull(props)
        assertEquals(2, props.size)
        assertTrue(props.containsKey("parentProp"), "Should inherit property from parent")
        assertTrue(props.containsKey("childProp"), "Should keep own property")
    }

    @Test
    fun `process should override properties from parent`() {
        val parent = Schema(
            properties = mapOf("status" to SchemaInterface.SchemaInline(Schema(type = "string")))
        )

        val child = Schema(
            allOf = listOf(
                SchemaInterface.SchemaReference(Reference("#/components/schemas/Parent", model = parent))
            ),
            properties = mapOf("status" to SchemaInterface.SchemaInline(Schema(type = "string", enum = listOf("A", "B"))))
        )

        val input = mapOf("Parent" to parent, "Child" to child)

        val result = normalizer.normalize(input)

        val processedChild = result["Child"]
        val statusProp = (processedChild?.properties?.get("status") as? SchemaInterface.SchemaInline)?.schema

        assertNotNull(statusProp)
        assertEquals(listOf("A", "B"), statusProp.enum, "Child property should override parent")
    }

    @Test
    fun `process should handle nested inline allOf`() {
        val parent = Schema(type="object", properties=mapOf("a" to SchemaInterface.SchemaInline(Schema(type="string"))))

        val inlineWithAllOf = Schema(
            allOf = listOf(SchemaInterface.SchemaReference(Reference("#/components/schemas/Parent", model=parent)))
        )

        val root = Schema(
            properties = mapOf("inline" to SchemaInterface.SchemaInline(inlineWithAllOf))
        )

        val input = mapOf("Parent" to parent, "Root" to root)

        val result = normalizer.normalize(input)

        val rootProps = result["Root"]?.properties
        val processedInline = (rootProps?.get("inline") as? SchemaInterface.SchemaInline)?.schema

        assertNotNull(processedInline)
        assertNull(processedInline.allOf)
        assertTrue(processedInline.properties?.containsKey("a") == true, "Nested inline schema should be flattened")
    }

    @Test
    fun `normalizes compositions inside array items and map values`() {
        val inheritedProperty =
            mapOf(
                "inherited" to SchemaInterface.SchemaInline(Schema(type = "string")),
            )
        val parent = Schema(type = "object", properties = inheritedProperty)
        val composedValue =
            Schema(
                allOf =
                    listOf(
                        SchemaInterface.SchemaReference(
                            Reference("#/components/schemas/Parent", model = parent),
                        ),
                    ),
            )
        val root =
            Schema(
                properties =
                    mapOf(
                        "values" to
                            SchemaInterface.SchemaInline(
                                Schema(
                                    type = "array",
                                    items = SchemaInterface.SchemaInline(composedValue),
                                ),
                            ),
                        "valuesByName" to
                            SchemaInterface.SchemaInline(
                                Schema(
                                    type = "object",
                                    additionalProperties = SchemaInterface.SchemaInline(composedValue),
                                ),
                            ),
                    ),
            )

        val result = normalizer.normalize(mapOf("Parent" to parent, "Root" to root))

        val values = (result.getValue("Root").properties?.get("values") as SchemaInterface.SchemaInline).schema
        val itemSchema = (values.items as SchemaInterface.SchemaInline).schema
        assertNull(itemSchema.allOf)
        assertEquals(inheritedProperty, itemSchema.properties)

        val valuesByName =
            (result.getValue("Root").properties?.get("valuesByName") as SchemaInterface.SchemaInline).schema
        val valueSchema = (valuesByName.additionalProperties as SchemaInterface.SchemaInline).schema
        assertNull(valueSchema.allOf)
        assertEquals(inheritedProperty, valueSchema.properties)
    }

    @Test
    fun `follows chained resolved references in compositions`() {
        val base =
            Schema(
                type = "object",
                properties =
                    mapOf(
                        "baseValue" to SchemaInterface.SchemaInline(Schema(type = "string")),
                    ),
            )
        val baseReference = Reference("#/components/schemas/Base", model = base)
        val aliasReference = Reference("#/components/schemas/BaseAlias", model = baseReference)
        val child =
            Schema(
                allOf = listOf(SchemaInterface.SchemaReference(aliasReference)),
                properties =
                    mapOf(
                        "childValue" to SchemaInterface.SchemaInline(Schema(type = "integer")),
                    ),
            )

        val result = normalizer.normalize(mapOf("Child" to child)).getValue("Child")

        assertNull(result.allOf)
        assertEquals(setOf("baseValue", "childValue"), result.properties?.keys)
    }

    @Test
    fun `normalizes distinct schemas that share a component name`() {
        val base =
            Schema(
                properties =
                    mapOf(
                        "baseValue" to SchemaInterface.SchemaInline(Schema(type = "string")),
                    ),
            )
        val distinctCommon =
            Schema(
                allOf =
                    listOf(
                        SchemaInterface.SchemaReference(
                            Reference("#/components/schemas/Base", model = base),
                        ),
                    ),
                properties =
                    mapOf(
                        "commonValue" to SchemaInterface.SchemaInline(Schema(type = "string")),
                    ),
            )
        val root =
            Schema(
                allOf =
                    listOf(
                        SchemaInterface.SchemaReference(
                            Reference("external.yaml#/components/schemas/Common", model = distinctCommon),
                        ),
                    ),
                properties =
                    mapOf(
                        "rootValue" to SchemaInterface.SchemaInline(Schema(type = "string")),
                    ),
            )

        val result = normalizer.normalize(mapOf("Common" to root)).getValue("Common")

        assertNull(result.allOf)
        assertEquals(setOf("baseValue", "commonValue", "rootValue"), result.properties?.keys)
    }

    @Test
    fun `terminates mutually recursive compositions`() {
        val referenceToA = Reference("#/components/schemas/A")
        val referenceToB = Reference("#/components/schemas/B")
        val schemaA =
            Schema(
                allOf = listOf(SchemaInterface.SchemaReference(referenceToB)),
                properties = mapOf("a" to SchemaInterface.SchemaInline(Schema(type = "string"))),
            )
        val schemaB =
            Schema(
                allOf = listOf(SchemaInterface.SchemaReference(referenceToA)),
                properties = mapOf("b" to SchemaInterface.SchemaInline(Schema(type = "string"))),
            )
        referenceToA.model = schemaA
        referenceToB.model = schemaB

        val result = normalizer.normalize(mapOf("A" to schemaA, "B" to schemaB))

        assertNull(result.getValue("A").allOf)
        assertEquals(setOf("a", "b"), result.getValue("A").properties?.keys)
        assertNull(result.getValue("B").allOf)
        assertEquals(setOf("a", "b"), result.getValue("B").properties?.keys)
    }
}
