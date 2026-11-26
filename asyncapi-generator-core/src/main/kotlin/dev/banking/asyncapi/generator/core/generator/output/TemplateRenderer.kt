package dev.banking.asyncapi.generator.core.generator.output

import com.github.mustachejava.DefaultMustacheFactory
import java.io.StringWriter
import java.io.Writer

/**
 * Renders the Mustache templates of one resource directory into generated artifacts of one kind.
 *
 * Mustache HTML-escapes `{{value}}` by default, which turns `Map<String, Int>` into
 * `Map&lt;String, Int&gt;`. Generated files are not HTML, so values are written exactly as given;
 * callers escape them for the target language where needed.
 *
 * @param resourceRoot the classpath directory of the templates
 * @param kind the kind of the rendered artifacts
 */
internal class TemplateRenderer(
    resourceRoot: String,
    private val kind: GeneratedArtifactKind,
) {
    private val mustacheFactory =
        object : DefaultMustacheFactory(resourceRoot) {
            override fun encode(
                value: String,
                writer: Writer,
            ) {
                writer.write(value)
            }
        }

    /** The artifact `<namespace path>/<fileName>` rendered from [template] with [data]. */
    fun render(
        namespace: String,
        fileName: String,
        template: String,
        data: Any,
    ): GeneratedArtifact {
        val writer = StringWriter()
        mustacheFactory.compile(template).execute(writer, data).flush()
        return GeneratedArtifact(
            relativePath = GeneratedArtifactPaths.fromNamespace(namespace, fileName),
            content = writer.toString(),
            kind = kind,
        )
    }
}
