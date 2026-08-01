package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentArray
import dev.banking.asyncapi.generator.core.document.DocumentBoolean
import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentNull
import dev.banking.asyncapi.generator.core.document.DocumentNumber
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.document.DocumentSource
import dev.banking.asyncapi.generator.core.document.DocumentString
import dev.banking.asyncapi.generator.core.fixtures.TestResources
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
        val file = TestResources.file("reader/yaml/reference-preservation.yaml")
        val source = DocumentSource(
            id = "reference-preservation",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )
        val document = reader.read(source)
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
        val file = TestResources.file("reader/yaml/non-asyncapi-object.yaml")
        val source = DocumentSource(
            id = "non-asyncapi-object",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )
        val root = assertIs<DocumentObject>(reader.read(source).root)

        assertTrue(root.members.containsKey("notAsyncApi"))
        assertTrue(root.members.containsKey("stillReaderInput"))
    }

    @Test
    fun `readers accept scalar roots without applying AsyncAPI rules`() {
        val yamlFile = TestResources.file("reader/yaml/invalid-root.yaml")
        val yamlSource = DocumentSource(
            id = "invalid-root",
            file = yamlFile,
            content = "true",
            format = DocumentFormat.YAML,
        )
        val jsonFile = TestResources.file("reader/json/invalid-root.json")
        val jsonSource = DocumentSource(
            id = "invalid-root",
            file = jsonFile,
            content = "true",
            format = DocumentFormat.JSON,
        )

        assertEquals(true, assertIs<DocumentBoolean>(reader.read(yamlSource).root).value)
        assertEquals(true, assertIs<DocumentBoolean>(jsonReader.read(jsonSource).root).value)
    }

    @Test
    fun `readers preserve explicit null roots`() {
        val yamlFile = TestResources.file("reader/yaml/invalid-root.yaml")
        val yamlSource = DocumentSource(
            id = "invalid-root",
            file = yamlFile,
            content = "null",
            format = DocumentFormat.YAML,
        )
        val jsonFile = TestResources.file("reader/json/invalid-root.json")
        val jsonSource = DocumentSource(
            id = "invalid-root",
            file = jsonFile,
            content = "null",
            format = DocumentFormat.JSON,
        )

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
        val yamlFile = TestResources.file("reader/yaml/invalid-root.yaml")
        val yamlSource = DocumentSource(
            id = "invalid-root",
            file = yamlFile,
            content = "value: 1234567890",
            format = DocumentFormat.YAML,
        )
        val jsonFile = TestResources.file("reader/json/invalid-root.json")
        val jsonSource = DocumentSource(
            id = "invalid-root",
            file = jsonFile,
            content = """{"value":1234567890}""",
            format = DocumentFormat.JSON,
        )

        assertFailsWith<DocumentReadException.ResourceLimitExceeded> {
            YamlDocumentReader(limits).read(yamlSource)
        }
        assertFailsWith<DocumentReadException.ResourceLimitExceeded> {
            JsonDocumentReader(limits).read(jsonSource)
        }
    }

    @Test
    fun `readers preserve equivalent integer range and decimal precision`() {
        val yamlFile = TestResources.file("reader/yaml/equivalent-document.yaml")
        val yamlSource = DocumentSource(
            id = "equivalent-document",
            file = yamlFile,
            content =
                """
                smallInteger: 42
                longInteger: 2147483648
                largeInteger: 9223372036854775808
                ordinaryDecimal: 12.500
                preciseDecimal: 0.123456789012345678901234567890
                hugeDecimal: 10e399
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )
        val jsonFile = TestResources.file("reader/json/equivalent-document.json")
        val jsonSource = DocumentSource(
            id = "equivalent-document",
            file = jsonFile,
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
            format = DocumentFormat.JSON,
        )

        val yamlRoot = assertIs<DocumentObject>(reader.read(yamlSource).root)
        val jsonRoot = assertIs<DocumentObject>(jsonReader.read(jsonSource).root)

        assertEquals(42, assertIs<Int>(assertIs<DocumentNumber>(yamlRoot["smallInteger"]).value))
        assertEquals(42, assertIs<Int>(assertIs<DocumentNumber>(jsonRoot["smallInteger"]).value))

        assertEquals(2147483648L, assertIs<Long>(assertIs<DocumentNumber>(yamlRoot["longInteger"]).value))
        assertEquals(2147483648L, assertIs<Long>(assertIs<DocumentNumber>(jsonRoot["longInteger"]).value))

        val largeInteger = BigInteger("9223372036854775808")
        assertEquals(largeInteger, assertIs<BigInteger>(assertIs<DocumentNumber>(yamlRoot["largeInteger"]).value))
        assertEquals(largeInteger, assertIs<BigInteger>(assertIs<DocumentNumber>(jsonRoot["largeInteger"]).value))

        assertEquals(12.5, assertIs<Double>(assertIs<DocumentNumber>(yamlRoot["ordinaryDecimal"]).value))
        assertEquals(12.5, assertIs<Double>(assertIs<DocumentNumber>(jsonRoot["ordinaryDecimal"]).value))

        val preciseDecimal = BigDecimal("0.12345678901234567890123456789")
        assertEquals(
            preciseDecimal,
            assertIs<BigDecimal>(assertIs<DocumentNumber>(yamlRoot["preciseDecimal"]).value),
        )
        assertEquals(
            preciseDecimal,
            assertIs<BigDecimal>(assertIs<DocumentNumber>(jsonRoot["preciseDecimal"]).value),
        )

        val hugeDecimal = BigDecimal("1e400")
        assertEquals(hugeDecimal, assertIs<BigDecimal>(assertIs<DocumentNumber>(yamlRoot["hugeDecimal"]).value))
        assertEquals(hugeDecimal, assertIs<BigDecimal>(assertIs<DocumentNumber>(jsonRoot["hugeDecimal"]).value))
    }

    @Test
    fun `readers enforce the same numeric token limit`() {
        val limits = DocumentReaderLimits.DEFAULT.copy(maxNumberCharacters = 4)
        val yamlFile = TestResources.file("reader/yaml/invalid-root.yaml")
        val yamlSource = DocumentSource(
            id = "invalid-root",
            file = yamlFile,
            content = "12345",
            format = DocumentFormat.YAML,
        )
        val jsonFile = TestResources.file("reader/json/invalid-root.json")
        val jsonSource = DocumentSource(
            id = "invalid-root",
            file = jsonFile,
            content = "12345",
            format = DocumentFormat.JSON,
        )

        assertFailsWith<DocumentReadException.ResourceLimitExceeded> {
            YamlDocumentReader(limits).read(yamlSource)
        }
        assertFailsWith<DocumentReadException.ResourceLimitExceeded> {
            JsonDocumentReader(limits).read(jsonSource)
        }
    }

    @Test
    fun `yaml and json readers produce equivalent document trees`() {
        val yamlFile = TestResources.file("reader/yaml/equivalent-document.yaml")
        val yamlSource = DocumentSource(
            id = "equivalent-document",
            file = yamlFile,
            content = yamlFile.readText(),
            format = DocumentFormat.YAML,
        )
        val jsonFile = TestResources.file("reader/json/equivalent-document.json")
        val jsonSource = DocumentSource(
            id = "equivalent-document",
            file = jsonFile,
            content = jsonFile.readText(),
            format = DocumentFormat.JSON,
        )

        val yamlRoot = assertIs<DocumentObject>(reader.read(yamlSource).root)
        val jsonRoot = assertIs<DocumentObject>(jsonReader.read(jsonSource).root)
        assertEquals(listOf("asyncapi", "info", "components"), yamlRoot.members.keys.toList())
        assertEquals(listOf("asyncapi", "info", "components"), jsonRoot.members.keys.toList())
        assertEquals("3.0.0", assertIs<DocumentString>(yamlRoot["asyncapi"]).value)
        assertEquals("3.0.0", assertIs<DocumentString>(jsonRoot["asyncapi"]).value)

        val yamlInfo = assertIs<DocumentObject>(yamlRoot["info"])
        val jsonInfo = assertIs<DocumentObject>(jsonRoot["info"])
        assertEquals(listOf("title"), yamlInfo.members.keys.toList())
        assertEquals(listOf("title"), jsonInfo.members.keys.toList())
        assertEquals("Demo API", assertIs<DocumentString>(yamlInfo["title"]).value)
        assertEquals("Demo API", assertIs<DocumentString>(jsonInfo["title"]).value)

        val yamlComponents = assertIs<DocumentObject>(yamlRoot["components"])
        val jsonComponents = assertIs<DocumentObject>(jsonRoot["components"])
        val yamlSchemas = assertIs<DocumentObject>(yamlComponents["schemas"])
        val jsonSchemas = assertIs<DocumentObject>(jsonComponents["schemas"])
        assertEquals(listOf("UserRef", "Example"), yamlSchemas.members.keys.toList())
        assertEquals(listOf("UserRef", "Example"), jsonSchemas.members.keys.toList())

        val yamlUserRef = assertIs<DocumentObject>(yamlSchemas["UserRef"])
        val jsonUserRef = assertIs<DocumentObject>(jsonSchemas["UserRef"])
        assertEquals("#/components/schemas/User", assertIs<DocumentString>(yamlUserRef["${'$'}ref"]).value)
        assertEquals("#/components/schemas/User", assertIs<DocumentString>(jsonUserRef["${'$'}ref"]).value)

        val yamlExample = assertIs<DocumentObject>(yamlSchemas["Example"])
        val jsonExample = assertIs<DocumentObject>(jsonSchemas["Example"])
        val exampleMembers = listOf("enabled", "quotedEnabled", "count", "quotedCount", "tags")
        assertEquals(exampleMembers, yamlExample.members.keys.toList())
        assertEquals(exampleMembers, jsonExample.members.keys.toList())
        assertEquals(true, assertIs<DocumentBoolean>(yamlExample["enabled"]).value)
        assertEquals(true, assertIs<DocumentBoolean>(jsonExample["enabled"]).value)
        assertEquals("true", assertIs<DocumentString>(yamlExample["quotedEnabled"]).value)
        assertEquals("true", assertIs<DocumentString>(jsonExample["quotedEnabled"]).value)
        assertEquals(12, assertIs<DocumentNumber>(yamlExample["count"]).value)
        assertEquals(12, assertIs<DocumentNumber>(jsonExample["count"]).value)
        assertEquals("12", assertIs<DocumentString>(yamlExample["quotedCount"]).value)
        assertEquals("12", assertIs<DocumentString>(jsonExample["quotedCount"]).value)

        val yamlTags = assertIs<DocumentArray>(yamlExample["tags"])
        val jsonTags = assertIs<DocumentArray>(jsonExample["tags"])
        assertEquals(2, yamlTags.elements.size)
        assertEquals(2, jsonTags.elements.size)
        assertEquals("public", assertIs<DocumentString>(yamlTags[0]).value)
        assertEquals("public", assertIs<DocumentString>(jsonTags[0]).value)
        assertEquals("internal", assertIs<DocumentString>(yamlTags[1]).value)
        assertEquals("internal", assertIs<DocumentString>(jsonTags[1]).value)
    }
}
