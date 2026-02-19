package dev.banking.asyncapi.generator.core.constants

object RegexPatterns {

    /**
     * Matches standard email addresses using a simplified pattern.
     *
     * While not a full implementation of RFC 5322, this pattern provides sufficient structural
     * validation for contact information by ensuring the `local-part@domain.tld` format.
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc5322">RFC 5322: Internet Message Format</a>
     */
    val EMAIL = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

    /**
     * Matches absolute URIs with support for both web and socket protocols.
     *
     * Supported schemes: `http`, `https`, `ws`, `wss`.
     * This pattern follows the general URI structure defined in RFC 3986.
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc3986">RFC 3986: Uniform Resource Identifier (URI)</a>
     */
    val URL = Regex("""^(https?|wss?)://\S+$""")

    /**
     * Matches absolute URLs restricted to the HTTP and HTTPS protocols.
     *
     * Used specifically for fields where a web browser is expected to be the primary consumer,
     * such as contact or license URLs.
     */
    val HTTP_URL = Regex("""^(https?)://\S+$""")

    /**
     * Matches any character that is NOT a letter (A-Z, a-z) or a digit (0-9).
     * Typically used as a delimiter pattern to split strings into words for case conversion.
     */
    val NON_ALPHANUMERIC = Regex("[^A-Za-z0-9]")

    /**
     * Matches Semantic Versioning strings (SemVer 2.0.0).
     *
     * Validates that the version follows the `MAJOR.MINOR.PATCH` format, optionally followed
     * by a pre-release identifier.
     *
     * **Example:** `1.0.0`, `3.0.0-rc1`
     * @see <a href="https://semver.org/">Semantic Versioning 2.0.0</a>
     */
    val SEMANTIC_VERSION = Regex("""^\d+\.\d+\.\d+(-[A-Za-z0-9]+)?$""")

    /**
     * Matches generic version strings containing alphanumeric characters and common separators.
     *
     * Useful for user-defined versions in the `info` block that may not strictly adhere to SemVer.
     * Supported characters: `A-Z`, `a-z`, `0-9`, `.`, `-`, `_`.
     */
    val ALPHANUMERIC_VERSION = Regex("""^[A-Za-z0-9_.-]+$""")

    /**
     * Matches URI and URN strings according to RFC 3986.
     *
     * Requires a scheme starting with an alphabetic character, followed by a colon and
     * the scheme-specific part.
     *
     * **Example:** `urn:uuid:550e8400-e29b-41d4-a716-446655440000`
     */
    val URI = Regex("""^[a-zA-Z][a-zA-Z0-9+.-]*:.+$""")

    /**
     * Matches MIME (Multipurpose Internet Mail Extensions) Media Types.
     *
     * Validates the `type/subtype` structure used in `contentType` fields.
     *
     * **Example:** `application/json`, `application/vnd.apache.avro+json`
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc2045">RFC 2045: Media Types</a>
     */
    val MIME_TYPE = Regex("""^[a-zA-Z0-9!#$&^_.+-]+/[a-zA-Z0-9!#$&^_.+-]+$""")

    /**
     * Identifies and extracts parameter placeholders from strings.
     *
     * This pattern identifies variables enclosed in curly braces, such as those found in
     * Channel Addresses or Server URLs.
     *
     * **Capture Group 1:** Contains the name of the parameter without the braces.
     *
     * **Example:** In `users.{userId}`, matches `{userId}` and captures `userId`.
     * @see <a href="https://www.asyncapi.com/docs/reference/specification/v3.0.0#channelObject">AsyncAPI Channel Object</a>
     */
    val PARAMETER_PLACEHOLDER = Regex("""\{([^}]+)}""")

    /**
     * Matches a Server Hostname including potential AsyncAPI variables.
     *
     * This pattern is a superset of RFC 1123, allowing for standard hostname characters,
     * variable placeholders `{}`, ports, and path segments.
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc1123">RFC 1123: Requirements for Internet Hosts</a>
     */
    val HOSTNAME = Regex("""^[\w\-.:/{}\[\]%~@*!$'()+,;=?#]+(:\d+)?$""")

    /**
     * Matches general AsyncAPI Runtime Expressions.
     *
     * Validates the expression format starting with a `$` followed by the source and path.
     *
     * **Example:** `$message.header#/correlationId`
     * @see <a href="https://www.asyncapi.com/docs/reference/specification/v3.0.0#runtimeExpression">AsyncAPI Runtime Expression</a>
     */
    val RUNTIME_EXPRESSION_GENERAL = Regex("""^\$[a-zA-Z]+\.[a-zA-Z0-9_/#]+$""")

    /**
     * Matches AsyncAPI Runtime Expressions restricted to `message` or `context` sources.
     *
     * Used primarily for Parameter locations where only specific sources are permissible.
     *
     * **Example:** `$message.payload#/id`
     */
    val RUNTIME_EXPRESSION_PARAMETER = Regex("""^\$(message|context)(\.[A-Za-z0-9_-]+)*(#/[-A-Za-z0-9_/]+)?$""")
}
