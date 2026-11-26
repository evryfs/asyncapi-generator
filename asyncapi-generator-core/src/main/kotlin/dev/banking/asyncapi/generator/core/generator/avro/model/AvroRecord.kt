package dev.banking.asyncapi.generator.core.generator.avro.model

/**
 * An Avro record, as its `.avsc` template writes it.
 *
 * @property namespace the Avro namespace
 * @property name the record name
 * @property doc the description as JSON-escaped `doc` text; null without one
 * @property fields the fields in contract order
 */
internal data class AvroRecord(
    val namespace: String,
    val name: String,
    val doc: String?,
    val fields: List<AvroField>,
) : AvroSchema
