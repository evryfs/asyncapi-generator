package dev.banking.asyncapi.generator.core.generator.kafka.spring

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KafkaKeyContractResolverTest {
    @Test
    fun `resolves supported scalar key schemas`() {
        val scenarios =
            listOf(
                Schema(type = "string") to ExpectedKeyType("String", "String"),
                Schema(type = "string", format = "uuid") to ExpectedKeyType("UUID", "UUID", "java.util.UUID"),
                Schema(type = "integer", format = "int32") to ExpectedKeyType("Integer", "Int"),
                Schema(type = "integer", format = "int64") to ExpectedKeyType("Long", "Long"),
                Schema(type = "number", format = "float") to ExpectedKeyType("Float", "Float"),
                Schema(type = "number", format = "double") to ExpectedKeyType("Double", "Double"),
                Schema(type = "boolean") to ExpectedKeyType("Boolean", "Boolean"),
            )

        scenarios.forEach { (schema, expected) ->
            val result =
                requireNotNull(
                    KafkaKeyContractResolver.resolve(
                        messageName = "AccountUpdated",
                        schema = SchemaInterface.SchemaInline(schema),
                    ),
                )

            assertEquals(schema, result.schema)
            assertEquals(expected.javaTypeName, result.javaTypeName)
            assertEquals(expected.kotlinTypeName, result.kotlinTypeName)
            assertEquals(expected.importName, result.importName)
            assertEquals(false, result.nullable)
        }
    }

    @Test
    fun `returns no key contract when the binding key is absent`() {
        assertNull(
            KafkaKeyContractResolver.resolve(
                messageName = "AccountUpdated",
                schema = null,
            ),
        )
    }

    @Test
    fun `preserves nullable scalar key schemas`() {
        val result =
            requireNotNull(
                KafkaKeyContractResolver.resolve(
                    messageName = "AccountUpdated",
                    schema =
                        SchemaInterface.SchemaInline(
                            Schema(type = listOf("integer", "null"), format = "int64"),
                        ),
                ),
            )

        assertEquals("Long", result.javaTypeName)
        assertEquals("Long", result.kotlinTypeName)
        assertTrue(result.nullable)
    }

    @Test
    fun `resolves referenced scalar key schemas`() {
        val referencedSchema = Schema(type = "string", format = "uuid")
        val result =
            requireNotNull(
                KafkaKeyContractResolver.resolve(
                    messageName = "AccountUpdated",
                    schema =
                        SchemaInterface.SchemaReference(
                            Reference(
                                ref = "./keys.yaml#/accountId",
                                model = referencedSchema,
                            ),
                        ),
                ),
            )

        assertEquals(referencedSchema, result.schema)
        assertEquals("UUID", result.javaTypeName)
        assertEquals("UUID", result.kotlinTypeName)
    }

    @Test
    fun `resolves inline object key schema to generated model type`() {
        val schema =
            Schema(
                type = "object",
                description = "Composite account key.",
                properties =
                    mapOf(
                        "institutionId" to SchemaInterface.SchemaInline(Schema(type = "string")),
                        "accountId" to SchemaInterface.SchemaInline(Schema(type = "string")),
                    ),
            )

        val result =
            requireNotNull(
                KafkaKeyContractResolver.resolve(
                    messageName = "AccountUpdated",
                    schema = SchemaInterface.SchemaInline(schema),
                    modelPackage = "com.example.account.model",
                ),
            )

        assertEquals(schema, result.schema)
        assertEquals("AccountUpdatedKey", result.javaTypeName)
        assertEquals("AccountUpdatedKey", result.kotlinTypeName)
        assertEquals("com.example.account.model.AccountUpdatedKey", result.importName)
        assertTrue(result.isModel)
    }

    private data class ExpectedKeyType(
        val javaTypeName: String,
        val kotlinTypeName: String,
        val importName: String? = null,
    )
}
