package dev.banking.asyncapi.generator.core.context

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SourceTrackingTest {

    private val tracking = SourceTracking()

    @Test
    fun `registerSourceAndGetPathId returns stable id for the same file`() {
        val file = File("/tmp/test.yaml")
        val id1 = tracking.registerSourceAndGetPathId(file, "content1")
        val id2 = tracking.registerSourceAndGetPathId(file, "content2")

        assertEquals(id1, id2)
    }

    @Test
    fun `getCurrentFile returns the last registered file`() {
        val first = File("/tmp/first.yaml")
        val second = File("/tmp/second.yaml")

        tracking.registerSource(first, "a")
        tracking.registerSource(second, "b")

        assertEquals(second.canonicalFile, tracking.getCurrentFile().canonicalFile)
    }

    @Test
    fun `findFileById returns null for unknown id`() {
        assertNull(tracking.findFileById("unknown"))
    }

    @Test
    fun `findFileById returns file after registration`() {
        val file = File("/tmp/test.yaml")
        val id = tracking.registerSourceAndGetPathId(file, "content")

        assertNotNull(tracking.findFileById(id))
    }

    @Test
    fun `registerLine and pathSnippet round-trip`() {
        val file = File("/tmp/test.yaml")
        tracking.registerSource(file, "line1\nline2\nline3\n")
        val id = tracking.registerSourceAndGetPathId(file, "line1\nline2\nline3\n")

        tracking.registerLine(id, 2)

        val snippet = tracking.pathSnippet(id)
        assertNotNull(snippet)
    }
}
