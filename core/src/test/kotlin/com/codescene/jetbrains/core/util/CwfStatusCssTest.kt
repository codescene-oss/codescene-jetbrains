package com.codescene.jetbrains.core.util

import org.junit.Assert.assertTrue
import org.junit.Test

class CwfStatusCssTest {
    @Test
    fun `css block contains success warning and issue variables as opaque hex`() {
        val css =
            buildCwfThemeCssVariables(
                CwfThemeCssInputs(
                    textForegroundHex = "111111",
                    linkForegroundHex = "222222",
                    buttonForegroundHex = "333333",
                    buttonBackgroundHex = "444444",
                    editorBackgroundHex = "555555",
                    scrollbarThumbHex = "66666666",
                    buttonSecondaryBackgroundHex = "777777",
                    fontSizePx = 12,
                    editorFontFamily = "JetBrains Mono",
                    editorFontSizePx = 13,
                    successHex = "89D185",
                    warningHex = "CCA700",
                    issueHex = "EE9D28",
                ),
            )

        val opaqueHexPattern = Regex("#[0-9A-F]{6};")
        assertTrue(css.contains(Regex("--cs-theme-success: ${opaqueHexPattern.pattern}")))
        assertTrue(css.contains(Regex("--cs-theme-warning: ${opaqueHexPattern.pattern}")))
        assertTrue(css.contains(Regex("--cs-theme-issue: ${opaqueHexPattern.pattern}")))
    }

    @Test
    fun `light theme css uses theme success and warning when readable and A15C00 issue`() {
        val lightEditorBg = RgbColor(255, 255, 255)
        val readableSuccess = RgbColor(0, 128, 0)
        val readableWarning = RgbColor(191, 136, 3)

        val css =
            buildCwfThemeCssVariables(
                CwfThemeSourceInputs(
                    textForeground = RgbColor(17, 17, 17),
                    linkForeground = RgbColor(34, 34, 34),
                    buttonForeground = RgbColor(51, 51, 51),
                    buttonBackground = RgbColor(68, 68, 68),
                    editorBackground = lightEditorBg,
                    scrollbarThumbHex = "66666666",
                    buttonSecondaryBackground = RgbColor(119, 119, 119),
                    fontSizePx = 12,
                    editorFontFamily = "JetBrains Mono",
                    editorFontSizePx = 13,
                    successForeground = readableSuccess,
                    warningForeground = readableWarning,
                    isBright = true,
                ),
            )

        assertTrue(css.contains("--cs-theme-success: #008000;"))
        assertTrue(css.contains("--cs-theme-warning: #BF8803;"))
        assertTrue(css.contains("--cs-theme-issue: #A15C00;"))
    }

    @Test
    fun `dark theme css uses theme success and warning when readable and EE9D28 issue`() {
        val darkEditorBg = RgbColor(0x1E, 0x1F, 0x22)
        val readableSuccess = RgbColor(137, 209, 133)
        val readableWarning = RgbColor(204, 167, 0)

        val css =
            buildCwfThemeCssVariables(
                CwfThemeSourceInputs(
                    textForeground = RgbColor(187, 187, 187),
                    linkForeground = RgbColor(100, 150, 200),
                    buttonForeground = RgbColor(200, 200, 200),
                    buttonBackground = RgbColor(68, 68, 68),
                    editorBackground = darkEditorBg,
                    scrollbarThumbHex = "66666666",
                    buttonSecondaryBackground = RgbColor(85, 85, 85),
                    fontSizePx = 12,
                    editorFontFamily = "JetBrains Mono",
                    editorFontSizePx = 13,
                    successForeground = readableSuccess,
                    warningForeground = readableWarning,
                    isBright = false,
                ),
            )

        assertTrue(css.contains("--cs-theme-success: #89D185;"))
        assertTrue(css.contains("--cs-theme-warning: #CCA700;"))
        assertTrue(css.contains("--cs-theme-issue: #EE9D28;"))
    }

    @Test
    fun `missing theme keys emit the brightness fallbacks for bright theme`() {
        val lightEditorBg = RgbColor(255, 255, 255)

        val css =
            buildCwfThemeCssVariables(
                CwfThemeSourceInputs(
                    textForeground = RgbColor(17, 17, 17),
                    linkForeground = RgbColor(34, 34, 34),
                    buttonForeground = RgbColor(51, 51, 51),
                    buttonBackground = RgbColor(68, 68, 68),
                    editorBackground = lightEditorBg,
                    scrollbarThumbHex = "66666666",
                    buttonSecondaryBackground = RgbColor(119, 119, 119),
                    fontSizePx = 12,
                    editorFontFamily = "JetBrains Mono",
                    editorFontSizePx = 13,
                    successForeground = null,
                    warningForeground = null,
                    isBright = true,
                ),
            )

        assertTrue(css.contains("--cs-theme-success: #388A34;"))
        assertTrue(css.contains("--cs-theme-warning: #BF8803;"))
        assertTrue(css.contains("--cs-theme-issue: #A15C00;"))
    }

