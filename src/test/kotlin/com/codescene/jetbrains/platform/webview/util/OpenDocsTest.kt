package com.codescene.jetbrains.platform.webview.util

import com.codescene.jetbrains.core.contracts.ITelemetryService
import com.codescene.jetbrains.core.models.DocsEntryPoint
import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.models.view.DocsData
import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.webview.WebViewInitializer
import com.codescene.jetbrains.platform.webview.handler.CwfMessageHandler
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Test

class OpenDocsTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `openDocs updates existing docs browser and sends telemetry`() {
        val project = mockk<Project>()
        val browser = mockk<JBCefBrowser>()
        val initializer = mockk<WebViewInitializer>()
        val messageHandler = mockk<CwfMessageHandler>(relaxed = true)
        val telemetryService = mockk<ITelemetryService>(relaxed = true)
        val appServices = mockk<CodeSceneApplicationServiceProvider>()

        every { initializer.getBrowser(View.DOCS) } returns browser
        every { appServices.telemetryService } returns telemetryService

        mockkObject(WebViewInitializer.Companion)
        every { WebViewInitializer.getInstance(project) } returns initializer

        mockkObject(CwfMessageHandler.Companion)
        every { CwfMessageHandler.getInstance(project) } returns messageHandler

        mockkObject(CodeSceneApplicationServiceProvider.Companion)
        every { CodeSceneApplicationServiceProvider.getInstance() } returns appServices

        val docsData = DocsData(docType = "overall_complexity", fileData = FileMetaType(fileName = "src/Main.kt"))
        openDocs(docsData, project, DocsEntryPoint.CODE_VISION)

        verify(exactly = 1) { messageHandler.postMessage(View.DOCS, any(), browser) }
        verify(exactly = 1) { telemetryService.logUsage(TelemetryEvents.OPEN_DOCS_PANEL, any()) }
    }
}
