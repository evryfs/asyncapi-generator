package com.tietoevry.banking.asyncapi.generator.core.generator.loader

import com.tietoevry.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import com.tietoevry.banking.asyncapi.generator.core.model.components.Component
import com.tietoevry.banking.asyncapi.generator.core.model.components.ComponentInterface
import com.tietoevry.banking.asyncapi.generator.core.model.messages.Message
import com.tietoevry.banking.asyncapi.generator.core.model.messages.MessageInterface
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
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
        assertTrue(loaded.containsKey("UserSignedUp"))
    }

    private fun docWithComponents(component: Component): AsyncApiDocument {
        return AsyncApiDocument(
            asyncapi = "3.0.0",
            info = com.tietoevry.banking.asyncapi.generator.core.model.info.Info("T", "1"),
            components = ComponentInterface.ComponentInline(component)
        )
    }
}
