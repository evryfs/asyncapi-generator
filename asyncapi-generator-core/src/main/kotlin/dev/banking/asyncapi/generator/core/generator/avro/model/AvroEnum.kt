package dev.banking.asyncapi.generator.core.generator.avro.model

/**
 * An Avro enum, as its `.avsc` template writes it.
 *
 * @property namespace the Avro namespace
 * @property name the enum name
 * @property doc the description as JSON-escaped `doc` text; null without one
 * @property symbols the symbols in contract order
 * @property default the default symbol; null without one
 */
internal data class AvroEnum(
    val namespace: String,
    val name: String,
    val doc: String?,
    val symbols: List<AvroEnumSymbol>,
    val default: String?,
) : AvroSchema
