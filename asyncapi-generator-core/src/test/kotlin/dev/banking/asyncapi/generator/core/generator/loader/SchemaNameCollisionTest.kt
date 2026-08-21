package dev.banking.asyncapi.generator.core.generator.loader

import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.SchemaNameCollision
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SchemaNameCollisionTest {

    @Test
    fun `rejects component schemas that normalize to the same name`() {
        val error =
            assertFailsWith<SchemaNameCollision> {
                AsyncApiSchemaLoader.load(
                    documentWithSchemas("order-item", "order_item"),
                )
            }

        assertTrue(error.message!!.contains("'order-item'"))
        assertTrue(error.message!!.contains("'order_item'"))
        assertTrue(error.message!!.contains("OrderItem"))
    }

    @Test
    fun `allows distinct component schemas`() {
        val result = AsyncApiSchemaLoader.load(
            documentWithSchemas("order-item", "payment-method"),
        )

        assertTrue(result.schemas.containsKey("OrderItem"))
        assertTrue(result.schemas.containsKey("PaymentMethod"))
    }

    @Test
    fun `allows duplicate component names`() {
        val result = AsyncApiSchemaLoader.load(
            documentWithSchemas("order-item", "order-item"),
        )

        assertTrue(result.schemas.containsKey("OrderItem"))
    }

    private fun documentWithSchemas(vararg names: String): AsyncApiDocument {
        val schemas = linkedMapOf<String, SchemaInterface>()
        names.forEach { name ->
            schemas[name] = SchemaInterface.SchemaInline(
                Schema(type = "object"),
            )
        }
        return AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info(title = "Test", version = "1.0.0"),
            components =
                ComponentInterface.ComponentInline(
                    Component(schemas = schemas),
                ),
        )
    }
}
