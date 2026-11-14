package dev.banking.asyncapi.generator.core.generator.ir

/** Names a model cannot have without shadowing a Kotlin or Java built-in type. */
internal val builtInTypeNames =
    setOf(
        "String", "Int", "Integer", "Long", "Boolean", "Double", "Float", "Any", "Object",
        "UUID", "LocalDate", "LocalTime", "OffsetDateTime", "BigDecimal",
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
            listOf("$modelPackage.$name").takeIf {
                name != declaringType && name !in builtInTypeNames && '.' !in name && name.first().isUpperCase()
            }.orEmpty()
    }

/**
 * Imports of the Jakarta Validation and Jackson annotations among [annotations].
 *
 * @param target Annotation prefix including its use-site target, such as `@field:` in Kotlin.
 * @param accessMarker Text that shows an annotation uses `JsonProperty.Access`.
 */
internal fun annotationImports(
    annotations: List<String>,
    target: String,
    accessMarker: String,
): List<String> =
    annotations.flatMap { annotation ->
        val trimmed = annotation.trim()
        when {
            trimmed.startsWith("${target}Valid") -> listOf("jakarta.validation.Valid")
            trimmed.startsWith("@JsonProperty") || trimmed.startsWith("${target}JsonProperty") ->
                listOfNotNull(
                    "com.fasterxml.jackson.annotation.JsonProperty",
                    "com.fasterxml.jackson.annotation.JsonProperty.Access".takeIf { accessMarker in trimmed },
                )
            else ->
                constraintAnnotations
                    .firstOrNull { trimmed.startsWith("$target$it") }
                    ?.let { listOf("jakarta.validation.constraints.$it") }
                    .orEmpty()
        }
    }

private val constraintAnnotations =
    listOf("NotNull", "Size", "Pattern", "Min", "Max", "DecimalMin", "DecimalMax", "Email")
