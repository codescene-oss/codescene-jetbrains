package com.codescene.jetbrains.platform.webview.util

import com.codescene.jetbrains.core.util.CwfThemeCssInputs
import com.codescene.jetbrains.core.util.buildCwfThemeCssVariables
import com.codescene.jetbrains.core.util.parseScrollbarHex
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefScrollbarsHelper
import java.awt.Color
import javax.swing.UIManager

@Service(Service.Level.APP)
class StyleHelper {
    companion object {
        fun getInstance(): StyleHelper = ApplicationManager.getApplication().getService(StyleHelper::class.java)
    }

    private fun toHex(c: Color): String = "%02X%02X%02X".format(c.red, c.green, c.blue)

    /**
     * Generates a CSS string defining theme variables based on the current JetBrains IDE theme.
     * These variables are injected into a WebView to visually match the IDE's appearance.
     *
     * The method reads colors and font settings from IntelliJ's theme system
     * (`EditorColorsManager`, `UIManager`, and `JBColor`) and constructs a CSS `:root`
     * block containing custom properties.
     *
     * The generated CSS includes:
     * - `--cs-theme-editor-background`: Background color for the editor area.
     * - `--cs-theme-textLink-foreground`: Text color for hyperlinks.
     * - `--cs-theme-foreground`: General foreground color (labels, text).
     * - `--cs-theme-panel-background`: Background color for panels.
     * - `--cs-theme-textCodeBlock-background`: Background color for code blocks.
     * - `--cs-theme-editor-font-family`: Font family used in the editor.
     * - `--cs-theme-editor-font-size`: Font size used in the editor.
     * - `--cs-theme-button-foreground`: Primary button text color.
     * - `--cs-theme-button-background`: Primary button background color.
     * - `--cs-theme-button-secondaryForeground`: Secondary button text color.
     * - `--cs-theme-button-secondaryBackground`: Secondary button background color.
     * - `--cs-theme-scroll-bar-thumb`: Scrollbar thumb background color.
     *
     * Note:
     * - The set of available theme keys is documented in:
     * [JDK.themeMetadata.json](https://github.com/JetBrains/intellij-community/blob/3e25b084a86c126b02041b11f1757e83edef5f19/platform/platform-resources/src/themes/metadata/JDK.themeMetadata.json).     */
    fun generateCssVariablesFromTheme(): String {
        return try {
            val scheme = EditorColorsManager.getInstance().globalScheme

            val editorFontSize = scheme.editorFontSize
            val editorFontFamily = scheme.editorFontName
            val editorBackground = scheme.defaultBackground

            val defaultFont = UIManager.getFont("Label.font")
            val fontSize = defaultFont.size

            val textFg = UIManager.getColor("Label.foreground")
            val linkFg = UIManager.getColor("Hyperlink.linkColor")
            val buttonBg = UIManager.getColor("Button.default.startBackground")
            val buttonFg = JBColor.namedColor("Button.default.foreground")
            val buttonSecondaryBg = UIManager.getColor("Button.default.endBackground")

            buildCwfThemeCssVariables(
                CwfThemeCssInputs(
                    textForegroundHex = toHex(textFg),
                    linkForegroundHex = toHex(linkFg),
                    buttonForegroundHex = toHex(buttonFg),
                    buttonBackgroundHex = toHex(buttonBg),
                    editorBackgroundHex = toHex(editorBackground),
                    scrollbarThumbHex = getScrollbarHex(),
                    buttonSecondaryBackgroundHex = toHex(buttonSecondaryBg),
                    fontSizePx = fontSize,
                    editorFontFamily = editorFontFamily,
                    editorFontSizePx = editorFontSize,
                ),
            )
        } catch (e: Exception) {
            ""
        }
    }

    private fun getScrollbarHex(): String = parseScrollbarHex(JBCefScrollbarsHelper.buildScrollbarsStyle())
}
