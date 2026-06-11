package com.codescene.jetbrains.platform.webview.util

import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.platform.webview.WebViewInitializer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project

private fun isUiTestDiagnosticsEnabled(): Boolean =
    System.getProperty("codescene.uiTestDiagnostics").equals("true", ignoreCase = true)

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
