package com.tietoevry.banking.asyncapi.generator.core.generator.java.model

data class PropertyModel(
    val name: String,
    val description: List<String>,
    val typeName: String,
    val getterName: String,
    val setterName: String,
    val annotations: List<String>
) {
    val docFirstLine: String? get() = description.firstOrNull()
    val docTailLines: List<String> get() = description.drop(1)
}
