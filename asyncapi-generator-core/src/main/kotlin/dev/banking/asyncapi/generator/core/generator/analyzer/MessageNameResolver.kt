package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.model.messages.Message

/**
 * Resolves the source-code name used to identify an AsyncAPI message.
 *
 * The Message Object `name` takes precedence when present. The message map key
 * is the stable fallback. Human-readable titles do not affect generated names.
 */
object MessageNameResolver {
    fun resolve(
        message: Message,
        messageId: String,
    ): String =
        MapperUtil.toPascalCase(
            message.name
                ?.takeIf(String::isNotBlank)
                ?: messageId,
        )
}
