package dev.banking.asyncapi.generator.core.reader

import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.messageContains
import assertk.assertions.prop
import dev.banking.asyncapi.generator.core.document.DocumentArray
import dev.banking.asyncapi.generator.core.document.DocumentBoolean
import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentMember
import dev.banking.asyncapi.generator.core.document.DocumentNull
import dev.banking.asyncapi.generator.core.document.DocumentNumber
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.document.DocumentSource
import dev.banking.asyncapi.generator.core.document.DocumentString
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import org.junit.jupiter.api.Test

class JsonDocumentReaderTest {

    private val reader = JsonDocumentReader()

    @Test
    fun `reads semantic scalar values`() {
        val file = TestResources.file("reader/json/semantic-scalars.json")
        val source = DocumentSource(
            id = "semantic-scalars",
            file = file,
            content = file.readText(),
            format = DocumentFormat.JSON,
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
            .prop(DocumentString::value).isEqualTo("folded text")
        info.prop("description") { it["description"] }.isNotNull().isInstanceOf<DocumentString>()
            .prop(DocumentString::value).isEqualTo("literal\ntext")
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
    fun `fails when json is malformed`() {
        val file = TestResources.file("reader/json/malformed.json")
        val source = DocumentSource(
            id = "malformed",
            file = file,
            content = file.readText(),
            format = DocumentFormat.JSON,
        )

        assertFailure {
            reader.read(source)
        }.isInstanceOf<DocumentReadException.MalformedDocument>()
    }

    @Test
    fun `reads an array root without applying AsyncAPI rules`() {
        val file = TestResources.file("reader/json/invalid-root.json")
        val source = DocumentSource(
            id = "invalid-root",
            file = file,
            content = file.readText(),
            format = DocumentFormat.JSON,
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
    fun `fails when document is empty`() {
        val file = TestResources.file("reader/json/empty.json")
        val source = DocumentSource(
            id = "empty",
            file = file,
            content = file.readText(),
            format = DocumentFormat.JSON,
        )

        assertFailure {
            reader.read(source)
        }.isInstanceOf<DocumentReadException.EmptyDocument>()
    }

    @Test
    fun `fails when object contains duplicate keys`() {
        val file = TestResources.file("reader/json/duplicate-key.json")
        val source = DocumentSource(
            id = "duplicate-key",
            file = file,
            content = file.readText(),
            format = DocumentFormat.JSON,
        )
        val failure = assertFailure {
            reader.read(source)
        }.isInstanceOf<DocumentReadException.DuplicateKey>()

        failure.messageContains("title")
        failure.messageContains(source.file.absolutePath)
    }

    @Test
    fun `normalizes nesting limit failures`() {
        val constrainedReader =
            JsonDocumentReader(
                DocumentReaderLimits.DEFAULT.copy(maxNestingDepth = 2),
            )
        val file = TestResources.file("reader/json/invalid-root.json")
        val source = DocumentSource(
            id = "invalid-root",
            file = file,
            content = """{"root":{"child":{"leaf":true}}}""",
            format = DocumentFormat.JSON,
        )

        assertFailure {
            constrainedReader.read(source)
        }.isInstanceOf<DocumentReadException.ResourceLimitExceeded>()
            .messageContains(source.file.absolutePath)
    }

    @Test
    fun `records source locations for root object fields and array items`() {
        val file = TestResources.file("reader/json/source-map.json")
        val source = DocumentSource(
            id = "source-map",
            file = file,
            content = file.readText(),
            format = DocumentFormat.JSON,
        )
        val document = reader.read(source)
        val root = assertThat(document.root).isInstanceOf<DocumentObject>()
        val info = root.prop("info") { it["info"] }.isNotNull().isInstanceOf<DocumentObject>()
        val tags = info.prop("tags") { it["tags"] }.isNotNull().isInstanceOf<DocumentArray>()

        root.prop(DocumentObject::location).all {
            prop("sourceId") { it.sourceId }.isEqualTo(source.id)
            prop("file") { it.file }.isEqualTo(source.file)
            prop("path") { it.path }.isEqualTo("root")
            prop("line") { it.line }.isEqualTo(1)
            prop("column") { it.column }.isGreaterThanOrEqualTo(1)
        }
        root.prop("asyncapi member") { it.member("asyncapi") }
            .isNotNull()
            .prop(DocumentMember::keyLocation)
            .all {
                prop("sourceId") { it.sourceId }.isEqualTo(source.id)
                prop("file") { it.file }.isEqualTo(source.file)
                prop("path") { it.path }.isEqualTo("root.asyncapi")
                prop("line") { it.line }.isEqualTo(2)
                prop("column") { it.column }.isGreaterThanOrEqualTo(1)
            }
        root.prop("info member") { it.member("info") }
            .isNotNull()
            .prop(DocumentMember::keyLocation)
            .all {
                prop("sourceId") { it.sourceId }.isEqualTo(source.id)
                prop("file") { it.file }.isEqualTo(source.file)
                prop("path") { it.path }.isEqualTo("root.info")
                prop("line") { it.line }.isEqualTo(3)
                prop("column") { it.column }.isGreaterThanOrEqualTo(1)
            }
        info.prop("title member") { it.member("title") }
            .isNotNull()
            .prop(DocumentMember::keyLocation)
            .all {
                prop("sourceId") { it.sourceId }.isEqualTo(source.id)
                prop("file") { it.file }.isEqualTo(source.file)
                prop("path") { it.path }.isEqualTo("root.info.title")
                prop("line") { it.line }.isEqualTo(4)
                prop("column") { it.column }.isGreaterThanOrEqualTo(1)
            }
        info.prop("tags member") { it.member("tags") }
            .isNotNull()
            .prop(DocumentMember::keyLocation)
            .all {
                prop("sourceId") { it.sourceId }.isEqualTo(source.id)
                prop("file") { it.file }.isEqualTo(source.file)
                prop("path") { it.path }.isEqualTo("root.info.tags")
                prop("line") { it.line }.isEqualTo(5)
                prop("column") { it.column }.isGreaterThanOrEqualTo(1)
            }
        tags.prop("first element") { it[0] }
            .prop("location") { it.location }
            .all {
                prop("sourceId") { it.sourceId }.isEqualTo(source.id)
                prop("file") { it.file }.isEqualTo(source.file)
                prop("path") { it.path }.isEqualTo("root.info.tags[0]")
                prop("line") { it.line }.isEqualTo(6)
                prop("column") { it.column }.isGreaterThanOrEqualTo(1)
            }
    }
}
