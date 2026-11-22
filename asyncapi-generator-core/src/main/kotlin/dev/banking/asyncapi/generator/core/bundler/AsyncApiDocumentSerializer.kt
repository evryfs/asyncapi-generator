package dev.banking.asyncapi.generator.core.bundler

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.util.StringQuotingChecker
import org.yaml.snakeyaml.nodes.NodeId
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.resolver.Resolver

/**
 * Writes an AsyncAPI document, or any part of its model, as YAML or JSON.
 *
 * Absent values are left out. In YAML, a string that would read back as another type, such as `"true"`,
 * `"123"`, or `"on"`, is quoted, so reading the written document gives back the same values.
 */
internal object AsyncApiDocumentSerializer {
    /** Writes [value] as YAML. */
    fun toYaml(value: Any): String = yamlMapper.writeValueAsString(value)

    /** Writes [value] as indented JSON. */
    fun toJson(value: Any): String =
        jsonMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(value)

    private val yamlMapper: ObjectMapper =
        ObjectMapper(
            YAMLFactory
                .builder()
                .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
                .configure(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR, true)
                .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
                .configure(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE, true)
                .stringQuotingChecker(ValuePreservingStringQuotingChecker)
                .build(),
        ).apply {
            setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
        }

    private val jsonMapper: ObjectMapper =
        ObjectMapper()
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)

    /** Quotes every string value that a YAML reader would otherwise resolve to a non-string type. */
    private object ValuePreservingStringQuotingChecker : StringQuotingChecker() {
        private val default = Default.instance()
        private val resolver = Resolver()

        override fun needToQuoteName(name: String): Boolean = default.needToQuoteName(name)

        override fun needToQuoteValue(value: String): Boolean =
            default.needToQuoteValue(value) || resolver.resolve(NodeId.scalar, value, true) != Tag.STR
    }
}
