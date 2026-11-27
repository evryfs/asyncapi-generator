package dev.banking.asyncapi.generator.core

/**
 * A generation that cannot start or did not complete. The message is written for the user: it names
 * settings, files and contract locations the way the user wrote them.
 *
 * @param message what went wrong
 * @param cause the failure behind it
 */
sealed class AsyncApiGenerationException(
    message: String,
    cause: Throwable,
) : Exception(message, cause) {
    /**
     * A setting is missing or invalid; nothing was read or written.
     *
     * @param message which setting is wrong and why
     * @param cause the failed check
     */
    class InvalidSettings internal constructor(
        message: String,
        cause: Throwable,
    ) : AsyncApiGenerationException(message, cause)

    /**
     * Reading, checking or writing failed: a missing or invalid contract, or a file that could not be written.
     *
     * @param message what failed, with the contract location when there is one
     * @param cause the failure
     */
    class Failed internal constructor(
        message: String,
        cause: Throwable,
    ) : AsyncApiGenerationException(message, cause)
}
