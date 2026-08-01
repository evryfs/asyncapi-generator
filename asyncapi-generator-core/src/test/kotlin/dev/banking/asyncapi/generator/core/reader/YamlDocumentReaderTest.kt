package dev.banking.asyncapi.generator.core.reader

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.messageContains
import assertk.assertions.prop
import assertk.assertions.startsWith
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

class YamlDocumentReaderTest {

    private val reader = YamlDocumentReader()

    @Test
    fun `reads semantic scalar values without yaml style markers`() {
        val file = TestResources.file("reader/yaml/semantic-scalars.yaml")
        val source = DocumentSource(
            id = "semantic-scalars",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )
        val document = reader.read(source)
        val root = assertThat(document.root).isInstanceOf<DocumentObject>()
        val info = root.prop("info") { it["info"] }.isNotNull().isInstanceOf<DocumentObject>()
        val components = root.prop("components") { it["components"] }.isNotNull().isInstanceOf<DocumentObject>()
        val schemas = components.prop("schemas") { it["schemas"] }.isNotNull().isInstanceOf<DocumentObject>()
        val example = schemas.prop("Example") { it["Example"] }.isNotNull().isInstanceOf<DocumentObject>()

        root.prop("asyncapi") { it["asyncapi"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("3.0.0")
        info.prop("title") { it["title"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("Demo API")
        info.prop("summary") { it["summary"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).startsWith("folded text")
        info.prop("description") { it["description"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).startsWith("literal\ntext")
        example.prop("enabled") { it["enabled"] }.isNotNull().isInstanceOf<DocumentBoolean>()
            .prop(DocumentBoolean::value).isEqualTo(true)
        example.prop("quotedEnabled") { it["quotedEnabled"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("true")
        example.prop("count") { it["count"] }.isNotNull().isInstanceOf<DocumentNumber>()
            .prop(DocumentNumber::value).isEqualTo(12)
        example.prop("quotedCount") { it["quotedCount"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("12")
        example.prop("price") { it["price"] }.isNotNull().isInstanceOf<DocumentNumber>()
            .prop(DocumentNumber::value).isEqualTo(12.5)
        example.prop("nullable") { it["nullable"] }.isNotNull().isInstanceOf<DocumentNull>()
    }

    @Test
    fun `fails when yaml is malformed`() {
        val file = TestResources.file("reader/yaml/malformed.yaml")
        val source = DocumentSource(
            id = "malformed",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )

        assertFailure {
            reader.read(source)
        }.isInstanceOf<DocumentReadException.MalformedDocument>()
    }

    @Test
    fun `normalizes invalid yaml characters as malformed input`() {
        val file = TestResources.file("reader/yaml/invalid-root.yaml")
        val source = DocumentSource(
            id = "invalid-root",
            file = file,
            content = "value: \u0001",
            format = DocumentFormat.YAML,
        )

        assertFailure {
            reader.read(source)
        }.isInstanceOf<DocumentReadException.MalformedDocument>()
    }

    @Test
    fun `reads an array root without applying AsyncAPI rules`() {
        val file = TestResources.file("reader/yaml/invalid-root.yaml")
        val source = DocumentSource(
            id = "invalid-root",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )
        val document = reader.read(source)

        val root = assertThat(document.root).isInstanceOf<DocumentArray>()
        root.prop("first element") { it[0] }.isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("asyncapi")
        root.prop("second element") { it[1] }.isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("info")
        root.prop("location path") { it.location.path }.isEqualTo("root")
        root.prop("location line") { it.location.line }.isEqualTo(1)
    }

    @Test
    fun `fails when mapping key is not scalar`() {
        val file = TestResources.file("reader/yaml/non-scalar-key.yaml")
        val source = DocumentSource(
            id = "non-scalar-key",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )

        assertFailure {
            reader.read(source)
        }.isInstanceOf<DocumentReadException.InvalidMappingKey>()
            .messageContains("expected string key")
    }

    @Test
    fun `fails when mapping key is a non-string scalar`() {
        val file = TestResources.file("reader/yaml/invalid-root.yaml")
        listOf("true", "42", "null").forEach { key ->
            val source = DocumentSource(
                id = "invalid-root",
                file = file,
                content = "  $key: value",
                format = DocumentFormat.YAML,
            )

            val failure = assertFailure {
                reader.read(source)
            }.isInstanceOf<DocumentReadException.InvalidMappingKey>()

            failure.messageContains(source.file.absolutePath)
            failure.messageContains("line 1, column 3")
            failure.messageContains("expected string key")
        }
    }

    @Test
    fun `accepts quoted keys that resemble non-string scalars`() {
        val file = TestResources.file("reader/yaml/invalid-root.yaml")
        val source = DocumentSource(
            id = "invalid-root",
            file = file,
            content =
                """
                "true": boolean-shaped
                "42": number-shaped
                "null": null-shaped
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )

        val root = assertThat(reader.read(source).root).isInstanceOf<DocumentObject>()
        root.prop("true") { it["true"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("boolean-shaped")
        root.prop("42") { it["42"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("number-shaped")
        root.prop("null") { it["null"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("null-shaped")
    }

    @Test
    fun `fails instead of exposing a yaml merge key as a literal member`() {
        val file = TestResources.file("reader/yaml/invalid-root.yaml")
        val source = DocumentSource(
            id = "invalid-root",
            file = file,
            content =
                """
                base: &base
                  enabled: true
                merged:
                  <<: *base
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )

        val failure = assertFailure {
            reader.read(source)
        }.isInstanceOf<DocumentReadException.InvalidMappingKey>()

        failure.messageContains("line 4, column 3")
        failure.messageContains("expected string key")
    }

    @Test
    fun `normalizes nesting limit failures`() {
        val constrainedReader =
            YamlDocumentReader(
                DocumentReaderLimits.DEFAULT.copy(maxNestingDepth = 2),
            )
        val file = TestResources.file("reader/yaml/invalid-root.yaml")
        val source = DocumentSource(
            id = "invalid-root",
            file = file,
            content =
                """
                root:
                  child:
                    leaf: true
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )

        assertFailure {
            constrainedReader.read(source)
        }.isInstanceOf<DocumentReadException.ResourceLimitExceeded>()
            .messageContains(source.file.absolutePath)
    }

    @Test
    fun `limits expanded collection aliases`() {
        val constrainedReader =
            YamlDocumentReader(
                DocumentReaderLimits.DEFAULT.copy(maxAliasesForCollections = 1),
            )
        val file = TestResources.file("reader/yaml/invalid-root.yaml")
        val source = DocumentSource(
            id = "invalid-root",
            file = file,
            content =
                """
                base: &base
                  value: true
                first: *base
                second: *base
                """.trimIndent(),
            format = DocumentFormat.YAML,
        )

        assertFailure {
            constrainedReader.read(source)
        }.isInstanceOf<DocumentReadException.ResourceLimitExceeded>()
    }

    @Test
    fun `returns linked map preserving input order`() {
        val file = TestResources.file("reader/yaml/order-preservation.yaml")
        val source = DocumentSource(
            id = "order-preservation",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )
        val root = assertThat(reader.read(source).root).isInstanceOf<DocumentObject>()

        root.prop("member names") { it.members.keys.toList() }
            .isEqualTo(listOf("asyncapi", "info", "channels"))
    }
}
