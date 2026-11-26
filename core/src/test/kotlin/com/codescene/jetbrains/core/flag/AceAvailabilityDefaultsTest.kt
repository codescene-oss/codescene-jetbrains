package com.codescene.jetbrains.core.flag

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.contracts.IAceService
import com.codescene.jetbrains.core.models.RefactoringRequest
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.core.models.shared.AutoRefactorConfig
import com.codescene.jetbrains.core.review.AceEntryCommand
import com.codescene.jetbrains.core.review.resolveAceEntryPointCommand
import com.codescene.jetbrains.core.review.shouldCheckRefactorableFunctions
import com.codescene.jetbrains.core.testdoubles.InMemorySettingsProvider
import com.codescene.jetbrains.core.util.AceEntryPoint
import com.codescene.jetbrains.core.util.autoRefactorConfigForDocsView
import com.codescene.jetbrains.core.util.isAceRefactorQuickFixAvailable
import com.codescene.jetbrains.core.util.resolveAcePreflightDecision
import com.codescene.jetbrains.core.util.toAutoRefactorConfig
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AceAvailabilityDefaultsTest {
    private val aceReadySettings = CodeSceneGlobalSettings(aceTokenConfigured = true, aceAcknowledged = true)

    @Test
    fun `preflight does not run by default even with a token`() {
        val decision = resolveAcePreflightDecision(tokenConfigured = true, token = "token", force = true)

        assertFalse(decision.shouldRun)
        assertEquals(null, decision.skippedStatus)
        assertEquals(null, decision.successStatus)
    }

    @Test
    fun `refactorable functions are not checked by default even with a token`() =
        runBlocking {
            val aceService = mockk<IAceService>(relaxed = true)

            val result = shouldCheckRefactorableFunctions(InMemorySettingsProvider(aceReadySettings), aceService, "kt")

            assertFalse(result)
            coVerify(exactly = 0) { aceService.runPreflight(any()) }
        }

    @Test
    fun `entry point command is skip by default even when acknowledged with a token`() {
        val request = RefactoringRequest("a.kt", null, mockk<FnToRefactor>(relaxed = true), AceEntryPoint.RETRY)

        assertEquals(AceEntryCommand.Skip, resolveAceEntryPointCommand(aceReadySettings, request))
    }

    @Test
    fun `refactor quick fix is unavailable by default even with a token`() {
        assertFalse(isAceRefactorQuickFixAvailable(aceTokenConfigured = true))
    }

    @Test
    fun `webview auto refactor config is off by default even with a token`() {
        assertEquals(AutoRefactorConfig(), toAutoRefactorConfig(aceReadySettings))
        assertEquals(
            AutoRefactorConfig(),
            autoRefactorConfigForDocsView(aceReadySettings, "docs_issues_complex_method", refactorTargetPresent = true),
        )
    }
}
