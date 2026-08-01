package dev.banking.asyncapi.generator.core.parser.correlations

import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserValueType
import dev.banking.asyncapi.generator.core.parser.ParserTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class CorrelationIdParserTest : ParserTestSupport() {

    private val parser = CorrelationIdParser(asyncApiContext)

    @Test
    fun `parse inline correlation ID`() {
        val correlationIdNode = readNode(
            "parser/correlations/asyncapi_parser_correlationid_valid.yaml",
            "components",
            "correlationIds",
            "MyCorrelationId",
        )
        val correlationIdInterface = parser.parseElement(correlationIdNode)
        assertTrue(correlationIdInterface is CorrelationIdInterface.CorrelationIdInline)
        assertThat(correlationIdInterface.correlationId)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(myCorrelationId())
    }

    @Test
    fun `parse correlation ID reports missing location`() {
        val correlationIdNode = readNode(
            "parser/correlations/asyncapi_parser_correlationid_invalid.yaml",
            "components",
            "correlationIds",
            "MissingLocationId",
        )
        assertMissingRequiredMember(
            memberName = "location",
            path = "asyncapi_parser_correlationid_invalid.root.components.correlationIds.MissingLocationId.location",
            sourcePath = "root.components.correlationIds.MissingLocationId",
            sourceFile = "asyncapi_parser_correlationid_invalid.yaml",
        ) {
            parser.parseElement(correlationIdNode)
        }
    }

    @Test
    fun `parse correlation ID reports non-string ref before inline parsing`() {
        val correlationIdNode = readNode(
            "parser/correlations/asyncapi_parser_correlationid_invalid.yaml",
            "components",
            "correlationIds",
            "NumericReference",
        )

        assertUnexpectedValueType(
            expectedType = "String",
            actualType = ParserValueType.NUMBER,
            actualValue = 42,
            path = "asyncapi_parser_correlationid_invalid.root.components.correlationIds.NumericReference.\$ref",
            sourcePath = "root.components.correlationIds.NumericReference.\$ref",
            sourceFile = "asyncapi_parser_correlationid_invalid.yaml",
        ) {
            parser.parseElement(correlationIdNode)
        }
    }
}
