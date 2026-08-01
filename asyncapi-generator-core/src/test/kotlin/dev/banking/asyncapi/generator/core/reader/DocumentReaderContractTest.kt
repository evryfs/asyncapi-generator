package dev.banking.asyncapi.generator.core.reader

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.prop
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
        val root = assertThat(document.root).isInstanceOf<DocumentObject>()
        val components = root.prop("components") { it["components"] }.isNotNull().isInstanceOf<DocumentObject>()
        val schemas = components.prop("schemas") { it["schemas"] }.isNotNull().isInstanceOf<DocumentObject>()
        val userRef = schemas.prop("UserRef") { it["UserRef"] }.isNotNull().isInstanceOf<DocumentObject>()

        userRef.prop("${'$'}ref") { it["${'$'}ref"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value)
            .isEqualTo("#/components/schemas/User")
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
        val root = assertThat(reader.read(source).root).isInstanceOf<DocumentObject>()

        val memberNames = root.prop("member names") { it.members.keys }
        memberNames.contains("notAsyncApi")
        memberNames.contains("stillReaderInput")
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

        assertThat(reader.read(yamlSource).root).isInstanceOf<DocumentBoolean>()
            .prop(DocumentBoolean::value).isTrue()
        assertThat(jsonReader.read(jsonSource).root).isInstanceOf<DocumentBoolean>()
            .prop(DocumentBoolean::value).isTrue()
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

        assertThat(reader.read(yamlSource).root).isInstanceOf<DocumentNull>()
        assertThat(jsonReader.read(jsonSource).root).isInstanceOf<DocumentNull>()
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

        assertFailure {
            YamlDocumentReader(limits).read(yamlSource)
        }.isInstanceOf<DocumentReadException.ResourceLimitExceeded>()
        assertFailure {
            JsonDocumentReader(limits).read(jsonSource)
        }.isInstanceOf<DocumentReadException.ResourceLimitExceeded>()
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

        val yamlRoot = assertThat(reader.read(yamlSource).root).isInstanceOf<DocumentObject>()
        val jsonRoot = assertThat(jsonReader.read(jsonSource).root).isInstanceOf<DocumentObject>()

        yamlRoot.prop("smallInteger") { it["smallInteger"] }.isNotNull().isInstanceOf<DocumentNumber>()
            .prop(DocumentNumber::value).isInstanceOf<Int>().isEqualTo(42)
        jsonRoot.prop("smallInteger") { it["smallInteger"] }.isNotNull().isInstanceOf<DocumentNumber>()
            .prop(DocumentNumber::value).isInstanceOf<Int>().isEqualTo(42)

        yamlRoot.prop("longInteger") { it["longInteger"] }.isNotNull().isInstanceOf<DocumentNumber>()
            .prop(DocumentNumber::value).isInstanceOf<Long>().isEqualTo(2147483648L)
        jsonRoot.prop("longInteger") { it["longInteger"] }.isNotNull().isInstanceOf<DocumentNumber>()
            .prop(DocumentNumber::value).isInstanceOf<Long>().isEqualTo(2147483648L)

        val largeInteger = BigInteger("9223372036854775808")
        yamlRoot.prop("largeInteger") { it["largeInteger"] }.isNotNull().isInstanceOf<DocumentNumber>()
            .prop(DocumentNumber::value).isInstanceOf<BigInteger>().isEqualTo(largeInteger)
        jsonRoot.prop("largeInteger") { it["largeInteger"] }.isNotNull().isInstanceOf<DocumentNumber>()
            .prop(DocumentNumber::value).isInstanceOf<BigInteger>().isEqualTo(largeInteger)

        yamlRoot.prop("ordinaryDecimal") { it["ordinaryDecimal"] }.isNotNull().isInstanceOf<DocumentNumber>()
            .prop(DocumentNumber::value).isInstanceOf<Double>().isEqualTo(12.5)
        jsonRoot.prop("ordinaryDecimal") { it["ordinaryDecimal"] }.isNotNull().isInstanceOf<DocumentNumber>()
            .prop(DocumentNumber::value).isInstanceOf<Double>().isEqualTo(12.5)

        val preciseDecimal = BigDecimal("0.12345678901234567890123456789")
        yamlRoot.prop("preciseDecimal") { it["preciseDecimal"] }.isNotNull().isInstanceOf<DocumentNumber>()
            .prop(DocumentNumber::value).isInstanceOf<BigDecimal>().isEqualTo(preciseDecimal)
        jsonRoot.prop("preciseDecimal") { it["preciseDecimal"] }.isNotNull().isInstanceOf<DocumentNumber>()
            .prop(DocumentNumber::value).isInstanceOf<BigDecimal>().isEqualTo(preciseDecimal)

        val hugeDecimal = BigDecimal("1e400")
        yamlRoot.prop("hugeDecimal") { it["hugeDecimal"] }.isNotNull().isInstanceOf<DocumentNumber>()
            .prop(DocumentNumber::value).isInstanceOf<BigDecimal>().isEqualTo(hugeDecimal)
        jsonRoot.prop("hugeDecimal") { it["hugeDecimal"] }.isNotNull().isInstanceOf<DocumentNumber>()
            .prop(DocumentNumber::value).isInstanceOf<BigDecimal>().isEqualTo(hugeDecimal)
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

        assertFailure {
            YamlDocumentReader(limits).read(yamlSource)
        }.isInstanceOf<DocumentReadException.ResourceLimitExceeded>()
        assertFailure {
            JsonDocumentReader(limits).read(jsonSource)
        }.isInstanceOf<DocumentReadException.ResourceLimitExceeded>()
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

        val yamlRoot = assertThat(reader.read(yamlSource).root).isInstanceOf<DocumentObject>()
        val jsonRoot = assertThat(jsonReader.read(jsonSource).root).isInstanceOf<DocumentObject>()
        yamlRoot.prop("member names") { it.members.keys.toList() }
            .isEqualTo(listOf("asyncapi", "info", "components"))
        jsonRoot.prop("member names") { it.members.keys.toList() }
            .isEqualTo(listOf("asyncapi", "info", "components"))
        yamlRoot.prop("asyncapi") { it["asyncapi"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("3.0.0")
        jsonRoot.prop("asyncapi") { it["asyncapi"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("3.0.0")

        val yamlInfo = yamlRoot.prop("info") { it["info"] }.isNotNull().isInstanceOf<DocumentObject>()
        val jsonInfo = jsonRoot.prop("info") { it["info"] }.isNotNull().isInstanceOf<DocumentObject>()
        yamlInfo.prop("member names") { it.members.keys.toList() }.isEqualTo(listOf("title"))
        jsonInfo.prop("member names") { it.members.keys.toList() }.isEqualTo(listOf("title"))
        yamlInfo.prop("title") { it["title"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("Demo API")
        jsonInfo.prop("title") { it["title"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("Demo API")

        val yamlComponents = yamlRoot.prop("components") { it["components"] }
            .isNotNull().isInstanceOf<DocumentObject>()
        val jsonComponents = jsonRoot.prop("components") { it["components"] }
            .isNotNull().isInstanceOf<DocumentObject>()
        val yamlSchemas = yamlComponents.prop("schemas") { it["schemas"] }
            .isNotNull().isInstanceOf<DocumentObject>()
        val jsonSchemas = jsonComponents.prop("schemas") { it["schemas"] }
            .isNotNull().isInstanceOf<DocumentObject>()
        yamlSchemas.prop("member names") { it.members.keys.toList() }
            .isEqualTo(listOf("UserRef", "Example"))
        jsonSchemas.prop("member names") { it.members.keys.toList() }
            .isEqualTo(listOf("UserRef", "Example"))

        val yamlUserRef = yamlSchemas.prop("UserRef") { it["UserRef"] }
            .isNotNull().isInstanceOf<DocumentObject>()
        val jsonUserRef = jsonSchemas.prop("UserRef") { it["UserRef"] }
            .isNotNull().isInstanceOf<DocumentObject>()
        yamlUserRef.prop("${'$'}ref") { it["${'$'}ref"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("#/components/schemas/User")
        jsonUserRef.prop("${'$'}ref") { it["${'$'}ref"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("#/components/schemas/User")

        val yamlExample = yamlSchemas.prop("Example") { it["Example"] }
            .isNotNull().isInstanceOf<DocumentObject>()
        val jsonExample = jsonSchemas.prop("Example") { it["Example"] }
            .isNotNull().isInstanceOf<DocumentObject>()
        val exampleMembers = listOf("enabled", "quotedEnabled", "count", "quotedCount", "tags")
        yamlExample.prop("member names") { it.members.keys.toList() }.isEqualTo(exampleMembers)
        jsonExample.prop("member names") { it.members.keys.toList() }.isEqualTo(exampleMembers)
        yamlExample.prop("enabled") { it["enabled"] }.isNotNull().isInstanceOf<DocumentBoolean>()
            .prop(DocumentBoolean::value).isTrue()
        jsonExample.prop("enabled") { it["enabled"] }.isNotNull().isInstanceOf<DocumentBoolean>()
            .prop(DocumentBoolean::value).isTrue()
        yamlExample.prop("quotedEnabled") { it["quotedEnabled"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("true")
        jsonExample.prop("quotedEnabled") { it["quotedEnabled"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("true")
        yamlExample.prop("count") { it["count"] }.isNotNull().isInstanceOf<DocumentNumber>()
            .prop(DocumentNumber::value).isEqualTo(12)
        jsonExample.prop("count") { it["count"] }.isNotNull().isInstanceOf<DocumentNumber>()
            .prop(DocumentNumber::value).isEqualTo(12)
        yamlExample.prop("quotedCount") { it["quotedCount"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("12")
        jsonExample.prop("quotedCount") { it["quotedCount"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("12")

        val yamlTags = yamlExample.prop("tags") { it["tags"] }.isNotNull().isInstanceOf<DocumentArray>()
        val jsonTags = jsonExample.prop("tags") { it["tags"] }.isNotNull().isInstanceOf<DocumentArray>()
        yamlTags.prop("size") { it.elements.size }.isEqualTo(2)
        jsonTags.prop("size") { it.elements.size }.isEqualTo(2)
        yamlTags.prop("first element") { it[0] }.isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("public")
        jsonTags.prop("first element") { it[0] }.isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("public")
        yamlTags.prop("second element") { it[1] }.isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("internal")
        jsonTags.prop("second element") { it[1] }.isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("internal")
    }
}
