package dev.banking.asyncapi.generator.core.generator.avro.model

data class AvroEnum(
    val namespace: String,
    val name: String,
    val doc: String?,
    val symbols: List<AvroEnumSymbol>,
    val default: String?,
) : AvroSchema
