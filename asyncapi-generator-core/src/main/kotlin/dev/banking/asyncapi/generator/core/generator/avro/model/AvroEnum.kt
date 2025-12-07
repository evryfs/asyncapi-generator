package dev.banking.asyncapi.generator.core.generator.avro.model

data class AvroEnum(
    val namespace: String,
    val name: String,
    val doc: String?,
    val symbols: List<AvroUnionType>, // Using existing wrapper for convenience
    val default: String?
) : AvroSchema

