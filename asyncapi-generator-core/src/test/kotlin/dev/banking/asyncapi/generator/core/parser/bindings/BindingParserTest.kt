package dev.banking.asyncapi.generator.core.parser.bindings

import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class BindingParserTest : ParserTestSupport() {

    private val parser = BindingParser(asyncApiContext)

    @Test
    fun `parse valid channel bindings`() {
        val channelBindingsNode = readNode(
            "parser/bindings/asyncapi_parser_bindings_valid.yaml",
            "components",
            "channelBindings",
        )
        val bindings = parser.parseMap(channelBindingsNode)
        assertTrue("userSignedUpChannel" in bindings)
        val binding = (bindings["userSignedUpChannel"] as BindingInterface.BindingInline).binding
        assertThat(binding)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(userSignedUpChannelBinding())
    }

    @Test
    fun `parse valid message bindings`() {
        val messageBindingsNode = readNode(
            "parser/bindings/asyncapi_parser_bindings_valid.yaml",
            "components",
            "messageBindings",
        )
        val bindings = parser.parseMap(messageBindingsNode)
        assertTrue("userSignedUpMessage" in bindings)
        val binding = (bindings["userSignedUpMessage"] as BindingInterface.BindingInline).binding
        assertThat(binding)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(userSignedUpMessageBinding())
    }

    @Test
    fun `parse valid server bindings`() {
        val serverBindingsNode = readNode(
            "parser/bindings/asyncapi_parser_bindings_valid.yaml",
            "components",
            "serverBindings",
        )
        val bindings = parser.parseMap(serverBindingsNode)
        assertTrue("myServerBinding" in bindings)
        val binding = (bindings["myServerBinding"] as BindingInterface.BindingInline).binding
        assertThat(binding)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(myServerBinding())
    }

    @Test
    fun `parse valid operation bindings`() {
        val operationBindingsNode = readNode(
            "parser/bindings/asyncapi_parser_bindings_valid.yaml",
            "components",
            "operationBindings",
        )
        val bindings = parser.parseMap(operationBindingsNode)
        assertTrue("myOperationBinding" in bindings)
        val binding = (bindings["myOperationBinding"] as BindingInterface.BindingInline).binding
        assertThat(binding)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(myOperationBinding())
    }

    @Test
    fun `parse binding with invalid structure throws UnexpectedValue`() {
        val channelBindingsNode = readNode(
            "parser/bindings/asyncapi_parser_binding_invalid.yaml",
            "components",
            "channelBindings",
        )
        assertParseFailure<AsyncApiParseException.UnexpectedValue>(
            "Unexpected value: expected Map",
            "asyncapi_parser_binding_invalid.yaml",
            "asyncapi_parser_binding_invalid.root.components.channelBindings.InvalidBindingStructure",
        ) {
            parser.parseMap(channelBindingsNode)
        }
    }
}
