package dev.banking.asyncapi.generator.core.source

import org.yaml.snakeyaml.DumperOptions.ScalarStyle.PLAIN
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.Tag

/**
 * Converts YAML scalars into document values.
 *
 * Numbers must use JSON number syntax, so a document means the same in YAML and JSON. A plain scalar that
 * YAML 1.1 would read as an octal number is rejected instead of silently becoming a string.
 *
 * @param limits limits for the length of numeric tokens
 */
internal class YamlScalarReader(
    private val limits: SourceLimits,
) {
    fun read(
        node: ScalarNode,
        location: SourceLocation,
    ): DocumentNode =
        when (node.tag) {
            Tag.NULL -> DocumentNull(location)
            Tag.BOOL -> parseBoolean(node.value, location)
            Tag.INT -> parseNumber(node.value, location, DocumentNumberParser::parseInteger)
            Tag.FLOAT -> parseNumber(node.value, location, DocumentNumberParser::parseDecimal)
            Tag.STR -> parseString(node, location)
            Tag.TIMESTAMP -> DocumentString(node.value, location)
            else -> throw DocumentReadException.MalformedDocument.at(
                location = location,
                reason = "Unsupported YAML scalar tag '${node.tag.value}'",
            )
        }

    private fun parseString(
        node: ScalarNode,
        location: SourceLocation,
    ): DocumentNode {
        if (node.scalarStyle == PLAIN && YAML_ONLY_PLAIN_NUMBER.matches(node.value)) {
            throw DocumentReadException.MalformedDocument.at(
                location = location,
                reason = "YAML numeric value '${node.value}' is not a JSON-compatible number",
            )
        }
        return DocumentString(node.value, location)
    }

    private fun parseBoolean(
        value: String,
        location: SourceLocation,
    ): DocumentNode =
        when (value.lowercase()) {
            "true" -> DocumentBoolean(true, location)
            "false" -> DocumentBoolean(false, location)
            else -> DocumentString(value, location)
        }

    private fun parseNumber(
        value: String,
        location: SourceLocation,
        parse: (String) -> Number?,
    ): DocumentNode {
        limits.requireNumberLength(value, location)
        if (!JSON_NUMBER.matches(value)) {
            throw DocumentReadException.MalformedDocument.at(
                location = location,
                reason = "YAML numeric value '$value' is not a JSON-compatible number",
            )
        }
        return parse(value)?.let { DocumentNumber(it, location) }
            ?: throw DocumentReadException.MalformedDocument.at(location, "Invalid numeric value '$value'")
    }

    private companion object {
        val JSON_NUMBER = Regex("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?")
        val YAML_ONLY_PLAIN_NUMBER = Regex("[-+]?0[oO][0-7](?:_?[0-7])*")
    }
}
