package dev.banking.asyncapi.generator.core.generator.avro.model

data class AvroRecord(
    val namespace: String,
    val name: String,
    val doc: String?,
    val fields: List<AvroField>
) : AvroSchema
