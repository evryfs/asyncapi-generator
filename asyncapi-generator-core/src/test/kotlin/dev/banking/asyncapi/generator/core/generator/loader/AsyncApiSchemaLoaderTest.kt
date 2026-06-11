package dev.banking.asyncapi.generator.core.generator.loader

import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaFormat
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AsyncApiSchemaLoaderTest {

    @Test
    fun `should load explicit schemas`() {
        val components = Component(
            schemas = mapOf(
                "User" to SchemaInterface.SchemaInline(Schema(type = "object"))
            )
        )
        val doc = docWithComponents(components)
        val loaded = AsyncApiSchemaLoader.load(doc)
        assertTrue(loaded.containsKey("User"))
    }

    @Test
    fun `should harvest schemas from message payloads`() {
        val components = Component(
            messages = mapOf(
                "UserSignedUp" to MessageInterface.MessageInline(
                    Message(
                        payload = SchemaInterface.SchemaInline(Schema(type = "object"))
                    )
                )
            )
        )
        val doc = docWithComponents(components)
        val loaded = AsyncApiSchemaLoader.load(doc)
        assertTrue(loaded.containsKey("UserSignedUpPayload"))
    }

    @Test
    fun `should load explicit multi format schemas separately from asyncapi schemas`() {
        val avroSchema = nativeAvroSchema()
        val components = Component(
            schemas = mapOf(
                "UserCreated" to SchemaInterface.MultiFormatSchemaInline(avroSchema),
            ),
        )
        val doc = docWithComponents(components)

        val loadedSchemas = AsyncApiSchemaLoader.load(doc)
        val loadedMultiFormatSchemas = AsyncApiSchemaLoader.loadMultiFormatSchemas(doc)

        assertFalse(loadedSchemas.containsKey("UserCreated"))
        assertSame(avroSchema, loadedMultiFormatSchemas["UserCreated"])
        assertEquals(SchemaFormat.AVRO_1_9_0_JSON, loadedMultiFormatSchemas["UserCreated"]?.format)
    }

    @Test
    fun `should harvest multi format schemas from message payloads`() {
        val avroSchema = nativeAvroSchema()
        val components = Component(
            messages = mapOf(
                "UserSignedUp" to MessageInterface.MessageInline(
                    Message(
                        payload = SchemaInterface.MultiFormatSchemaInline(avroSchema),
                    ),
                ),
            ),
        )
        val doc = docWithComponents(components)

        val loadedMultiFormatSchemas = AsyncApiSchemaLoader.loadMultiFormatSchemas(doc)

        assertSame(avroSchema, loadedMultiFormatSchemas["UserSignedUpPayload"])
    }

    private fun docWithComponents(component: Component): AsyncApiDocument {
        return AsyncApiDocument(
            asyncapi = "3.0.0",
            info = dev.banking.asyncapi.generator.core.model.info.Info("T", "1"),
            components = ComponentInterface.ComponentInline(component)
        )
    }

    private fun nativeAvroSchema(): MultiFormatSchema =
        MultiFormatSchema(
            schemaFormat = "application/vnd.apache.avro+json;version=1.9.0",
            schema = mapOf("type" to "record", "name" to "UserCreated", "fields" to emptyList<Any>()),
        )
}
