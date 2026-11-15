package dev.banking.asyncapi.generator.core.source

import java.io.File

/**
 * Collects the members of one object in document order and rejects duplicate keys.
 *
 * @param file file of the document, reported when a key is duplicated
 */
internal class DocumentObjectBuilder(
    private val file: File,
) {
    private val members = linkedMapOf<String, DocumentMember>()

    /** Adds the member [name]; [readValue] is called only when [name] is not a duplicate. */
    fun add(
        name: String,
        keyLocation: SourceLocation,
        readValue: () -> DocumentNode,
    ) {
        if (name in members) {
            throw DocumentReadException.DuplicateKey(file = file, memberName = name, location = keyLocation)
        }
        members[name] = DocumentMember(keyLocation = keyLocation, value = readValue())
    }

    fun build(location: SourceLocation): DocumentObject = DocumentObject(members, location)
}
