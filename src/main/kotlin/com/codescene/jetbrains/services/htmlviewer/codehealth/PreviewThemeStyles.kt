package com.codescene.jetbrains.services.htmlviewer.codehealth

import com.codescene.jetbrains.util.webRgba
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color

// TODO[CWF-DELETE]: Remove once CWF is fully rolled out
object PreviewThemeStyles {

    fun createStylesheet(): String {
        val scheme = EditorColorsManager.getInstance().globalScheme
        val disabledColorString = JBUI.CurrentTheme.Label.disabledForeground().webRgba()
        val codeBackgroundColor = JBColor(Color(212, 222, 231, 255 / 4), Color(212, 222, 231, 25))
        val codeBackgroundColorString = codeBackgroundColor.webRgba(codeBackgroundColor.alpha / 255.0)
        val linkForegroundColorString = JBUI.CurrentTheme.Link.Foreground.ENABLED.webRgba()
        val backgroundColorGlobalString = scheme.defaultBackground.webRgba()
        val foregroundColorGlobalString = scheme.defaultForeground.webRgba()
        val fontSize = scheme.editorFontSize

        // language=CSS
        return """
            |body {
            |  background-color: $backgroundColorGlobalString;
            |  color: $foregroundColorGlobalString;
            |  font-size: ${fontSize + 1}px;
            |}
            |a {
            |  color: $linkForegroundColorString;
            |}
            |pre, code, hr {
            |  background-color: $codeBackgroundColorString;
            |}
            |#function-location:hover {
            |  background-color: $codeBackgroundColorString;
            |}
            |#line-number {
            |  color: $disabledColorString;
            |}
        """.trimMargin().trim()
    }
}