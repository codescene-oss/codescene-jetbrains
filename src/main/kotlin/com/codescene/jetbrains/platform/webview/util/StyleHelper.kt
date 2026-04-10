package com.codescene.jetbrains.platform.webview.util

import com.codescene.jetbrains.core.util.CwfThemeSourceInputs
import com.codescene.jetbrains.core.util.RgbColor
import com.codescene.jetbrains.core.util.buildCwfThemeCssVariables
import com.codescene.jetbrains.core.util.parseScrollbarHex
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefScrollbarsHelper
import java.awt.Color
import javax.swing.UIManager
import kotlin.math.pow

@Service(Service.Level.APP)
class StyleHelper {
    companion object {
        private const val MIN_LABEL_ON_BACKGROUND_CONTRAST = 4.5

        fun getInstance(): StyleHelper = ApplicationManager.getApplication().getService(StyleHelper::class.java)
    }

    /**
     * Generates a CSS string defining theme variables based on the current JetBrains IDE theme.
     * These variables are injected into a WebView to visually match the IDE's appearance.
     *
     * The method reads colors and font settings from IntelliJ's theme system
     * (`EditorColorsManager`, `UIManager`, and `JBColor`) and constructs a CSS `:root`
     * block containing custom properties.
     *
     * The generated CSS includes:
     * - `--cs-theme-editor-background`: Background aligned with the editor when it contrasts
     *   with [Label.foreground]; otherwise [Panel.background] so it pairs with general UI text.
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
            val rawEditorBackground = scheme.defaultBackground

            val defaultFont = UIManager.getFont("Label.font")
            val fontSize = defaultFont.size

            val textFg = UIManager.getColor("Label.foreground")
            val editorBackground =
                textFg?.let { fg ->
                    if (contrastRatio(rawEditorBackground, fg) < MIN_LABEL_ON_BACKGROUND_CONTRAST) {
                        UIManager.getColor("Panel.background") ?: rawEditorBackground
                    } else {
                        rawEditorBackground
                    }
                } ?: rawEditorBackground
            val linkFg = UIManager.getColor("Hyperlink.linkColor")
            val buttonBg = UIManager.getColor("Button.default.startBackground")
            val buttonFg = JBColor.namedColor("Button.default.foreground")
            val buttonSecondaryBg = UIManager.getColor("Button.default.endBackground")

            buildCwfThemeCssVariables(
                CwfThemeSourceInputs(
                    textForeground = textFg.toRgbColor(),
                    linkForeground = linkFg.toRgbColor(),
                    buttonForeground = buttonFg.toRgbColor(),
                    buttonBackground = buttonBg.toRgbColor(),
                    editorBackground = editorBackground.toRgbColor(),
                    scrollbarThumbHex = getScrollbarHex(),
                    buttonSecondaryBackground = buttonSecondaryBg.toRgbColor(),
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

private fun Color.toRgbColor(): RgbColor = RgbColor(red = red, green = green, blue = blue)

private fun contrastRatio(
    background: Color,
    foreground: Color,
): Double {
    val l1 = relativeLuminance(background)
    val l2 = relativeLuminance(foreground)
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}

private fun relativeLuminance(color: Color): Double {
    fun channel(c: Int): Double {
        val v = c / 255.0
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
}
