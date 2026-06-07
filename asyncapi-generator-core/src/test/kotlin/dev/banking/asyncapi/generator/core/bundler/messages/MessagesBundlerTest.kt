package dev.banking.asyncapi.generator.core.bundler.messages

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

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

        assertThat(bundled).isSameAs(messageInterface)
        assertThat(messageReference.inline).isTrue()
        assertThat(messageReference.model).isInstanceOf(Message::class.java)
        assertThat(((messageReference.model as Message).payload as SchemaInterface.SchemaReference).reference.inline).isTrue()
    }

    @Test
    fun `bundle keeps a visited message reference unchanged`() {
        val message = Message(name = "userUpdated")
        val messageReference = Reference("#/components/messages/userUpdated", model = message)
        val messageInterface = MessageInterface.MessageReference(messageReference)

        val bundled = bundler.bundle(messageInterface, BundlingContext.empty().enter(messageReference))

        assertThat(bundled).isSameAs(messageInterface)
        assertThat(messageReference.inline).isFalse()
        assertThat(messageReference.model).isSameAs(message)
    }
}
