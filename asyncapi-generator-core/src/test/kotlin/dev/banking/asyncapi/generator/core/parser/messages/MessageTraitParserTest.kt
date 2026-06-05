package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageTraitParserTest : ParserTestSupport() {

    private val parser = MessageTraitParser(asyncApiContext)

    @Test
    fun `parse message trait list`() {
        val traitsNode = readNode(
            "parser/messages/asyncapi_parser_message_edge_cases.yaml",
            "components",
            "messages",
            "InlineTraitMessage",
            "traits",
        )

        val traits = parser.parseList(traitsNode)

        assertEquals(1, traits.size)
        assertTrue(traits.first() is MessageTraitInterface.InlineMessageTrait)
        val trait = (traits.first() as MessageTraitInterface.InlineMessageTrait).trait
        assertTrue(trait.headers is SchemaInterface.SchemaInline)
        assertEquals("string", (trait.headers as SchemaInterface.SchemaInline).schema.type)
    }

    @Test
    fun `parse message trait with invalid structure throws UnexpectedValue`() {
        val traitsNode = readNode(
            "parser/messages/asyncapi_parser_message_trait_invalid.yaml",
            "components",
            "messageTraitCases",
            "InvalidTraitStructure",
        )
        assertParseFailure<AsyncApiParseException.UnexpectedValue>(
            "Unexpected value: expected Map",
            "asyncapi_parser_message_trait_invalid.yaml",
            "asyncapi_parser_message_trait_invalid.root.components.messageTraitCases.InvalidTraitStructure.badTrait",
        ) {
            parser.parseMap(traitsNode)
        }
    }

    @Test
    fun `parse message trait with boolean content type throws UnexpectedValue`() {
        val traitsNode = readNode(
            "parser/messages/asyncapi_parser_message_trait_invalid.yaml",
            "components",
            "messageTraitCases",
            "BooleanContentType",
        )
        assertParseFailure<AsyncApiParseException.UnexpectedValue>(
            "Unexpected value: expected String",
            "found Boolean true",
            "quote the value",
            "asyncapi_parser_message_trait_invalid.yaml",
            "asyncapi_parser_message_trait_invalid.root.components.messageTraitCases.BooleanContentType.badTrait.contentType",
        ) {
            parser.parseMap(traitsNode)
        }
    }

    @Test
    fun `parse message trait with invalid example structure throws UnexpectedValue`() {
        val traitsNode = readNode(
            "parser/messages/asyncapi_parser_message_trait_invalid.yaml",
            "components",
            "messageTraitCases",
            "InvalidExampleStructure",
        )
        assertParseFailure<AsyncApiParseException.UnexpectedValue>(
            "Unexpected value: expected Map",
            "asyncapi_parser_message_trait_invalid.yaml",
            "asyncapi_parser_message_trait_invalid.root.components.messageTraitCases.InvalidExampleStructure.badTrait.examples[0]",
        ) {
            parser.parseMap(traitsNode)
        }
    }
}
