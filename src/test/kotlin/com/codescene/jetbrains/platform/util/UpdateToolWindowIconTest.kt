package com.codescene.jetbrains.platform.util

import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import javax.swing.Icon
import org.junit.After
import org.junit.Test

class UpdateToolWindowIconTest {
    @After
    fun tearDown() {
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
            ),
        )

        verify(exactly = 0) { toolWindow.setIcon(any()) }
    }

    @Test
    fun `uses base icon when notification is false`() {
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
            ),
        )

        verify(timeout = 1000) { toolWindow.setIcon(icon) }
    }

    @Test
    fun `uses indicator icon when notification is true`() {
        val project = mockk<Project>()
        val manager = mockk<ToolWindowManager>()
        val toolWindow = mockk<ToolWindow>(relaxed = true)
        val icon = mockk<Icon>()
        val indicator = mockk<Icon>()

        mockkStatic(ToolWindowManager::class)
        mockkStatic(ExecutionUtil::class)
        every { ToolWindowManager.getInstance(project) } returns manager
        every { manager.getToolWindow("CodeScene") } returns toolWindow
        every { ExecutionUtil.getIndicator(icon, 10, 10, any()) } returns indicator

        updateToolWindowIcon(
            UpdateToolWindowIconParams(
                baseIcon = icon,
                project = project,
                toolWindowId = "CodeScene",
                hasNotification = true,
            ),
        )

        verify(timeout = 1000) { toolWindow.setIcon(indicator) }
    }
}
