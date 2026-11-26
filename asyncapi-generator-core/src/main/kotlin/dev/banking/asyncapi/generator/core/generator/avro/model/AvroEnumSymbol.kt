package dev.banking.asyncapi.generator.core.generator.avro.model

/**
 * A symbol of an Avro enum.
 *
 * @property name the symbol
 * @property last whether it is the last symbol, so the template omits the trailing comma
 */
internal data class AvroEnumSymbol(
    val name: String,
    val last: Boolean,
)
