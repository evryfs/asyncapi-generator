package dev.banking.asyncapi.generator.core.validator.util

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationFormatsTest {

    @Test
    fun `email validation accepts exact addr specs without normalizing invalid values`() {
        assertTrue(ValidationFormats.isEmailAddress("first.last+events@example.com"))
        assertTrue(ValidationFormats.isEmailAddress("service@localhost"))

        assertFalse(ValidationFormats.isEmailAddress("Support <support@example.com>"))
        assertFalse(ValidationFormats.isEmailAddress(" support@example.com "))
        assertFalse(ValidationFormats.isEmailAddress("first..last@example.com"))
        assertFalse(ValidationFormats.isEmailAddress("missing-at.example.com"))
    }
}
