package com.codescene.jetbrains.components.webview.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
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
     * Generates CSS string defining theme variables based on current IDE theme.
     * Work in Progress implementation.
     */
    fun generateCssVariablesFromTheme(): String {
        return try {
            val scheme: EditorColorsScheme = EditorColorsManager.getInstance().globalScheme

            // TODO: Replace these with actual IDE API calls
            val editorBackground = scheme.defaultBackground

            val editorFontFamily = scheme.editorFontName
            val editorFontSize = scheme.editorFontSize

            val textForeground = UIManager.getColor("Label.foreground")
//            val linkForeground = UIManager.getColor("Label.linkForeground")

//            val codeBlockBackground = Color(45, 45, 48)

            val buttonForeground = UIManager.getColor("Button.foreground")
            val buttonBackground = UIManager.getColor("Button.background")

            val textFgHex = toHex(textForeground)
//            val buttonFgHex =
//                if (toHex(editorBackground) == BlueThemeColorName) toHex(buttonForeground) else DarkAndLightThemeBtnTextColorName
//            val buttonBgHex =
//                if (toHex(editorBackground) == DarkThemeColorName) DarkThemeFallbackSecondaryBg else toHex(
//                    buttonBackground
//                )
            val editorBgHex = toHex(editorBackground)
//            val textLinkFgHex = toHex(linkForeground)
//            val codeBlockBgHex = toHex(codeBlockBackground)
//            val secondaryButtonBgHex =
//                if (toHex(editorBackground) == DarkThemeColorName) DarkThemeFallbackSecondaryBg else buttonBgHex

            val sb = StringBuilder()
            sb.appendLine(":root {")
            sb.appendLine("  --cs-theme-editor-background: #$editorBgHex;")
//            sb.appendLine("  --cs-theme-textLink-foreground: #$textLinkFgHex;")
            sb.appendLine("  --cs-theme-foreground: #$textFgHex;") //
            sb.appendLine("  --cs-theme-panel-background: #$textFgHex;")
//            sb.appendLine("  --cs-theme-textCodeBlock-background: #$codeBlockBgHex;")
            sb.appendLine("  --cs-theme-editor-font-family: '$editorFontFamily', monospace;")
            sb.appendLine("  --cs-theme-editor-font-size: ${editorFontSize}px;")
//            sb.appendLine("  --cs-theme-button-foreground: #$buttonFgHex;")
//            sb.appendLine("  --cs-theme-button-background: #$buttonBgHex;")
//            sb.appendLine("  --cs-theme-button-secondaryForeground: #$buttonFgHex;")
//            sb.appendLine("  --cs-theme-button-secondaryBackground: #$secondaryButtonBgHex;")

            opacityVariants.forEach { (key, value) ->
                sb.appendLine("  --cs-theme-button-foreground-$key: #$textFgHex$value;")
//                sb.appendLine("  --cs-theme-button-background-$key: #$buttonBgHex$value;")
                sb.appendLine("  --cs-theme-foreground-$key: #$textFgHex$value;")
//                sb.appendLine("  --cs-theme-button-secondaryBackground-$key: #$secondaryButtonBgHex$value;")
            }

            sb.appendLine("}")
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }
}