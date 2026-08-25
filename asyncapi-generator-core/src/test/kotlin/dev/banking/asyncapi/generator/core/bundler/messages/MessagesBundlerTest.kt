package dev.banking.asyncapi.generator.core.bundler.messages

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.junit.jupiter.api.Test
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MessagesBundlerTest {

    private val bundler = MessagesBundler()

    @Test
    fun `bundle bundles and inlines an unvisited message reference`() {
        val payloadReference = Reference("#/components/schemas/User", model = Schema(type = "object"))
        val message = Message(
            name = "userUpdated",
            payload = SchemaInterface.SchemaReference(payloadReference),
        )
        val messageReference = Reference("#/components/messages/userUpdated", model = message)
        val messageInterface = MessageInterface.MessageReference(messageReference)

        val bundled = bundler.bundle(messageInterface, BundlingContext.empty())

        assertSame(messageInterface, bundled)
        assertTrue(messageReference.inline)
        assertIs<Message>(messageReference.model)
        assertTrue(((messageReference.model as Message).payload as SchemaInterface.SchemaReference).reference.inline)
    }
}
