package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.model

data class PropertyModel(
    val name: String,
    val description: List<String>,
    val typeName: String,         // The final, complete type name, e.g., "String", "List<MyObject>?"
    val defaultValue: String?,      // The fully formatted default value, e.g., "MyEnum.VALUE", "\"hello\"", "null"
    val annotations: List<String>
)
