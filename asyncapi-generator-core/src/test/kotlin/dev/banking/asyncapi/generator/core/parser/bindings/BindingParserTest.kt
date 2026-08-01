package dev.banking.asyncapi.generator.core.parser.bindings

import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.BINDING
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
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
    fun `parse binding preserves plain nested and null values`() {
        val channelBindingsNode = readNode(
            "parser/bindings/asyncapi_parser_bindings_valid.yaml",
            "components",
            "channelBindings",
        )

        val bindings = parser.parseMap(channelBindingsNode)
        val binding = (bindings.getValue("plainChannel") as BindingInterface.BindingInline).binding

        assertEquals(
            mapOf(
                "custom" to mapOf(
                    "enabled" to true,
                    "attempts" to 3,
                    "values" to listOf("primary", 7, false, null),
                    "metadata" to mapOf("nullable" to null),
                ),
            ),
            binding.content,
        )
    }

    @Test
    fun `parse binding reference preserves category`() {
        val channelBindingsNode = readNode(
            "parser/bindings/asyncapi_parser_bindings_valid.yaml",
            "components",
            "channelBindings",
        )

        val bindings = parser.parseMap(channelBindingsNode)
        val reference = (bindings.getValue("referencedChannel") as BindingInterface.BindingReference).reference

        assertEquals("#/components/channelBindings/userSignedUpChannel", reference.ref)
        assertEquals(BINDING, reference.referenceCategoryKey)
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
    fun `parse Kafka key schema from message binding`() {
        val messageBindingsNode = readNode(
            "parser/bindings/asyncapi_parser_bindings_valid.yaml",
            "components",
            "messageBindings",
        )

        val bindings = parser.parseMap(messageBindingsNode)
        val binding = (bindings.getValue("accountUpdatedMessage") as BindingInterface.BindingInline).binding
        val keySchema = (binding.kafkaKeySchema as SchemaInterface.SchemaInline).schema

        assertThat(keySchema.type).isEqualTo("integer")
        assertThat(keySchema.format).isEqualTo("int64")
        assertThat(keySchema.description).isEqualTo("Account identifier used as the Kafka record key.")
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
    fun `parse binding reports invalid inline structure`() {
        val channelBindingsNode = readNode(
            "parser/bindings/asyncapi_parser_binding_invalid.yaml",
            "components",
            "channelBindings",
        )
        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.STRING,
            actualValue = "this-should-be-a-map",
            path = "asyncapi_parser_binding_invalid.root.components.channelBindings.InvalidBindingStructure",
            sourcePath = "root.components.channelBindings.InvalidBindingStructure",
            sourceFile = "asyncapi_parser_binding_invalid.yaml",
        ) {
            parser.parseMap(channelBindingsNode)
        }
    }

    @Test
    fun `parse binding reports explicit null ref before inline parsing`() {
        val bindingNode = readNode(
            "parser/bindings/asyncapi_parser_binding_invalid.yaml",
            "components",
            "bindingCases",
            "NullReference",
        )

        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NULL,
            actualValue = null,
            path = "asyncapi_parser_binding_invalid.root.components.bindingCases.NullReference.badBinding.\$ref",
            sourcePath = "root.components.bindingCases.NullReference.badBinding.\$ref",
            sourceFile = "asyncapi_parser_binding_invalid.yaml",
        ) {
            parser.parseMap(bindingNode)
        }
    }

    @Test
    fun `parse binding map reports list container`() {
        val bindingNode = readNode(
            "parser/bindings/asyncapi_parser_binding_invalid.yaml",
            "components",
            "bindingCases",
            "InvalidBindingsContainer",
        )

        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.ARRAY,
            actualValue = listOf(mapOf("kafka" to mapOf("topic" to "invalid"))),
            path = "asyncapi_parser_binding_invalid.root.components.bindingCases.InvalidBindingsContainer",
            sourcePath = "root.components.bindingCases.InvalidBindingsContainer",
            sourceFile = "asyncapi_parser_binding_invalid.yaml",
        ) {
            parser.parseMap(bindingNode)
        }
    }
}
