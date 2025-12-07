package dev.banking.asyncapi.generator.core.model.exceptions

sealed class AsyncApiGeneratorException(message: String) : Exception(message) {
    class EmptyLanguageList :
        AsyncApiGeneratorException("The language list cannot be empty")

    class NullComponents :
        AsyncApiGeneratorException("The Components object cannot be null")

    class UnsupportedLanguage(language: String) :
        AsyncApiGeneratorException("The language $language is not supported")
}
