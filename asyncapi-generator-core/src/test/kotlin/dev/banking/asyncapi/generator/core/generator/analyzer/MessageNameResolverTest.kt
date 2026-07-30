package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.model.messages.Message
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MessageNameResolverTest {
    @Test
    fun `uses the message name when it is present`() {
        val message =
            Message(
                name = "AccountUpdatedV1",
                title = "Human-readable account update",
            )

        val result =
            MessageNameResolver.resolve(
                message = message,
                messageId = "accountUpdateAlias",
            )

        assertEquals("AccountUpdatedV1", result)
    }

    @Test
    fun `uses the message id when the message name is absent`() {
        val message =
            Message(
                title = "Human-readable account update",
            )

        val result =
            MessageNameResolver.resolve(
                message = message,
                messageId = "accountUpdatedV2",
            )

        assertEquals("AccountUpdatedV2", result)
    }

    @Test
    fun `uses the message id when the message name is blank`() {
        val message = Message(name = " ")

        val result =
            MessageNameResolver.resolve(
                message = message,
                messageId = "accountUpdatedV3",
            )

        assertEquals("AccountUpdatedV3", result)
    }
}
