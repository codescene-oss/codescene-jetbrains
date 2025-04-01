package com.codescene.jetbrains.util

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.config.global.MonitorTreeSortOptions
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

class SortDeltaFindingsTest {
    private lateinit var mockApplication: Application
    private lateinit var mockSettingsStore: CodeSceneGlobalSettingsStore
    private val mockDeltaResults = ConcurrentHashMap(
        mapOf(
            "aFile" to mockk<Delta>(relaxed = true) {
                every { oldScore } returns Optional.of(3.21)
                every { newScore } returns Optional.of(5.32)
            },
            "bFile" to mockk<Delta>(relaxed = true) {
                every { oldScore } returns Optional.of(5.43)
                every { newScore } returns Optional.of(9.32)
            },
            "cFile" to mockk<Delta>(relaxed = true) {
                every { oldScore } returns Optional.of(9.32)
                every { newScore } returns Optional.of(1.93)
            })
    )

    @Before
    fun setUp() {
        mockApplication = mockk()
        mockSettingsStore = mockk()

        every { mockApplication.getService(CodeSceneGlobalSettingsStore::class.java) } returns mockSettingsStore

        mockkStatic(ApplicationManager::class)
        every { ApplicationManager.getApplication() } returns mockApplication
    }

    @Test
    fun `sortDeltaFindings sorts correctly if type is FILE_NAME`() {
        every { mockSettingsStore.state.monitorTreeSortOption } returns MonitorTreeSortOptions.FILE_NAME

        val result = sortDeltaFindings(mockDeltaResults)

        assertEquals("aFile", result[0].key)
        assertEquals("bFile", result[1].key)
        assertEquals("cFile", result[2].key)
    }

    @Test
    fun `sortDeltaFindings sorts correctly if type is SCORE_DESCENDING`() {
        every { mockSettingsStore.state.monitorTreeSortOption } returns MonitorTreeSortOptions.SCORE_DESCENDING

        val result = sortDeltaFindings(mockDeltaResults)

        assertEquals("bFile", result[0].key)
        assertEquals("aFile", result[1].key)
        assertEquals("cFile", result[2].key)
    }

    @Test
    fun `sortDeltaFindings sorts correctly if type is SCORE_ASCENDING`() {
        every { mockSettingsStore.state.monitorTreeSortOption } returns MonitorTreeSortOptions.SCORE_ASCENDING

        val result = sortDeltaFindings(mockDeltaResults)

        assertEquals("cFile", result[0].key)
        assertEquals("aFile", result[1].key)
        assertEquals("bFile", result[2].key)
    }
}