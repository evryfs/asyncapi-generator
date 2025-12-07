package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.model

data class PropertyModel(
    val name: String,
    val description: List<String>,
    val typeName: String,
    val defaultValue: String?,
    val annotations: List<String>
) {
    val isNullable: Boolean get() = typeName.endsWith("?")
    val baseType: String get() = typeName.removeSuffix("?")

    val docFirstLine: String? get() = description.firstOrNull()
    val docTailLines: List<String> get() = description.drop(1)
}
