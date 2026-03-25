package com.codescene.jetbrains.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CwfThemeCssUtilsTest {
    @Test
    fun `toHex converts rgb color to hex`() {
        assertEquals("0A14FF", toHex(RgbColor(10, 20, 255)))
    }

    @Test
    fun `buildCwfThemeCssVariables accepts source inputs`() {
        val css =
            buildCwfThemeCssVariables(
                CwfThemeSourceInputs(
                    textForeground = RgbColor(17, 17, 17),
                    linkForeground = RgbColor(34, 34, 34),
                    buttonForeground = RgbColor(51, 51, 51),
                    buttonBackground = RgbColor(68, 68, 68),
                    editorBackground = RgbColor(85, 85, 85),
                    scrollbarThumbHex = "66666666",
                    buttonSecondaryBackground = RgbColor(119, 119, 119),
                    fontSizePx = 12,
                    editorFontFamily = "JetBrains Mono",
                    editorFontSizePx = 13,
                ),
            )

        assertEquals(
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
                ),
            ),
            css,
        )
    }

    @Test
    fun `buildCwfThemeCssVariables produces expected root block`() {
        val inputs =
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
            )

        val css = buildCwfThemeCssVariables(inputs)

        assertEquals(
            """
            :root {
              --cs-theme-editor-background: #555555;
              --cs-theme-textLink-foreground: #222222;
              --cs-theme-foreground: #111111;
              --cs-theme-panel-background: #111111;
              --cs-theme-textCodeBlock-background: #555555;
              --cs-theme-scroll-bar-thumb: #66666666;
              --cs-theme-font-size: 12px;
              --cs-theme-editor-font-family: 'JetBrains Mono', monospace;
              --cs-theme-editor-font-size: 13px;
              --cs-theme-button-foreground: #333333;
              --cs-theme-button-background: #444444;
              --cs-theme-button-secondaryForeground: #333333;
              --cs-theme-button-secondaryBackground: #777777;
              --cs-theme-button-foreground-1: #11111103;
              --cs-theme-button-background-1: #44444403;
              --cs-theme-foreground-1: #11111103;
              --cs-theme-button-secondaryBackground-1: #77777703;
              --cs-theme-button-foreground-3: #11111108;
              --cs-theme-button-background-3: #44444408;
              --cs-theme-foreground-3: #11111108;
              --cs-theme-button-secondaryBackground-3: #77777708;
              --cs-theme-button-foreground-7: #11111112;
              --cs-theme-button-background-7: #44444412;
              --cs-theme-foreground-7: #11111112;
              --cs-theme-button-secondaryBackground-7: #77777712;
              --cs-theme-button-foreground-10: #1111111A;
              --cs-theme-button-background-10: #4444441A;
              --cs-theme-foreground-10: #1111111A;
              --cs-theme-button-secondaryBackground-10: #7777771A;
              --cs-theme-button-foreground-20: #11111133;
              --cs-theme-button-background-20: #44444433;
              --cs-theme-foreground-20: #11111133;
              --cs-theme-button-secondaryBackground-20: #77777733;
              --cs-theme-button-foreground-30: #1111114D;
              --cs-theme-button-background-30: #4444444D;
              --cs-theme-foreground-30: #1111114D;
              --cs-theme-button-secondaryBackground-30: #7777774D;
              --cs-theme-button-foreground-40: #11111166;
              --cs-theme-button-background-40: #44444466;
              --cs-theme-foreground-40: #11111166;
              --cs-theme-button-secondaryBackground-40: #77777766;
              --cs-theme-button-foreground-50: #11111180;
              --cs-theme-button-background-50: #44444480;
              --cs-theme-foreground-50: #11111180;
              --cs-theme-button-secondaryBackground-50: #77777780;
              --cs-theme-button-foreground-60: #11111199;
              --cs-theme-button-background-60: #44444499;
              --cs-theme-foreground-60: #11111199;
              --cs-theme-button-secondaryBackground-60: #77777799;
              --cs-theme-button-foreground-70: #111111B3;
              --cs-theme-button-background-70: #444444B3;
              --cs-theme-foreground-70: #111111B3;
              --cs-theme-button-secondaryBackground-70: #777777B3;
              --cs-theme-button-foreground-75: #111111BF;
              --cs-theme-button-background-75: #444444BF;
              --cs-theme-foreground-75: #111111BF;
              --cs-theme-button-secondaryBackground-75: #777777BF;
              --cs-theme-button-foreground-80: #111111CC;
              --cs-theme-button-background-80: #444444CC;
              --cs-theme-foreground-80: #111111CC;
              --cs-theme-button-secondaryBackground-80: #777777CC;
              --cs-theme-button-foreground-85: #111111D9;
              --cs-theme-button-background-85: #444444D9;
              --cs-theme-foreground-85: #111111D9;
              --cs-theme-button-secondaryBackground-85: #777777D9;
              --cs-theme-button-foreground-90: #111111E6;
              --cs-theme-button-background-90: #444444E6;
              --cs-theme-foreground-90: #111111E6;
              --cs-theme-button-secondaryBackground-90: #777777E6;
            }

            """.trimIndent(),
            css,
        )
    }
}
