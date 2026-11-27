package dev.banking.asyncapi.generator.core.generator.input

import dev.banking.asyncapi.generator.core.fixtures.GenerationInputFixtures
import dev.banking.asyncapi.generator.core.model.schemas.SchemaFormat
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class GenerationInputFactoryTest {
    private val factory = GenerationInputFactory()
    private val fixtures = GenerationInputFixtures()

    @Test
    fun `create prepares analyzed schemas polymorphic relationships and channels`() {
        val input = factory.create(fixtures.documentWithPolymorphicComponentAndChannel())

        assertTrue(input.schemas.containsKey("Root"))
        assertTrue(input.schemas.containsKey("External"))
        assertTrue(input.schemas.containsKey("Status"))
        assertEquals(listOf("Root"), input.polymorphicRelationships["External"])
        assertTrue(input.schemaDeclarations.asyncApiSchemas.containsKey("Root"))
        assertTrue(input.schemaDeclarations.asyncApiSchemas.containsKey("UserCreatedPayload"))
        assertFalse(input.schemaDeclarations.asyncApiSchemas.containsKey("External"))

        val channel = input.channels.single()
        assertEquals("userEvents", channel.channelName)
        assertEquals("user.events", channel.topic)
        assertEquals("UserCreated", channel.messages.single().messageName)
        assertEquals(
            "UserCreatedPayload",
            channel.messages
                .single()
                .payload
                ?.typeName,
        )
    }

    @Test
    fun `create preserves multi format schemas separately from asyncapi schemas`() {
        val input = factory.create(fixtures.documentWithMultiFormatComponent())

        assertFalse(input.schemas.containsKey("UserCreated"))
        assertEquals(SchemaFormat.AVRO_1_9_0_JSON, input.multiFormatSchemas["UserCreated"]?.format)
        assertSame(input.schemaDeclarations.multiFormatSchemas, input.multiFormatSchemas)
    }
}