    @Test
    fun `missing theme keys emit the brightness fallbacks for dark theme`() {
        val darkEditorBg = RgbColor(0x1E, 0x1F, 0x22)

        val css =
            buildCwfThemeCssVariables(
                CwfThemeSourceInputs(
                    textForeground = RgbColor(187, 187, 187),
                    linkForeground = RgbColor(100, 150, 200),
                    buttonForeground = RgbColor(200, 200, 200),
                    buttonBackground = RgbColor(68, 68, 68),
                    editorBackground = darkEditorBg,
                    scrollbarThumbHex = "66666666",
                    buttonSecondaryBackground = RgbColor(85, 85, 85),
                    fontSizePx = 12,
                    editorFontFamily = "JetBrains Mono",
                    editorFontSizePx = 13,
                    successForeground = null,
                    warningForeground = null,
                    isBright = false,
                ),
            )

        assertTrue(css.contains("--cs-theme-success: #89D185;"))
        assertTrue(css.contains("--cs-theme-warning: #CCA700;"))
        assertTrue(css.contains("--cs-theme-issue: #EE9D28;"))
    }

    @Test
    fun `low contrast theme keys emit the brightness fallbacks for bright theme`() {
        val lightEditorBg = RgbColor(255, 255, 255)
        val paleGreen = RgbColor(200, 255, 200)
        val paleAmber = RgbColor(255, 240, 200)

        assertTrue(contrastRatio(paleGreen, lightEditorBg) < 3.0)
        assertTrue(contrastRatio(paleAmber, lightEditorBg) < 3.0)

        val css =
            buildCwfThemeCssVariables(
                CwfThemeSourceInputs(
                    textForeground = RgbColor(17, 17, 17),
                    linkForeground = RgbColor(34, 34, 34),
                    buttonForeground = RgbColor(51, 51, 51),
                    buttonBackground = RgbColor(68, 68, 68),
                    editorBackground = lightEditorBg,
                    scrollbarThumbHex = "66666666",
                    buttonSecondaryBackground = RgbColor(119, 119, 119),
                    fontSizePx = 12,
                    editorFontFamily = "JetBrains Mono",
                    editorFontSizePx = 13,
                    successForeground = paleGreen,
                    warningForeground = paleAmber,
                    isBright = true,
                ),
            )

        assertTrue(css.contains("--cs-theme-success: #388A34;"))
        assertTrue(css.contains("--cs-theme-warning: #BF8803;"))
        assertTrue(css.contains("--cs-theme-issue: #A15C00;"))
    }

    @Test
    fun `low contrast theme keys emit the brightness fallbacks for dark theme`() {
        val darkEditorBg = RgbColor(0x1E, 0x1F, 0x22)
        val paleGreen = RgbColor(50, 70, 50)
        val paleAmber = RgbColor(60, 60, 50)

        assertTrue(contrastRatio(paleGreen, darkEditorBg) < 3.0)
        assertTrue(contrastRatio(paleAmber, darkEditorBg) < 3.0)

        val css =
            buildCwfThemeCssVariables(
                CwfThemeSourceInputs(
                    textForeground = RgbColor(187, 187, 187),
                    linkForeground = RgbColor(100, 150, 200),
                    buttonForeground = RgbColor(200, 200, 200),
                    buttonBackground = RgbColor(68, 68, 68),
                    editorBackground = darkEditorBg,
                    scrollbarThumbHex = "66666666",
                    buttonSecondaryBackground = RgbColor(85, 85, 85),
                    fontSizePx = 12,
                    editorFontFamily = "JetBrains Mono",
                    editorFontSizePx = 13,
                    successForeground = paleGreen,
                    warningForeground = paleAmber,
                    isBright = false,
                ),
            )

        assertTrue(css.contains("--cs-theme-success: #89D185;"))
        assertTrue(css.contains("--cs-theme-warning: #CCA700;"))
        assertTrue(css.contains("--cs-theme-issue: #EE9D28;"))
    }

    @Test
    fun `existing theme variables are unchanged by the status colours`() {
        val css =
            buildCwfThemeCssVariables(
                CwfThemeCssInputs(
                    textForegroundHex = "111111",
                    linkForegroundHex = "222222",
                    buttonForegroundHex = "333333",
                    buttonBackgroundHex = "444444",
                    editorBackgroundHex = "555555",
                    scrollbarThumbHex = "66666666",
                    buttonSecondaryBackgroundHex = "777777",
                    fontSizePx = 12,
                    editorFontFamily = "JetBrains Mono",
                    editorFontSizePx = 13,
                    successHex = "89D185",
                    warningHex = "CCA700",
                    issueHex = "EE9D28",
                ),
            )

        val expectedExistingLines =
            listOf(
                ":root {",
                "  --cs-theme-editor-background: #555555;",
                "  --cs-theme-textLink-foreground: #222222;",
                "  --cs-theme-foreground: #111111;",
                "  --cs-theme-panel-background: #111111;",
                "  --cs-theme-textCodeBlock-background: #555555;",
                "  --cs-theme-scroll-bar-thumb: #66666666;",
                "  --cs-theme-font-size: 12px;",
                "  --cs-theme-editor-font-family: 'JetBrains Mono', monospace;",
                "  --cs-theme-editor-font-size: 13px;",
                "  --cs-theme-button-foreground: #333333;",
                "  --cs-theme-button-background: #444444;",
                "  --cs-theme-button-secondaryForeground: #333333;",
                "  --cs-theme-button-secondaryBackground: #777777;",
            )

        val cssLines = css.lines()
        expectedExistingLines.forEach { expectedLine ->
            assertTrue(
                "Expected existing line '$expectedLine' not found in CSS",
                cssLines.contains(expectedLine),
            )
        }
    }
}
