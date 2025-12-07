package dev.banking.asyncapi.generator.core.model.exceptions

sealed class AsyncApiReadException(message: String) : Exception(message) {
    class InvalidYaml(message: String, source: String? = null) :
        AsyncApiReadException("$message ${source.orEmpty()}")
}
