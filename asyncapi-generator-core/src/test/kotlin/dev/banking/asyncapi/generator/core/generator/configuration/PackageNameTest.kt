package dev.banking.asyncapi.generator.core.generator.configuration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PackageNameTest {
    @Test
    fun `fromConfigurationValue creates package name`() {
        val packageName =
            PackageName.fromConfigurationValue(
                value = "com.example.account.model",
                path = "modelPackage",
            )

        assertEquals("com.example.account.model", packageName.value)
    }

    @Test
    fun `fromConfigurationValue rejects empty package name`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PackageName.fromConfigurationValue(
                    value = " ",
                    path = "modelPackage",
                )
            }

        assertEquals("modelPackage cannot be empty", exception.message)
    }

    @Test
    fun `fromConfigurationValue rejects malformed package name`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PackageName.fromConfigurationValue(
                    value = "com.example.account-model",
                    path = "modelPackage",
                )
            }

        assertEquals(
            "modelPackage must be a dot-separated package name, for example com.example.model",
            exception.message,
        )
    }
}
