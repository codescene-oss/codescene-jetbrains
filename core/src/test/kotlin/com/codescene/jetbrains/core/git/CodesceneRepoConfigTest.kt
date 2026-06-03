package com.codescene.jetbrains.core.git

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodesceneRepoConfigTest {
    @Test
    fun `parseBaselineBranchFromConfig returns branch when present`() {
        assertEquals("develop", parseBaselineBranchFromConfig("""{"baseline_branch":"develop"}"""))
    }

    @Test
    fun `parseBaselineBranchFromConfig ignores unknown keys`() {
        assertEquals(
            "main",
            parseBaselineBranchFromConfig("""{"baseline_branch":"main","other":1}"""),
        )
    }

    @Test
    fun `parseBaselineBranchFromConfig returns null for blank or invalid`() {
        assertNull(parseBaselineBranchFromConfig(null))
        assertNull(parseBaselineBranchFromConfig(""))
        assertNull(parseBaselineBranchFromConfig("{}"))
        assertNull(parseBaselineBranchFromConfig("not json"))
        assertNull(parseBaselineBranchFromConfig("""{"baseline_branch":""}"""))
    }
}
