package com.ly.ideamcp.service

import com.ly.ideamcp.model.refactor.RenameRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * RefactoringService 单元测试
 */
class RefactoringServiceTest {

    @Test
    fun `RenameRequest should validate offset or line-column`() {
        // Valid with offset
        val request1 = RenameRequest(
            filePath = "test.kt",
            offset = 10,
            newName = "newName"
        )
        assertEquals(10, request1.offset)
        assertNull(request1.line)
        assertNull(request1.column)

        // Valid with line and column
        val request2 = RenameRequest(
            filePath = "test.kt",
            line = 5,
            column = 10,
            newName = "newName"
        )
        assertNull(request2.offset)
        assertEquals(5, request2.line)
        assertEquals(10, request2.column)

        // Invalid: no offset or line/column
        assertThrows<IllegalArgumentException> {
            RenameRequest(
                filePath = "test.kt",
                newName = "newName"
            )
        }
    }

    @Test
    fun `RenameRequest should validate new name`() {
        // Valid name
        val request1 = RenameRequest(
            filePath = "test.kt",
            offset = 10,
            newName = "validName"
        )
        assertEquals("validName", request1.newName)

        // Invalid: blank name
        assertThrows<IllegalArgumentException> {
            RenameRequest(
                filePath = "test.kt",
                offset = 10,
                newName = ""
            )
        }

        assertThrows<IllegalArgumentException> {
            RenameRequest(
                filePath = "test.kt",
                offset = 10,
                newName = "   "
            )
        }
    }

    @Test
    fun `RenameRequest should have default values`() {
        val request = RenameRequest(
            filePath = "test.kt",
            offset = 10,
            newName = "newName"
        )

        assertTrue(request.searchInComments)
        assertFalse(request.searchInStrings)
        assertFalse(request.preview)
    }

    @Test
    fun `RenameRequest should allow custom options`() {
        val request = RenameRequest(
            filePath = "test.kt",
            offset = 10,
            newName = "newName",
            searchInComments = false,
            searchInStrings = true,
            preview = true
        )

        assertFalse(request.searchInComments)
        assertTrue(request.searchInStrings)
        assertTrue(request.preview)
    }

    // Note: Integration tests that require IDEA project and PSI elements
    // should be in a separate test source set with proper IDEA test framework setup
}
