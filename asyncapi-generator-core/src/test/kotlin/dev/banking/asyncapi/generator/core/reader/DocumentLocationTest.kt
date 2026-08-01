package dev.banking.asyncapi.generator.core.reader

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.prop
import dev.banking.asyncapi.generator.core.document.DocumentArray
import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentMember
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.document.DocumentSource
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import org.junit.jupiter.api.Test

class DocumentLocationTest {

    private val reader = YamlDocumentReader()

    @Test
    fun `records source locations for root object fields and array items`() {
        val file = TestResources.file("reader/yaml/source-map.yaml")
        val source = DocumentSource(
            id = "source-map",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )
        val document = reader.read(source)
        val root = assertThat(document.root).isInstanceOf<DocumentObject>()
        val info = root.prop("info") { it["info"] }
            .isNotNull()
            .isInstanceOf<DocumentObject>()
        val tags = info.prop("tags") { it["tags"] }
            .isNotNull()
            .isInstanceOf<DocumentArray>()

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
                prop("line") { it.line }.isEqualTo(1)
                prop("column") { it.column }.isGreaterThanOrEqualTo(1)
            }
        root.prop("info member") { it.member("info") }
            .isNotNull()
            .prop(DocumentMember::keyLocation)
            .all {
                prop("sourceId") { it.sourceId }.isEqualTo(source.id)
                prop("file") { it.file }.isEqualTo(source.file)
                prop("path") { it.path }.isEqualTo("root.info")
                prop("line") { it.line }.isEqualTo(2)
                prop("column") { it.column }.isGreaterThanOrEqualTo(1)
            }
        info.prop("title member") { it.member("title") }
            .isNotNull()
            .prop(DocumentMember::keyLocation)
            .all {
                prop("sourceId") { it.sourceId }.isEqualTo(source.id)
                prop("file") { it.file }.isEqualTo(source.file)
                prop("path") { it.path }.isEqualTo("root.info.title")
                prop("line") { it.line }.isEqualTo(3)
                prop("column") { it.column }.isGreaterThanOrEqualTo(1)
            }
        info.prop("tags member") { it.member("tags") }
            .isNotNull()
            .prop(DocumentMember::keyLocation)
            .all {
                prop("sourceId") { it.sourceId }.isEqualTo(source.id)
                prop("file") { it.file }.isEqualTo(source.file)
                prop("path") { it.path }.isEqualTo("root.info.tags")
                prop("line") { it.line }.isEqualTo(4)
                prop("column") { it.column }.isGreaterThanOrEqualTo(1)
            }
        tags.prop("first element") { it[0] }
            .prop("location") { it.location }
            .all {
                prop("sourceId") { it.sourceId }.isEqualTo(source.id)
                prop("file") { it.file }.isEqualTo(source.file)
                prop("path") { it.path }.isEqualTo("root.info.tags[0]")
                prop("line") { it.line }.isEqualTo(5)
                prop("column") { it.column }.isGreaterThanOrEqualTo(1)
            }
    }
}
