package com.codescene.jetbrains.core.git

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClosestMainLineMergeBaseTest {
    @Test
    fun `resolveClosestMainLineMergeBase returns null when no merge base`() {
        assertNull(
            resolveClosestMainLineMergeBase(
                isAncestor = { _, _ -> false },
                mergeBaseForRef = { null },
            ),
        )
    }

    @Test
    fun `selectClosestMergeBase returns single candidate`() {
        assertEquals(
            "only",
            selectClosestMergeBase(listOf("only")) { _, _ -> false },
        )
    }

    @Test
    fun `selectClosestMergeBase picks descendant when chain is linear`() {
        val ordered = listOf("old", "mid", "new")
        val isAncestor: (String, String) -> Boolean = { a, d ->
            when {
                a == "old" && d == "mid" -> true
                a == "old" && d == "new" -> true
                a == "mid" && d == "new" -> true
                else -> false
            }
        }
        assertEquals("new", selectClosestMergeBase(ordered, isAncestor))
    }

    @Test
    fun `selectClosestMergeBase falls back to first when candidates are incomparable`() {
        val ordered = listOf("a", "b")
        val isAncestor: (String, String) -> Boolean = { _, _ -> false }
        assertEquals("a", selectClosestMergeBase(ordered, isAncestor))
    }

    @Test
    fun `collectOrderedUniqueMergeBases preserves probe order and dedupes`() {
        val seen = mutableListOf<String>()
        val bases =
            collectOrderedUniqueMergeBases { ref ->
                seen.add(ref)
                when (ref) {
                    "main" -> "sha1"
                    "origin/main" -> "sha1"
                    "master" -> "sha2"
                    else -> null
                }
            }
        assertEquals(listOf("sha1", "sha2"), bases)
        assertEquals("main", seen.first())
        assertEquals("origin/main", seen[1])
    }

    @Test
    fun `resolveClosestMainLineMergeBase picks newest among main and develop bases`() {
        val mergeBases = mapOf("main" to "mainMb", "develop" to "developMb")
        val result =
            resolveClosestMainLineMergeBase(
                isAncestor = { a, d ->
                    a == "mainMb" && d == "developMb"
                },
                mergeBaseForRef = { ref -> mergeBases[ref] },
            )
        assertEquals("developMb", result)
    }
}
