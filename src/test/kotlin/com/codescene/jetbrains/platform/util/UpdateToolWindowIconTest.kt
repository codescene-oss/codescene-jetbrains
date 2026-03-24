package com.codescene.jetbrains.platform.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.awt.Color
import javax.swing.Icon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateToolWindowIconTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `does not update icon when tool window is missing`() {
        val project = mockk<Project>()
        val manager = mockk<ToolWindowManager>()
        val toolWindow = mockk<ToolWindow>(relaxed = true)
        val icon = mockk<Icon>()

        mockkStatic(ToolWindowManager::class)
        every { ToolWindowManager.getInstance(project) } returns manager
        every { manager.getToolWindow("CodeScene") } returns null

        updateToolWindowIcon(
            UpdateToolWindowIconParams(
                baseIcon = icon,
                project = project,
                toolWindowId = "CodeScene",
                hasNotification = false,
                badgeColor = Color.RED,
            ),
        )

        verify(exactly = 0) { toolWindow.setIcon(any()) }
    }

    @Test
    fun `uses base icon when notification is false`() =
        runTest(testDispatcher) {
            val project = mockk<Project>()
            val manager = mockk<ToolWindowManager>()
            val toolWindow = mockk<ToolWindow>(relaxed = true)
            val icon = mockk<Icon>()

            mockkStatic(ToolWindowManager::class)
            every { ToolWindowManager.getInstance(project) } returns manager
            every { manager.getToolWindow("CodeScene") } returns toolWindow

            updateToolWindowIcon(
                UpdateToolWindowIconParams(
                    baseIcon = icon,
                    project = project,
                    toolWindowId = "CodeScene",
                    hasNotification = false,
                    badgeColor = Color.RED,
                ),
            )

            advanceUntilIdle()
            verify { toolWindow.setIcon(icon) }
        }
}
