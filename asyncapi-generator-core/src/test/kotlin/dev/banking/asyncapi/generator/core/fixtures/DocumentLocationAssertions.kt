package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.reader.DocumentNode
import dev.banking.asyncapi.generator.core.reader.DocumentObject
import dev.banking.asyncapi.generator.core.reader.InputDocument
import dev.banking.asyncapi.generator.core.reader.SourceLocation
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal fun InputDocument.assertNodeLocation(
    node: DocumentNode,
    path: String,
    line: Int,
) {
    assertSourceLocation(node.location, path, line)
}

internal fun InputDocument.assertMemberLocation(
    node: DocumentObject,
    memberName: String,
    path: String,
    line: Int,
) {
    val member = assertNotNull(node.member(memberName), "Expected document member $path")
    assertSourceLocation(member.keyLocation, path, line)
}

private fun InputDocument.assertSourceLocation(
    location: SourceLocation,
    path: String,
    line: Int,
) {
    assertEquals(source.id, location.sourceId)
    assertEquals(source.file, location.file)
    assertEquals(path, location.path)
    assertEquals(line, location.line)
    assertTrue(location.column >= 1)
}
