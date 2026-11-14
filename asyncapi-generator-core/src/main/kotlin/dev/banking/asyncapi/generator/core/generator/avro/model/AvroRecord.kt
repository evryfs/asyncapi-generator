package dev.banking.asyncapi.generator.core.generator.avro.model

internal data class AvroRecord(
    val namespace: String,
    val name: String,
    val doc: String?,
    val fields: List<AvroField>
) : AvroSchema
