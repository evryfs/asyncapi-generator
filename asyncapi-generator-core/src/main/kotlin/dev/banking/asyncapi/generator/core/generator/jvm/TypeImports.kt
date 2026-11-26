package dev.banking.asyncapi.generator.core.generator.jvm

import dev.banking.asyncapi.generator.core.generator.sourcemodel.TypeRef

/** Names of Kotlin and Java built-in types, which a named type never imports from the model package. */
private val builtInTypeNames =
    setOf(
        "String",
        "Int",
        "Integer",
        "Long",
        "Boolean",
        "Double",
        "Float",
        "Any",
        "Object",
        "UUID",
        "LocalDate",
        "LocalTime",
        "OffsetDateTime",
        "BigDecimal",
    )

/** The JVM class a scalar is imported from; null for implicitly imported types. */
internal val TypeRef.Scalar.importName: String?
    get() =
        when (this) {
            TypeRef.Scalar.UUID -> "java.util.UUID"
            TypeRef.Scalar.DATE -> "java.time.LocalDate"
            TypeRef.Scalar.TIME -> "java.time.LocalTime"
            TypeRef.Scalar.DATE_TIME -> "java.time.OffsetDateTime"
            TypeRef.Scalar.DECIMAL -> "java.math.BigDecimal"
            else -> null
        }

/**
 * The imports a source file declaring [declaringType] in [modelPackage] needs to use this type.
 *
 * @param modelPackage Package of the generated models, which named types are imported from.
 * @param declaringType Type the source file declares, which needs no import of itself.
 * @param listImport Import of the list type; null where lists need none.
 * @param mapImport Import of the map type; null where maps need none.
 */
internal fun TypeRef.imports(
    modelPackage: String,
    declaringType: String,
    listImport: String? = null,
    mapImport: String? = null,
): List<String> =
    when (this) {
        is TypeRef.Scalar -> listOfNotNull(importName)
        is TypeRef.ListOf ->
            listOfNotNull(listImport) + element.imports(modelPackage, declaringType, listImport, mapImport)
        is TypeRef.MapOf ->
            listOfNotNull(mapImport) + value.imports(modelPackage, declaringType, listImport, mapImport)
        is TypeRef.Named ->
            listOf("$modelPackage.$name")
                .takeIf {
                    name != declaringType && name !in builtInTypeNames && '.' !in name && name.first().isUpperCase()
                }.orEmpty()
        is TypeRef.External -> listOfNotNull(packageName?.let { "$it.$name" })
    }
