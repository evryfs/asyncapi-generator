package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Internal Protobuf compiler output selected from the source generator.
 *
 * `KOTLIN` generates the Java Protobuf messages required at runtime together
 * with the official Kotlin DSL sources.
 */
enum class ProtobufModelType(
    val configurationValue: String,
) {
    JAVA("java"),
    KOTLIN("kotlin"),
    ;
}
