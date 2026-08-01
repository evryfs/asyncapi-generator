package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentArray
import dev.banking.asyncapi.generator.core.document.DocumentBoolean
import dev.banking.asyncapi.generator.core.document.DocumentNull
import dev.banking.asyncapi.generator.core.document.DocumentNumber
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.document.DocumentString
import dev.banking.asyncapi.generator.core.fixtures.ReaderFixtures
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
        val components = assertIs<DocumentObject>(root["components"])
        val schemas = assertIs<DocumentObject>(components["schemas"])
        val userRef = assertIs<DocumentObject>(schemas["UserRef"])
        assertEquals(
            "#/components/schemas/User",
            assertIs<DocumentString>(userRef["${'$'}ref"]).value,
        )
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

        val yamlSmallInteger = assertIs<DocumentNumber>(yamlRoot["smallInteger"]).value
        val jsonSmallInteger = assertIs<DocumentNumber>(jsonRoot["smallInteger"]).value
        assertEquals(yamlSmallInteger, jsonSmallInteger)
        assertIs<Int>(yamlSmallInteger)

        val yamlLongInteger = assertIs<DocumentNumber>(yamlRoot["longInteger"]).value
        val jsonLongInteger = assertIs<DocumentNumber>(jsonRoot["longInteger"]).value
        assertEquals(yamlLongInteger, jsonLongInteger)
        assertIs<Long>(yamlLongInteger)

        val yamlLargeInteger = assertIs<DocumentNumber>(yamlRoot["largeInteger"]).value
        val jsonLargeInteger = assertIs<DocumentNumber>(jsonRoot["largeInteger"]).value
        assertEquals(yamlLargeInteger, jsonLargeInteger)
        assertEquals(BigInteger("9223372036854775808"), yamlLargeInteger)

        val yamlOrdinaryDecimal = assertIs<DocumentNumber>(yamlRoot["ordinaryDecimal"]).value
        val jsonOrdinaryDecimal = assertIs<DocumentNumber>(jsonRoot["ordinaryDecimal"]).value
        assertEquals(yamlOrdinaryDecimal, jsonOrdinaryDecimal)
        assertIs<Double>(yamlOrdinaryDecimal)

        val yamlPreciseDecimal = assertIs<DocumentNumber>(yamlRoot["preciseDecimal"]).value
        val jsonPreciseDecimal = assertIs<DocumentNumber>(jsonRoot["preciseDecimal"]).value
        assertEquals(yamlPreciseDecimal, jsonPreciseDecimal)
        assertEquals(
            BigDecimal("0.12345678901234567890123456789"),
            yamlPreciseDecimal,
        )

        val yamlHugeDecimal = assertIs<DocumentNumber>(yamlRoot["hugeDecimal"]).value
        val jsonHugeDecimal = assertIs<DocumentNumber>(jsonRoot["hugeDecimal"]).value
        assertEquals(yamlHugeDecimal, jsonHugeDecimal)
        assertEquals(BigDecimal("1e400"), yamlHugeDecimal)
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

        val yamlRoot = assertIs<DocumentObject>(yamlDocument.root)
        val jsonRoot = assertIs<DocumentObject>(jsonDocument.root)
        assertEquals(yamlRoot.members.keys, jsonRoot.members.keys)
        assertEquals(
            assertIs<DocumentString>(yamlRoot["asyncapi"]).value,
            assertIs<DocumentString>(jsonRoot["asyncapi"]).value,
        )

        val yamlInfo = assertIs<DocumentObject>(yamlRoot["info"])
        val jsonInfo = assertIs<DocumentObject>(jsonRoot["info"])
        assertEquals(yamlInfo.members.keys, jsonInfo.members.keys)
        assertEquals(
            assertIs<DocumentString>(yamlInfo["title"]).value,
            assertIs<DocumentString>(jsonInfo["title"]).value,
        )

        val yamlComponents = assertIs<DocumentObject>(yamlRoot["components"])
        val jsonComponents = assertIs<DocumentObject>(jsonRoot["components"])
        val yamlSchemas = assertIs<DocumentObject>(yamlComponents["schemas"])
        val jsonSchemas = assertIs<DocumentObject>(jsonComponents["schemas"])
        assertEquals(yamlSchemas.members.keys, jsonSchemas.members.keys)

        val yamlUserRef = assertIs<DocumentObject>(yamlSchemas["UserRef"])
        val jsonUserRef = assertIs<DocumentObject>(jsonSchemas["UserRef"])
        assertEquals(
            assertIs<DocumentString>(yamlUserRef["${'$'}ref"]).value,
            assertIs<DocumentString>(jsonUserRef["${'$'}ref"]).value,
        )

        val yamlExample = assertIs<DocumentObject>(yamlSchemas["Example"])
        val jsonExample = assertIs<DocumentObject>(jsonSchemas["Example"])
        assertEquals(yamlExample.members.keys, jsonExample.members.keys)
        assertEquals(
            assertIs<DocumentBoolean>(yamlExample["enabled"]).value,
            assertIs<DocumentBoolean>(jsonExample["enabled"]).value,
        )
        assertEquals(
            assertIs<DocumentString>(yamlExample["quotedEnabled"]).value,
            assertIs<DocumentString>(jsonExample["quotedEnabled"]).value,
        )
        assertEquals(
            assertIs<DocumentNumber>(yamlExample["count"]).value,
            assertIs<DocumentNumber>(jsonExample["count"]).value,
        )
        assertEquals(
            assertIs<DocumentString>(yamlExample["quotedCount"]).value,
            assertIs<DocumentString>(jsonExample["quotedCount"]).value,
        )

        val yamlTags = assertIs<DocumentArray>(yamlExample["tags"])
        val jsonTags = assertIs<DocumentArray>(jsonExample["tags"])
        assertEquals(yamlTags.elements.size, jsonTags.elements.size)
        assertEquals(
            assertIs<DocumentString>(yamlTags[0]).value,
            assertIs<DocumentString>(jsonTags[0]).value,
        )
        assertEquals(
            assertIs<DocumentString>(yamlTags[1]).value,
            assertIs<DocumentString>(jsonTags[1]).value,
        )
    }
}
