package dev.banking.asyncapi.generator.core.parser.correlations

import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.parser.AbstractParserTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CorrelationIdParserTest : AbstractParserTest() {

    private val parser = CorrelationIdParser(asyncApiContext)

    @Test
    fun `parse inline correlation ID`() {
        val root = readYaml("src/test/resources/parser/correlations/asyncapi_parser_correlationid_valid.yaml")
        val correlationIdNode = root
            .mandatory("components")
            .mandatory("correlationIds")
            .mandatory("MyCorrelationId")
        val correlationIdInterface = parser.parseElement(correlationIdNode)
        assertTrue(correlationIdInterface is CorrelationIdInterface.CorrelationIdInline)
        assertThat(correlationIdInterface.correlationId)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*sourceId", ".*inline")
            .isEqualTo(myCorrelationId())
    }

    @Test
    fun `parse correlation ID missing location throws RequiredObject`() {
        val root = readYaml("src/test/resources/parser/correlations/asyncapi_parser_correlationid_invalid.yaml")
        val correlationIdNode = root.mandatory("components").mandatory("correlationIds").mandatory("MissingLocationId")
        assertFailsWith<AsyncApiParseException.Mandatory> {
            parser.parseElement(correlationIdNode)
        }
    }
}
