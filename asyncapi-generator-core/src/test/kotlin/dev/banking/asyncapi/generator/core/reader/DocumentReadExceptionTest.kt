package dev.banking.asyncapi.generator.core.reader

import dev.banking.asyncapi.generator.core.document.DocumentFormat
import dev.banking.asyncapi.generator.core.document.DocumentSource
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DocumentReadExceptionTest {

    private val reader = YamlDocumentReader()

    @Test
    fun `empty document error includes the source file`() {
        val file = TestResources.file("reader/yaml/empty.yaml")
        val source = DocumentSource(
            id = "empty",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )

        val failure = assertFailsWith<DocumentReadException.EmptyDocument> {
            reader.read(source)
        }
        assertEquals(source.file, failure.file)
        assertEquals(null, failure.location)
        assertTrue(failure.message.orEmpty().contains(source.file.absolutePath))
    }

    @Test
    fun `duplicate key error includes the key and source file`() {
        val file = TestResources.file("reader/yaml/duplicate-key.yaml")
        val source = DocumentSource(
            id = "duplicate-key",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )

        val failure = assertFailsWith<DocumentReadException.DuplicateKey> {
            reader.read(source)
        }

        assertEquals(source.file, failure.file)
        assertEquals("title", failure.memberName)
        val location = assertNotNull(failure.location)
        assertEquals(source.id, location.sourceId)
        assertEquals(source.file, location.file)
        assertEquals("root.info.title", location.path)
        assertEquals(3, location.line)
        assertEquals(3, location.column)
        assertTrue(failure.message.orEmpty().contains("title"))
        assertTrue(failure.message.orEmpty().contains(source.file.absolutePath))
    }

    @Test
    fun `malformed document exposes source location and original cause`() {
        val file = TestResources.file("reader/yaml/malformed.yaml")
        val source = DocumentSource(
            id = "malformed",
            file = file,
            content = file.readText(),
            format = DocumentFormat.YAML,
        )

        val failure = assertFailsWith<DocumentReadException.MalformedDocument> {
            reader.read(source)
        }

        assertEquals(source.file, failure.file)
        val location = assertNotNull(failure.location)
        assertEquals(source.id, location.sourceId)
        assertTrue(location.line > 0)
        assertTrue(location.column > 0)
        assertNotNull(failure.cause)
    }

    @Test
    fun `resource limit failure exposes the concrete limit and maximum`() {
        val file = TestResources.file("reader/yaml/invalid-root.yaml")
        val source = DocumentSource(
            id = "invalid-root",
            file = file,
            content = "12345",
            format = DocumentFormat.YAML,
        )
        val constrainedReader = YamlDocumentReader(
            DocumentReaderLimits.DEFAULT.copy(maxNumberCharacters = 4),
        )

        val failure = assertFailsWith<DocumentReadException.ResourceLimitExceeded> {
            constrainedReader.read(source)
        }

        assertEquals(source.file, failure.file)
        assertEquals(DocumentResourceLimit.NUMBER_CHARACTERS, failure.limit)
        assertEquals(4L, failure.maximum)
        assertEquals(1, assertNotNull(failure.location).line)
        assertIs<IllegalArgumentException>(failure.cause)
    }
}
