package com.tietoevry.banking.asyncapi.generator.core.generator.avro.model

data class AvroUnion(
    val namespace: String,
    val name: String,
    val types: List<AvroUnionType> // e.g. ["com.example.CardPayment", "com.example.BankPayment"]
) : AvroSchema
