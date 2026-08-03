package dev.banking.asyncapi.generator.core.parser.version

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AsyncApiSpecificationVersionTest {

    @Test
    fun `parses a valid version into its specification line`() {
        assertEquals(
            AsyncApiSpecificationVersion(
                major = 3,
                minor = 0,
            ),
            AsyncApiSpecificationVersion.parse("3.0.7-rc1"),
        )
    }

    @Test
    fun `rejects incomplete decorated whitespace padded and overflowing versions`() {
        listOf(
            "3",
            "3.0",
            "3.0.0+build",
            " 3.0.0",
            "3.0.0 ",
            "999999999999999999999.0.0",
        ).forEach { value ->
            assertNull(AsyncApiSpecificationVersion.parse(value), value)
        }
    }
}
