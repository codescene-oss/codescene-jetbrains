package com.codescene.jetbrains.core.util

private val cwfThemeOpacitySuffixes =
    mapOf(
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
        90 to "E6",
    )

data class RgbColor(
    val red: Int,
    val green: Int,
    val blue: Int,
)

data class CwfThemeSourceInputs(
    val textForeground: RgbColor,
    val linkForeground: RgbColor,
    val buttonForeground: RgbColor,
    val buttonBackground: RgbColor,
    val editorBackground: RgbColor,
    val scrollbarThumbHex: String,
    val buttonSecondaryBackground: RgbColor,
    val fontSizePx: Int,
    val editorFontFamily: String,
    val editorFontSizePx: Int,
)

data class CwfThemeCssInputs(
    val textForegroundHex: String,
    val linkForegroundHex: String,
    val buttonForegroundHex: String,
    val buttonBackgroundHex: String,
    val editorBackgroundHex: String,
    val scrollbarThumbHex: String,
    val buttonSecondaryBackgroundHex: String,
    val fontSizePx: Int,
    val editorFontFamily: String,
    val editorFontSizePx: Int,
)

fun toHex(color: RgbColor): String = "%02X%02X%02X".format(color.red, color.green, color.blue)

fun buildCwfThemeCssVariables(inputs: CwfThemeSourceInputs): String =
    buildCwfThemeCssVariables(
        CwfThemeCssInputs(
            textForegroundHex = toHex(inputs.textForeground),
            linkForegroundHex = toHex(inputs.linkForeground),
            buttonForegroundHex = toHex(inputs.buttonForeground),
            buttonBackgroundHex = toHex(inputs.buttonBackground),
            editorBackgroundHex = toHex(inputs.editorBackground),
            scrollbarThumbHex = inputs.scrollbarThumbHex,
            buttonSecondaryBackgroundHex = toHex(inputs.buttonSecondaryBackground),
            fontSizePx = inputs.fontSizePx,
            editorFontFamily = inputs.editorFontFamily,
            editorFontSizePx = inputs.editorFontSizePx,
        ),
    )

fun buildCwfThemeCssVariables(inputs: CwfThemeCssInputs): String {
    val textFg = inputs.textForegroundHex
    val linkFg = inputs.linkForegroundHex
    val buttonFg = inputs.buttonForegroundHex
    val buttonBg = inputs.buttonBackgroundHex
    val editorBg = inputs.editorBackgroundHex
    val scrollbarThumbHex = inputs.scrollbarThumbHex
    val buttonSecondaryBg = inputs.buttonSecondaryBackgroundHex
    val fontSize = inputs.fontSizePx
    val editorFontFamily = inputs.editorFontFamily
    val editorFontSize = inputs.editorFontSizePx

    val sb = StringBuilder()
    sb.appendLine(":root {")
    sb.appendLine("  --cs-theme-editor-background: #$editorBg;")
    sb.appendLine("  --cs-theme-textLink-foreground: #$linkFg;")
    sb.appendLine("  --cs-theme-foreground: #$textFg;")
    sb.appendLine("  --cs-theme-panel-background: #$textFg;")
    sb.appendLine("  --cs-theme-textCodeBlock-background: #$editorBg;")
    sb.appendLine("  --cs-theme-scroll-bar-thumb: #$scrollbarThumbHex;")
    sb.appendLine("  --cs-theme-font-size: ${fontSize}px;")
    sb.appendLine("  --cs-theme-editor-font-family: '$editorFontFamily', monospace;")
    sb.appendLine("  --cs-theme-editor-font-size: ${editorFontSize}px;")
    sb.appendLine("  --cs-theme-button-foreground: #$buttonFg;")
    sb.appendLine("  --cs-theme-button-background: #$buttonBg;")
    sb.appendLine("  --cs-theme-button-secondaryForeground: #$buttonFg;")
    sb.appendLine("  --cs-theme-button-secondaryBackground: #$buttonSecondaryBg;")

    cwfThemeOpacitySuffixes.forEach { (key, value) ->
        sb.appendLine("  --cs-theme-button-foreground-$key: #$textFg$value;")
        sb.appendLine("  --cs-theme-button-background-$key: #$buttonBg$value;")
        sb.appendLine("  --cs-theme-foreground-$key: #$textFg$value;")
        sb.appendLine("  --cs-theme-button-secondaryBackground-$key: #$buttonSecondaryBg$value;")
    }

    sb.appendLine("}")
    return sb.toString()
}
