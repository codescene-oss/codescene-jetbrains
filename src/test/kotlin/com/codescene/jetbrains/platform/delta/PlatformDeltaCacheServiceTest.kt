package com.codescene.jetbrains.platform.delta

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.core.delta.DeltaCacheEntry
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.telemetry.CodeHealthMonitorTelemetryState
import com.codescene.jetbrains.platform.webview.util.updateMonitor
import com.intellij.openapi.project.Project
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class PlatformDeltaCacheServiceTest {
    private lateinit var project: Project
    private lateinit var service: PlatformDeltaCacheService

    @Before
    fun setup() {
        project = mockk(relaxed = true)
        mockkStatic("com.codescene.jetbrains.platform.webview.util.UpdateMonitorKt")
        every { updateMonitor(any()) } just Runs

        mockkObject(CodeSceneProjectServiceProvider.Companion)
        every { CodeSceneProjectServiceProvider.getInstance(project) } returns mockk(relaxed = true)
        mockkObject(CodeHealthMonitorTelemetryState.Companion)
        every { CodeHealthMonitorTelemetryState.getInstance(project) } returns
            mockk(relaxed = true) {
                every { toolWindowVisible } returns true
            }

        service = PlatformDeltaCacheService(project)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `put triggers updateMonitor immediately after caching delta`() {
        val delta =
            mockk<Delta>(relaxed = true) {
                every { scoreChange } returns 1.0
            }
        val entry = DeltaCacheEntry("/path/test.kt", "old", "new", delta)

        service.put(entry)

        verify(exactly = 1) { updateMonitor(project) }
    }

    @Test
    fun `multiple puts trigger updateMonitor after each one`() {
        val delta =
            mockk<Delta>(relaxed = true) {
                every { scoreChange } returns 1.0
            }

        service.put(DeltaCacheEntry("/path/a.kt", "old", "new", delta))
        service.put(DeltaCacheEntry("/path/b.kt", "old", "new", delta))
        service.put(DeltaCacheEntry("/path/c.kt", "old", "new", delta))

        verify(exactly = 3) { updateMonitor(project) }
    }

    @Test
    fun `invalidate triggers updateMonitor`() {
        val delta =
            mockk<Delta>(relaxed = true) {
                every { scoreChange } returns 1.0
            }
        service.put(DeltaCacheEntry("/path/test.kt", "old", "new", delta))

        service.invalidate("/path/test.kt")

        verify(exactly = 2) { updateMonitor(project) }
    }
}
