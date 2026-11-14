package dev.banking.asyncapi.generator.core.generator.util

import com.github.mustachejava.DefaultMustacheFactory
import java.io.Writer

/**
 * Mustache templates for generated Kotlin and Java source.
 *
 * Mustache HTML-escapes `{{value}}` by default, which turns `Map<String, Int>` into
 * `Map&lt;String, Int&gt;`. Source code is not HTML, so values are written exactly as given;
 * callers escape them for the target language where needed.
 */
internal class SourceMustacheFactory(resourceRoot: String) : DefaultMustacheFactory(resourceRoot) {
    override fun encode(value: String, writer: Writer) {
        writer.write(value)
    }
}
