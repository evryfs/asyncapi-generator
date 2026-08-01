package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentBoolean
import dev.banking.asyncapi.generator.core.document.DocumentNull
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.fixtures.ReaderFixtures
import dev.banking.asyncapi.generator.core.fixtures.childObject
import dev.banking.asyncapi.generator.core.fixtures.semanticValue
import dev.banking.asyncapi.generator.core.fixtures.value
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DocumentReaderContractTest {

    private val reader = YamlDocumentReader()
    private val jsonReader = JsonDocumentReader()

    @Test
    fun `reader preserves references without resolving them`() {
        val document = reader.read(ReaderFixtures.yamlSource("reference-preservation.yaml"))
        val root = assertIs<DocumentObject>(document.root)
        val userRef = root.childObject("components")
            .childObject("schemas")
            .childObject("UserRef")
        assertEquals("#/components/schemas/User", userRef["${'$'}ref"]?.semanticValue())
    }

    @Test
    fun `reader accepts object roots without validating AsyncAPI semantics`() {
        val document = reader.read(ReaderFixtures.yamlSource("non-asyncapi-object.yaml"))
        val root = assertIs<DocumentObject>(document.root)
        assertTrue(root.members.containsKey("notAsyncApi"))
        assertTrue(root.members.containsKey("stillReaderInput"))
    }

    @Test
    fun `readers accept scalar roots without applying AsyncAPI rules`() {
        val yamlSource = ReaderFixtures.yamlSource("invalid-root.yaml").copy(content = "true")
        val jsonSource = ReaderFixtures.jsonSource("invalid-root.json").copy(content = "true")

        assertEquals(true, assertIs<DocumentBoolean>(reader.read(yamlSource).root).value)
        assertEquals(true, assertIs<DocumentBoolean>(jsonReader.read(jsonSource).root).value)
    }

    @Test
    fun `readers preserve explicit null roots`() {
        val yamlSource = ReaderFixtures.yamlSource("invalid-root.yaml").copy(content = "null")
        val jsonSource = ReaderFixtures.jsonSource("invalid-root.json").copy(content = "null")

        assertIs<DocumentNull>(reader.read(yamlSource).root)
        assertIs<DocumentNull>(jsonReader.read(jsonSource).root)
    }

    @Test
    fun `readers enforce the same encoded document size limit`() {
        val limits =
            DocumentReaderLimits.DEFAULT.copy(
                maxDocumentBytes = 16,
                maxDocumentCharacters = 16,
            )
        val yamlSource = ReaderFixtures.yamlSource("invalid-root.yaml").copy(content = "value: 1234567890")
        val jsonSource = ReaderFixtures.jsonSource("invalid-root.json").copy(content = """{"value":1234567890}""")

        assertFailsWith<DocumentReadException.ResourceLimitExceeded> {
            YamlDocumentReader(limits).read(yamlSource)
        }
        assertFailsWith<DocumentReadException.ResourceLimitExceeded> {
            JsonDocumentReader(limits).read(jsonSource)
        }
    }

    @Test
    fun `readers preserve equivalent integer range and decimal precision`() {
        val yamlSource =
            ReaderFixtures.yamlSource("equivalent-document.yaml").copy(
                content =
                    """
                    smallInteger: 42
                    longInteger: 2147483648
                    largeInteger: 9223372036854775808
                    ordinaryDecimal: 12.500
                    preciseDecimal: 0.123456789012345678901234567890
                    hugeDecimal: 10e399
                    """.trimIndent(),
            )
        val jsonSource =
            ReaderFixtures.jsonSource("equivalent-document.json").copy(
                content =
                    """
                    {
                      "smallInteger": 42,
                      "longInteger": 2147483648,
                      "largeInteger": 9223372036854775808,
                      "ordinaryDecimal": 12.5,
                      "preciseDecimal": 0.12345678901234567890123456789,
                      "hugeDecimal": 1e400
                    }
                    """.trimIndent(),
            )

        val yamlRoot = assertIs<DocumentObject>(reader.read(yamlSource).root)
        val jsonRoot = assertIs<DocumentObject>(jsonReader.read(jsonSource).root)

        assertEquals(yamlRoot.semanticValue(), jsonRoot.semanticValue())
        assertIs<Int>(yamlRoot.value("smallInteger"))
        assertIs<Long>(yamlRoot.value("longInteger"))
        assertEquals(BigInteger("9223372036854775808"), yamlRoot.value("largeInteger"))
        assertIs<Double>(yamlRoot.value("ordinaryDecimal"))
        assertEquals(
            BigDecimal("0.12345678901234567890123456789"),
            yamlRoot.value("preciseDecimal"),
        )
        assertEquals(BigDecimal("1e400"), yamlRoot.value("hugeDecimal"))
    }

    @Test
    fun `readers enforce the same numeric token limit`() {
        val limits = DocumentReaderLimits.DEFAULT.copy(maxNumberCharacters = 4)
        val yamlSource = ReaderFixtures.yamlSource("invalid-root.yaml").copy(content = "12345")
        val jsonSource = ReaderFixtures.jsonSource("invalid-root.json").copy(content = "12345")

        assertFailsWith<DocumentReadException.ResourceLimitExceeded> {
            YamlDocumentReader(limits).read(yamlSource)
        }
        assertFailsWith<DocumentReadException.ResourceLimitExceeded> {
            JsonDocumentReader(limits).read(jsonSource)
        }
    }

    @Test
    fun `yaml and json readers produce equivalent document trees`() {
        val yamlDocument = reader.read(ReaderFixtures.yamlSource("equivalent-document.yaml"))
        val jsonDocument = jsonReader.read(ReaderFixtures.jsonSource("equivalent-document.json"))

        assertEquals(yamlDocument.root.semanticValue(), jsonDocument.root.semanticValue())
    }
}
