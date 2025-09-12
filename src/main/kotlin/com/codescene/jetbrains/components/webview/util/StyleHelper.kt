package com.codescene.jetbrains.components.webview.util

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

    private val opacityVariants = mapOf(
        1 to "03",
        3 to "08",
        7 to "12",
        10 to "1A",
        20 to "33",
        30 to "4D",
        40 to "66",
        50 to "80",
        60 to "99",
        70 to "B3",
        75 to "BF",
        80 to "CC",
        85 to "D9",
        90 to "E6"
    )

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
            val fontFamily = defaultFont.family

            val textFg = UIManager.getColor("Label.foreground")
            val linkFg = UIManager.getColor("Hyperlink.linkColor")
            val buttonBg = UIManager.getColor("Button.default.startBackground")
            val buttonFg = JBColor.namedColor("Button.default.foreground")
            val buttonSecondaryBg = UIManager.getColor("Button.default.endBackground")

            val textFgHex = toHex(textFg)
            val linkFgHex = toHex(linkFg)
            val buttonFgHex = toHex(buttonFg)
            val buttonBgHex = toHex(buttonBg)
            val editorBgHex = toHex(editorBackground)
            val scrollbarThumbHex = getScrollbarHex()
            val buttonSecondaryBgHex = toHex(buttonSecondaryBg)

            val sb = StringBuilder()
            sb.appendLine(":root {")
            sb.appendLine("  --cs-theme-editor-background: #$editorBgHex;")
            sb.appendLine("  --cs-theme-textLink-foreground: #$linkFgHex;")
            sb.appendLine("  --cs-theme-foreground: #$textFgHex;")
            sb.appendLine("  --cs-theme-panel-background: #$textFgHex;")
            sb.appendLine("  --cs-theme-textCodeBlock-background: #$editorBgHex;")
            sb.appendLine("  --cs-theme-scroll-bar-thumb: #$scrollbarThumbHex;")

//            sb.appendLine("  --cs-theme-font-family: '$fontFamily', sans-serif;") TODO
            sb.appendLine("  --cs-theme-font-size: ${fontSize}px;")
            sb.appendLine("  --cs-theme-editor-font-family: '$editorFontFamily', monospace;")
            sb.appendLine("  --cs-theme-editor-font-size: ${editorFontSize}px;")

            sb.appendLine("  --cs-theme-button-foreground: #$buttonFgHex;")
            sb.appendLine("  --cs-theme-button-background: #$buttonBgHex;")
            sb.appendLine("  --cs-theme-button-secondaryForeground: #$buttonFgHex;")
            sb.appendLine("  --cs-theme-button-secondaryBackground: #$buttonSecondaryBgHex;")

            opacityVariants.forEach { (key, value) ->
                sb.appendLine("  --cs-theme-button-foreground-$key: #$textFgHex$value;")
                sb.appendLine("  --cs-theme-button-background-$key: #$buttonBgHex$value;")
                sb.appendLine("  --cs-theme-foreground-$key: #$textFgHex$value;")
                sb.appendLine("  --cs-theme-button-secondaryBackground-$key: #$buttonSecondaryBgHex$value;")
            }

            sb.appendLine("}")
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun getScrollbarHex(): String {
        val css = JBCefScrollbarsHelper.buildScrollbarsStyle()

        val regex = """::-webkit-scrollbar-thumb\s*\{[^}]*background-color:\s*([^;]+);""".toRegex()
        val match = regex.find(css)

        match?.let {
            val rgba = match.groupValues[1].trim()
            val rgbaRegex = """rgba?\((\d+),\s*(\d+),\s*(\d+),?\s*([\d.]+)?\)""".toRegex()
            val rgbaMatch = rgbaRegex.find(rgba)

            rgbaMatch?.let {
                val r = rgbaMatch.groupValues[1].toInt().coerceIn(0, 255)
                val g = rgbaMatch.groupValues[2].toInt().coerceIn(0, 255)
                val b = rgbaMatch.groupValues[3].toInt().coerceIn(0, 255)
                val a = (rgbaMatch.groupValues.getOrNull(4)?.toFloatOrNull() ?: 1f).coerceIn(0f, 1f)

                return String.format("%02X%02X%02X%02X", r, g, b, (a * 255).toInt())
            }
        }

        return ""
    }
}