package dev.banking.asyncapi.generator.core.parser.version

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AsyncApiObjectMembersTest {

    @Test
    fun `version 3 profile defines members for every ordinary object`() {
        val members = AsyncApiObjectType.entries.associateWith(AsyncApiParserProfile.V3_0::allowedMembers)

        assertEquals(AsyncApiObjectType.entries.toSet(), members.keys)
        assertTrue(members.values.all { it.isNotEmpty() })
    }

}
