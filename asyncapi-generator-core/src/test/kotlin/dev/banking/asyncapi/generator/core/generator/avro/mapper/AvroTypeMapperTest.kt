package dev.banking.asyncapi.generator.core.generator.avro.mapper

import dev.banking.asyncapi.generator.core.generator.avro.model.AvroType
import dev.banking.asyncapi.generator.core.generator.sourcemodel.TypeRef
import dev.banking.asyncapi.generator.core.generator.sourcemodel.UnionModel
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.UntypedAvroValue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class AvroTypeMapperTest {
    private val pet =
        UnionModel(
            name = "Pet",
            packageName = "com.example",
            description = null,
            discriminator = null,
            subtypes = emptyList(),
            members = listOf("Dog", "Cat"),
        )
    private val mapper = AvroTypeMapper("com.example.avro", listOf(pet))

    @Test
    fun `maps scalars, collections and models to Avro types`() {
        val expectations =
            mapOf(
                TypeRef.Scalar.STRING to "\"string\"",
                TypeRef.Scalar.INT64 to "\"long\"",
                TypeRef.Scalar.DECIMAL to "\"double\"",
                TypeRef.Scalar.UUID to "{\"type\": \"string\", \"logicalType\": \"uuid\"}",
                TypeRef.Scalar.DATE_TIME to "{\"type\": \"long\", \"logicalType\": \"timestamp-millis\"}",
                TypeRef.ListOf(TypeRef.Scalar.INT32) to "{\"type\": \"array\", \"items\": \"int\"}",
                TypeRef.MapOf(TypeRef.Named("Account", isModel = true)) to
                    "{\"type\": \"map\", \"values\": \"com.example.avro.Account\"}",
                TypeRef.ListOf(TypeRef.Named("Pet", isModel = true)) to
                    "{\"type\": \"array\", \"items\": [\"com.example.avro.Dog\", \"com.example.avro.Cat\"]}",
            )

        expectations.forEach { (type, json) -> assertEquals(json, mapper.map(type, "value").json(), "$type") }
    }

    @Test
    fun `a value that may be anything fails with its location`() {
        val failure =
            assertFailsWith<UntypedAvroValue> {
                mapper.map(TypeRef.MapOf(TypeRef.Scalar.ANY), "property 'details' in components.schemas.Audit")
            }

        assertEquals("property 'details' in components.schemas.Audit", failure.location)
    }

    @Test
    fun `a field typed by a union holds its members, with null first unless there is a default`() {
        val union = mapper.map(TypeRef.Named("Pet", isModel = true), "pet")

        assertEquals("[\"com.example.avro.Dog\", \"com.example.avro.Cat\"]", union.json())
        assertEquals(
            "[\"null\", \"com.example.avro.Dog\", \"com.example.avro.Cat\"]",
            AvroType.optional(union, hasDefault = false).json(),
        )
        assertEquals("[\"string\", \"null\"]", AvroType.optional(AvroType.STRING, hasDefault = true).json())
    }
}
