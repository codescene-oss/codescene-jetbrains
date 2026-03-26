package com.codescene.jetbrains.core.review

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AceRefactoringRunCoordinatorTest {
    @Test
    fun `only latest generation is authoritative`() {
        val coordinator = AceRefactoringRunCoordinator()
        val first = coordinator.nextGeneration()
        val second = coordinator.nextGeneration()
        assertTrue(second > first)
        assertFalse(coordinator.isLatest(first))
        assertTrue(coordinator.isLatest(second))
    }
}
