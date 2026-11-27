package dev.banking.asyncapi.generator.core.generator.sourcemodel

import dev.banking.asyncapi.generator.core.generator.sourcemodel.TypeRef.ListOf
import dev.banking.asyncapi.generator.core.generator.sourcemodel.TypeRef.MapOf
import dev.banking.asyncapi.generator.core.generator.sourcemodel.TypeRef.Named
import dev.banking.asyncapi.generator.core.generator.sourcemodel.TypeRef.Scalar
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.JsonSchemaType
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ModelTypeMapperTest {
    private val mapper =
        ModelTypeMapper(
            mapOf(
                "User" to
                    Schema(
                        type = JsonSchemaType.Single("object"),
                        properties =
                            mapOf("id" to inline(Schema(type = JsonSchemaType.Single("string")))),
                    ),
                "Status" to Schema(type = JsonSchemaType.Single("string"), enum = listOf("A", "B")),
                "Name" to Schema(type = JsonSchemaType.Single("string")),
                "Anything" to Schema(type = JsonSchemaType.Single("object")),
            ),
        )

    @Test
    fun `maps string formats, numbers, and booleans to scalars`() {
        val expectations =
            mapOf(
                Schema(type = JsonSchemaType.Single("string")) to Scalar.STRING,
                Schema(type = JsonSchemaType.Single("string"), format = "email") to Scalar.STRING,
                Schema(type = JsonSchemaType.Single("string"), format = "uuid") to Scalar.UUID,
                Schema(type = JsonSchemaType.Single("string"), format = "date") to Scalar.DATE,
                Schema(type = JsonSchemaType.Single("string"), format = "time") to Scalar.TIME,
                Schema(type = JsonSchemaType.Single("string"), format = "date-time") to Scalar.DATE_TIME,
                Schema(type = JsonSchemaType.Single("integer")) to Scalar.INT32,
                Schema(type = JsonSchemaType.Single("integer"), format = "int64") to Scalar.INT64,
                Schema(type = JsonSchemaType.Single("integer"), maximum = Int.MAX_VALUE.toLong() + 1) to Scalar.INT64,
                Schema(type = JsonSchemaType.Single("integer"), minimum = 0, maximum = 10) to Scalar.INT32,
                Schema(type = JsonSchemaType.Single("number")) to Scalar.DOUBLE,
                Schema(type = JsonSchemaType.Single("number"), format = "float") to Scalar.FLOAT,
                Schema(type = JsonSchemaType.Single("number"), multipleOf = 0.01) to Scalar.DECIMAL,
                Schema(type = JsonSchemaType.Single("boolean")) to Scalar.BOOLEAN,
                Schema(type = JsonSchemaType.Multiple(listOf("string", "integer"))) to Scalar.ANY,
            )

        expectations.forEach { (schema, expected) -> assertEquals(expected, mapper.map("value", schema), "$schema") }
    }

    @Test
    fun `maps arrays to lists of their item type`() {
        assertEquals(
            ListOf(Scalar.STRING),
            mapper.map("tags", array(inline(Schema(type = JsonSchemaType.Single("string"))))),
        )
        assertEquals(ListOf(Scalar.ANY), mapper.map("tags", Schema(type = JsonSchemaType.Single("array"))))
        assertEquals(ListOf(Named("User", isModel = true)), mapper.map("users", array(ref("User"))))
        assertEquals(ListOf(Named("Status", isModel = false)), mapper.map("statuses", array(ref("Status"))))
        assertEquals(ListOf(Scalar.STRING), mapper.map("names", array(ref("Name"))))
    }

    @Test
    fun `maps objects without properties to string-keyed maps`() {
        assertEquals(MapOf(Scalar.ANY), mapper.map("map", Schema(type = JsonSchemaType.Single("object"))))
        assertEquals(
            MapOf(Scalar.ANY),
            mapper.map(
                "map",
                Schema(
                    type = JsonSchemaType.Single("object"),
                    additionalProperties = SchemaInterface.BooleanSchema(true),
                ),
            ),
        )
        assertEquals(
            MapOf(Scalar.INT32),
            mapper.map(
                "map",
                Schema(
                    type = JsonSchemaType.Single("object"),
                    additionalProperties = inline(Schema(type = JsonSchemaType.Single("integer"))),
                ),
            ),
        )
        assertEquals(
            MapOf(Named("User", isModel = true)),
            mapper.map("map", Schema(type = JsonSchemaType.Single("object"), additionalProperties = ref("User"))),
        )
        assertEquals(
            Scalar.ANY,
            mapper.map(
                "map",
                Schema(
                    type = JsonSchemaType.Single("object"),
                    additionalProperties = SchemaInterface.BooleanSchema(false),
                ),
            ),
        )
    }

    @Test
    fun `names inline enums and unions after their title or property`() {
        assertEquals(
            "OrderStatus",
            (
                mapper.map(
                    "status",
                    Schema(type = JsonSchemaType.Single("string"), title = "order status", enum = listOf("A")),
                ) as Named
            ).name,
        )
        assertEquals(
            Named("Status", isModel = false),
            mapper.map("status", Schema(type = JsonSchemaType.Single("string"), enum = listOf("A"))),
        )
        assertEquals(Named("Payment", isModel = true), mapper.map("payment", Schema(oneOf = listOf(ref("User")))))
    }

    @Test
    fun `marks references to open objects`() {
        assertEquals(
            Named("Anything", isModel = true, isOpenObject = true),
            mapper.mapReference(Reference("#/components/schemas/Anything")),
        )
        assertEquals(Named("User", isModel = true), mapper.mapReference(Reference("#/components/schemas/User")))
    }

    @Test
    fun `fails on inline objects with properties the analyzer should have promoted`() {
        assertThrows<IllegalStateException> {
            mapper.map(
                "inline",
                Schema(
                    type = JsonSchemaType.Single("object"),
                    properties =
                        mapOf("id" to inline(Schema(type = JsonSchemaType.Single("string")))),
                ),
            )
        }
    }

    @Test
    fun `cascades validation into models, list elements, and map values`() {
        assertTrue(Named("User", isModel = true).cascadesValidation)
        assertTrue(ListOf(Named("User", isModel = true)).cascadesValidation)
        assertTrue(MapOf(Named("User", isModel = true)).cascadesValidation)
        assertFalse(Named("Status", isModel = false).cascadesValidation)
        assertFalse(ListOf(Scalar.STRING).cascadesValidation)
        assertFalse(MapOf(Scalar.INT32).cascadesValidation)
        assertTrue(mapper.map("unknown", Schema(oneOf = listOf(ref("User")))).cascadesValidation)
    }

    private fun inline(schema: Schema) = SchemaInterface.SchemaInline(schema)

    private fun ref(name: String) = SchemaInterface.SchemaReference(Reference("#/components/schemas/$name"))

    private fun array(items: SchemaInterface) = Schema(type = JsonSchemaType.Single("array"), items = items)
}
