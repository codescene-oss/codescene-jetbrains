package com.codescene.jetbrains.core.git

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainLineBranchContextTest {
    @Test
    fun `isMainLineBranch uses configured baseline only`() {
        val ctx =
            MainLineBranchContext(
                configuredBaseline = "develop",
                defaultBranchFromOriginHead = "main",
                localBranchNames = setOf("main", "develop", "master"),
            )
        assertTrue(ctx.isMainLineBranch("develop"))
        assertFalse(ctx.isMainLineBranch("main"))
        assertFalse(ctx.isMainLineBranch("master"))
    }

    @Test
    fun `isMainLineBranch uses origin default when no config`() {
        val ctx =
            MainLineBranchContext(
                defaultBranchFromOriginHead = "main",
                localBranchNames = setOf("main", "develop", "master"),
            )
        assertTrue(ctx.isMainLineBranch("main"))
        assertFalse(ctx.isMainLineBranch("develop"))
    }

    @Test
    fun `isMainLineBranch falls back to static list when no default`() {
        val ctx = MainLineBranchContext(localBranchNames = setOf("master"))
        assertTrue(ctx.isMainLineBranch("master"))
        assertFalse(ctx.isMainLineBranch("feature"))
    }

    @Test
    fun `refsForMergeBaseProbe returns only configured baseline refs`() {
        val ctx =
            MainLineBranchContext(
                configuredBaseline = "develop",
                defaultBranchFromOriginHead = "main",
                localBranchNames = setOf("main", "develop"),
            )
        assertEquals(listOf("develop", "origin/develop"), ctx.refsForMergeBaseProbe())
    }

    @Test
    fun `refsForMergeBaseProbe returns only origin default when known`() {
        val ctx =
            MainLineBranchContext(
                defaultBranchFromOriginHead = "main",
                localBranchNames = setOf("main", "develop"),
            )
        assertEquals(listOf("main", "origin/main"), ctx.refsForMergeBaseProbe())
    }

    @Test
    fun `refsForMergeBaseProbe uses local static names when no default`() {
        val ctx = MainLineBranchContext(localBranchNames = setOf("master", "feature"))
        assertEquals(
            listOf("master", "origin/master"),
            ctx.refsForMergeBaseProbe(),
        )
    }
}
