package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OperationReplyAddressParserTest : ParserTestSupport() {

    private val parser = OperationReplyAddressParser(asyncApiContext)

    @Test
    fun `parse operation reply address`() {
        val addressNode = readNode(
            "parser/operations/asyncapi_parser_operations_valid.yaml",
            "operations",
            "receiveLightMeasurement",
            "reply",
            "address",
        )

        val address = parser.parseElement(addressNode)

        assertTrue(address is OperationReplyAddressInterface.OperationReplyAddressInline)
        assertEquals($$"$message.header#/replyTo", address.operationReplyAddress.location)
    }

    @Test
    fun `parse operation reply address missing location reports the required member and source`() {
        val addressNode = readNode(
            "parser/operations/asyncapi_parser_operation_reply_address_invalid.yaml",
            "components",
            "operationReplyAddressCases",
            "MissingLocation",
        )
        assertMissingRequiredMember(
            memberName = "location",
            path = "asyncapi_parser_operation_reply_address_invalid.root.components.operationReplyAddressCases.MissingLocation.location",
            sourcePath = "root.components.operationReplyAddressCases.MissingLocation",
            sourceFile = "asyncapi_parser_operation_reply_address_invalid.yaml",
        ) {
            parser.parseElement(addressNode)
        }
    }

    @Test
    fun `parse operation reply address with boolean location reports its expected type and source`() {
        val addressNode = readNode(
            "parser/operations/asyncapi_parser_operation_reply_address_invalid.yaml",
            "components",
            "operationReplyAddressCases",
            "BooleanLocation",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.BOOLEAN,
            actualValue = true,
            path = "asyncapi_parser_operation_reply_address_invalid.root.components.operationReplyAddressCases.BooleanLocation.location",
            sourcePath = "root.components.operationReplyAddressCases.BooleanLocation.location",
            sourceFile = "asyncapi_parser_operation_reply_address_invalid.yaml",
        ) {
            parser.parseElement(addressNode)
        }
    }

    @Test
    fun `parse operation reply address with null reference reports its expected type and source`() {
        val addressNode = readNode(
            "parser/operations/asyncapi_parser_operation_reply_address_invalid.yaml",
            "components",
            "operationReplyAddressCases",
            "NullReference",
        )
        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NULL,
            actualValue = null,
            path = "asyncapi_parser_operation_reply_address_invalid.root.components.operationReplyAddressCases.NullReference.\$ref",
            sourcePath = "root.components.operationReplyAddressCases.NullReference.\$ref",
            sourceFile = "asyncapi_parser_operation_reply_address_invalid.yaml",
        ) {
            parser.parseElement(addressNode)
        }
    }

    @Test
    fun `parse operation reply address map from an array reports the container type and source`() {
        val addressesNode = readNode(
            "parser/operations/asyncapi_parser_operation_reply_address_invalid.yaml",
            "components",
            "operationReplyAddressCases",
            "ArrayInsteadOfMap",
        )
        assertUnexpectedValueType(
            expectedType = "Map<String, Any?>",
            actualType = ParserValueType.ARRAY,
            actualValue = listOf(mapOf("location" to "\$message.header#/replyTo")),
            path = "asyncapi_parser_operation_reply_address_invalid.root.components.operationReplyAddressCases.ArrayInsteadOfMap",
            sourcePath = "root.components.operationReplyAddressCases.ArrayInsteadOfMap",
            sourceFile = "asyncapi_parser_operation_reply_address_invalid.yaml",
        ) {
            parser.parseMap(addressesNode)
        }
    }
}
