package com.codescene.jetbrains.platform.webview.util

import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.platform.webview.WebViewInitializer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project

private fun isUiTestDiagnosticsEnabled(): Boolean =
    "true".equals(System.getProperty("codescene.uiTestDiagnostics"), ignoreCase = true)

/**
 * Marks the JCEF Swing component for [view] with a stable name/accessibleName so that
 * Remote Robot UI tests can confirm the CWF webapp called handleInit (i.e. the React
 * webapp actually bootstrapped, not just that the JCEF shell exists).
 *
 * Only active when -Dcodescene.uiTestDiagnostics=true. No-op in normal runtime.
 */
fun markCwfInitializedForUiTests(
    project: Project,
    view: View,
) {
    if (!isUiTestDiagnosticsEnabled()) return
    val component = WebViewInitializer.getInstance(project).getBrowser(view)?.component ?: return
    ApplicationManager.getApplication().invokeLater {
        component.name = "codescene-cwf-initialized-${view.value}"
        component.accessibleContext.accessibleName = "CodeScene CWF initialized ${view.value}"
    }
}

fun markCodeHealthMonitorStateForUiTests(
    project: Project,
    activeJobCount: Int,
    deltaResultCount: Int,
) {
    if (!isUiTestDiagnosticsEnabled()) return
    val component = WebViewInitializer.getInstance(project).getBrowser(View.HOME)?.component ?: return
    val state =
        when {
            activeJobCount > 0 -> "running"
            deltaResultCount > 0 -> "has-results"
            else -> "clean"
        }
    val marker = "CodeScene Code Health Monitor $state"
    ApplicationManager.getApplication().invokeLater {
        component.accessibleContext.accessibleDescription = marker
        component.toolTipText = marker
    }
}
