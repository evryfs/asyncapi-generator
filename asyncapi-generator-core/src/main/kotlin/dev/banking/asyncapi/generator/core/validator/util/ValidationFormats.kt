package dev.banking.asyncapi.generator.core.validator.util

import com.fasterxml.jackson.core.JsonPointer
import jakarta.activation.MimeType
import jakarta.mail.internet.InternetAddress
import java.net.URI

/** Exact-value format checks shared by semantic validators. */
internal object ValidationFormats {

    fun absoluteUri(value: String): URI? =
        runCatching { URI(value) }
            .getOrNull()
            ?.takeIf(URI::isAbsolute)

    fun isSpecificMediaType(value: String): Boolean =
        runCatching { MimeType(value) }
            .getOrNull()
            ?.let { it.primaryType != "*" && it.subType != "*" }
            ?: false

    fun isEmailAddress(value: String): Boolean =
        runCatching {
            InternetAddress(value, true).also { address ->
                address.validate()
                require(address.personal == null && address.address == value)
            }
        }.isSuccess

    fun isReusableObjectName(value: String): Boolean = REUSABLE_OBJECT_NAME.matches(value)

    fun isRuntimeExpression(value: String): Boolean {
        val prefix = RUNTIME_EXPRESSION_PREFIXES.firstOrNull(value::startsWith) ?: return false
        if (value == prefix) return true
        if (!value.startsWith("$prefix#")) return false

        val fragment = runCatching { URI(value.substring(prefix.length)).fragment }.getOrNull() ?: return false
        if (INVALID_JSON_POINTER_ESCAPE.containsMatchIn(fragment)) return false
        return runCatching { JsonPointer.compile(fragment) }.isSuccess
    }

    private val RUNTIME_EXPRESSION_PREFIXES = listOf(
        "\$message.header",
        "\$message.payload",
    )
    private val INVALID_JSON_POINTER_ESCAPE = Regex("~(?![01])")
    private val REUSABLE_OBJECT_NAME = Regex("^[A-Za-z0-9_-]+$")
}
