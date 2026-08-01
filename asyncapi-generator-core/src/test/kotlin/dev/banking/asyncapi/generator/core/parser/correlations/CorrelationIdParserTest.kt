package dev.banking.asyncapi.generator.core.parser.correlations

import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
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
    fun `parse correlation ID missing location throws RequiredObject`() {
        val correlationIdNode = readNode(
            "parser/correlations/asyncapi_parser_correlationid_invalid.yaml",
            "components",
            "correlationIds",
            "MissingLocationId",
        )
        assertParseFailure<AsyncApiParseException.Mandatory>(
            "Missing mandatory 'location'",
            "asyncapi_parser_correlationid_invalid.yaml",
            "asyncapi_parser_correlationid_invalid.root.components.correlationIds.MissingLocationId.location",
        ) {
            parser.parseElement(correlationIdNode)
        }
    }
}
