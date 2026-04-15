package com.codescene.jetbrains.uitest

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.JCefBrowserFixture
import com.intellij.remoterobot.search.locators.byXpath
import com.intellij.remoterobot.steps.CommonSteps
import com.intellij.remoterobot.utils.waitFor
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CodeSceneHomeToolWindowUiTest {
    private val robotUrl = System.getProperty("robot.server.url", "http://127.0.0.1:8082")
    private lateinit var remoteRobot: RemoteRobot

    @Before
    fun waitForIde() {
        remoteRobot = RemoteRobot(robotUrl)
        waitFor(Duration.ofMinutes(3)) {
            try {
                remoteRobot.callJs<Boolean>("true")
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    @After
    fun closeProject() {
        try {
            CommonSteps(remoteRobot).closeProject()
        } catch (_: Exception) {
        }
    }

    @Test(timeout = 600_000)
    fun `home tool window shows webview or jcef fallback`() {
        val projectDir = TempGitProject.createWithJavaFile()
        val pathForOpen = projectDir.toAbsolutePath().normalize().toString().replace('\\', '/')

        val steps = CommonSteps(remoteRobot)
        steps.openProject(pathForOpen)
        waitFor(Duration.ofMinutes(5)) { !steps.isDumbMode() }

        activateCodeSceneToolWindow()

        waitFor(Duration.ofMinutes(2)) {
            webViewLoaded() || jcefUnsupportedVisible()
        }
        assertTrue(
            "Expected CodeScene tool window content (JCEF page or unsupported label)",
            webViewLoaded() || jcefUnsupportedVisible(),
        )
    }

    private fun webViewLoaded(): Boolean {
        return try {
            val candidates =
                listOf(JCefBrowserFixture.canvasLocator, JCefBrowserFixture.macLocator).flatMap { locator ->
                    try {
                        remoteRobot.findAll(JCefBrowserFixture::class.java, locator)
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            if (candidates.isEmpty()) {
                return false
            }
            val dom =
                try {
                    candidates.first().getDom()
                } catch (_: Exception) {
                    return false
                }
            dom.contains("JetBrains React Webview") || dom.contains("id=\"root\"")
        } catch (_: Exception) {
            false
        }
    }

    private fun jcefUnsupportedVisible(): Boolean =
        try {
            remoteRobot.findAll(
                ComponentFixture::class.java,
                byXpath("//div[contains(@text, 'JCEF is not supported')]"),
            ).isNotEmpty()
        } catch (_: Exception) {
            false
        }

    private fun activateCodeSceneToolWindow() {
        remoteRobot.runJs(
            """
            importClass(com.intellij.openapi.application.ApplicationManager)
            importClass(com.intellij.openapi.project.ProjectManager)
            importClass(com.intellij.openapi.wm.ToolWindowManager)
            importClass(java.lang.Runnable)

            const activate = new Runnable({
                run: function() {
                    const projects = ProjectManager.getInstance().getOpenProjects();
                    if (projects.length === 0) return;
                    const tw = ToolWindowManager.getInstance(projects[0]).getToolWindow("CodeScene");
                    if (tw !== null) tw.activate(null);
                }
            });
            ApplicationManager.getApplication().invokeLater(activate);
            """.trimIndent(),
            runInEdt = true,
        )
    }
}
