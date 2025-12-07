package dev.banking.asyncapi.generator.core.parser.bindings

import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.parser.AbstractParserTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BindingParserTest : AbstractParserTest() {

    private val parser = BindingParser(asyncApiContext)

    @Test
    fun `parse valid channel bindings`() {
        val root = readYaml("src/test/resources/parser/bindings/asyncapi_parser_bindings_valid.yaml")
        val bindings = parser.parseMap(root.mandatory("components").mandatory("channelBindings"))
        assertTrue("userSignedUpChannel" in bindings)
        val binding = (bindings["userSignedUpChannel"] as BindingInterface.BindingInline).binding
        assertThat(binding)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(userSignedUpChannelBinding())
    }

    @Test
    fun `parse valid message bindings`() {
        val root = readYaml("src/test/resources/parser/bindings/asyncapi_parser_bindings_valid.yaml")
        val bindings = parser.parseMap(root.mandatory("components").mandatory("messageBindings"))
        assertTrue("userSignedUpMessage" in bindings)
        val binding = (bindings["userSignedUpMessage"] as BindingInterface.BindingInline).binding
        assertThat(binding)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(userSignedUpMessageBinding())
    }

    @Test
    fun `parse valid server bindings`() {
        val root = readYaml("src/test/resources/parser/bindings/asyncapi_parser_bindings_valid.yaml")
        val bindings = parser.parseMap(root.mandatory("components").mandatory("serverBindings"))
        assertTrue("myServerBinding" in bindings)
        val binding = (bindings["myServerBinding"] as BindingInterface.BindingInline).binding
        assertThat(binding)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(myServerBinding())
    }

    @Test
    fun `parse valid operation bindings`() {
        val root = readYaml("src/test/resources/parser/bindings/asyncapi_parser_bindings_valid.yaml")
        val bindings = parser.parseMap(root.mandatory("components").mandatory("operationBindings"))
        assertTrue("myOperationBinding" in bindings)
        val binding = (bindings["myOperationBinding"] as BindingInterface.BindingInline).binding
        assertThat(binding)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(myOperationBinding())
    }

    @Test
    fun `parse binding with invalid structure throws UnexpectedValue`() {
        val root = readYaml("src/test/resources/parser/bindings/asyncapi_parser_binding_invalid.yaml")
        assertFailsWith<AsyncApiParseException.UnexpectedValue> {
            parser.parseMap(root.mandatory("components").mandatory("channelBindings"))
        }
    }
}
