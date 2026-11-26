package com.example.projectresources

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Generated sources and schemas share one directory without changing which of the project's own files are resources. */
class ProjectResourcesTest {
    @Test
    fun `the project's own resources are packaged, whatever their file names`() {
        assertNotNull(javaClass.getResource("/templates/Example.kt"))
    }

    @Test
    fun `generated schemas are packaged and generated sources are not`() {
        assertNotNull(javaClass.getResource("/com/example/projectresources/avro/Note.avsc"))
        assertNull(javaClass.getResource("/com/example/projectresources/kotlin/Note.kt"))
    }
}
