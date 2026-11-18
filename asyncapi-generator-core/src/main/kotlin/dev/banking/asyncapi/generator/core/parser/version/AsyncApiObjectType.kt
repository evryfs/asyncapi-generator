package dev.banking.asyncapi.generator.core.parser.version

/**
 * The fixed-field AsyncAPI objects whose allowed members a parser profile defines.
 *
 * @param displayName the object's name as the specification writes it
 */
internal enum class AsyncApiObjectType(
    val displayName: String,
) {
    ASYNC_API("AsyncAPI Object"),
    INFO("Info Object"),
    CONTACT("Contact Object"),
    LICENSE("License Object"),
    SERVER("Server Object"),
    SERVER_VARIABLE("Server Variable Object"),
    CHANNEL("Channel Object"),
    PARAMETER("Parameter Object"),
    CORRELATION_ID("Correlation ID Object"),
    EXTERNAL_DOCUMENTATION("External Documentation Object"),
    TAG("Tag Object"),
    MESSAGE("Message Object"),
    MESSAGE_TRAIT("Message Trait Object"),
    MESSAGE_EXAMPLE("Message Example Object"),
    OPERATION("Operation Object"),
    OPERATION_TRAIT("Operation Trait Object"),
    OPERATION_REPLY("Operation Reply Object"),
    OPERATION_REPLY_ADDRESS("Operation Reply Address Object"),
    COMPONENTS("Components Object"),
    MULTI_FORMAT_SCHEMA("Multi Format Schema Object"),
    SECURITY_SCHEME("Security Scheme Object"),
    OAUTH_FLOWS("OAuth Flows Object"),
    OAUTH_FLOW("OAuth Flow Object"),
}
