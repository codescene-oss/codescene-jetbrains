package com.codescene.jetbrains.platform.webview.util

import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefScrollbarsHelper
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.awt.Color
import java.awt.Font
import javax.swing.UIManager
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StyleHelperTest {
    private lateinit var scheme: EditorColorsScheme
    private lateinit var colorsManager: EditorColorsManager

    @Before
    fun setUp() {
        scheme = mockk()
        colorsManager = mockk()

        mockkStatic(EditorColorsManager::class)
        mockkStatic(UIManager::class)
        mockkStatic(JBColor::class)
        mockkStatic(JBCefScrollbarsHelper::class)

        every { EditorColorsManager.getInstance() } returns colorsManager
        every { colorsManager.globalScheme } returns scheme
        every { JBCefScrollbarsHelper.buildScrollbarsStyle() } returns ""
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `generateCssVariablesFromTheme emits success and warning from FILESTATUS_ADDED and Component warningFocusColor`() {
        val editorBg = Color(255, 255, 255)
        val labelFg = Color(17, 17, 17)
        val successColor = Color(0, 128, 0)
        val warningColor = Color(191, 136, 3)

        setupBasicTheme(editorBg, labelFg)
        every { scheme.getColor(ColorKey.find("FILESTATUS_ADDED")) } returns successColor
        every { UIManager.getColor("Component.warningFocusColor") } returns warningColor
        every { JBColor.isBright() } returns true

        val helper = StyleHelper()
        val css = helper.generateCssVariablesFromTheme()

        assertTrue(css.contains("--cs-theme-success: #008000;"))
        assertTrue(css.contains("--cs-theme-warning: #BF8803;"))
    }

    @Test
    fun `generateCssVariablesFromTheme emits light issue A15C00 when bright`() {
        val editorBg = Color(255, 255, 255)
        val labelFg = Color(17, 17, 17)

        setupBasicTheme(editorBg, labelFg)
        every { scheme.getColor(ColorKey.find("FILESTATUS_ADDED")) } returns null
        every { UIManager.getColor("Component.warningFocusColor") } returns null
        every { JBColor.isBright() } returns true

        val helper = StyleHelper()
        val css = helper.generateCssVariablesFromTheme()

        assertTrue(css.contains("--cs-theme-issue: #A15C00;"))
    }

    @Test
    fun `generateCssVariablesFromTheme emits dark issue EE9D28 when dark`() {
        val editorBg = Color(0x1E, 0x1F, 0x22)
        val labelFg = Color(187, 187, 187)

        setupBasicTheme(editorBg, labelFg)
        every { scheme.getColor(ColorKey.find("FILESTATUS_ADDED")) } returns null
        every { UIManager.getColor("Component.warningFocusColor") } returns null
        every { JBColor.isBright() } returns false

        val helper = StyleHelper()
        val css = helper.generateCssVariablesFromTheme()

        assertTrue(css.contains("--cs-theme-issue: #EE9D28;"))
    }

    @Test
    fun `missing theme keys fall back to brightness for bright theme`() {
        val editorBg = Color(255, 255, 255)
        val labelFg = Color(17, 17, 17)

        setupBasicTheme(editorBg, labelFg)
        every { scheme.getColor(ColorKey.find("FILESTATUS_ADDED")) } returns null
        every { UIManager.getColor("Component.warningFocusColor") } returns null
        every { JBColor.isBright() } returns true

        val helper = StyleHelper()
        val css = helper.generateCssVariablesFromTheme()

        assertTrue(css.contains("--cs-theme-success: #388A34;"))
        assertTrue(css.contains("--cs-theme-warning: #BF8803;"))
        assertTrue(css.contains("--cs-theme-issue: #A15C00;"))
    }

    @Test
    fun `missing theme keys fall back to brightness for dark theme`() {
        val editorBg = Color(0x1E, 0x1F, 0x22)
        val labelFg = Color(187, 187, 187)

        setupBasicTheme(editorBg, labelFg)
        every { scheme.getColor(ColorKey.find("FILESTATUS_ADDED")) } returns null
        every { UIManager.getColor("Component.warningFocusColor") } returns null
        every { JBColor.isBright() } returns false

        val helper = StyleHelper()
        val css = helper.generateCssVariablesFromTheme()

        assertTrue(css.contains("--cs-theme-success: #89D185;"))
        assertTrue(css.contains("--cs-theme-warning: #CCA700;"))
        assertTrue(css.contains("--cs-theme-issue: #EE9D28;"))
    }

    @Test
    fun `low contrast theme keys fall back to brightness for bright theme`() {
        val editorBg = Color(255, 255, 255)
        val labelFg = Color(17, 17, 17)
        val paleGreen = Color(200, 255, 200)
        val paleAmber = Color(255, 240, 200)

        setupBasicTheme(editorBg, labelFg)
        every { scheme.getColor(ColorKey.find("FILESTATUS_ADDED")) } returns paleGreen
        every { UIManager.getColor("Component.warningFocusColor") } returns paleAmber
        every { JBColor.isBright() } returns true

        val helper = StyleHelper()
        val css = helper.generateCssVariablesFromTheme()

        assertTrue(css.contains("--cs-theme-success: #388A34;"))
        assertTrue(css.contains("--cs-theme-warning: #BF8803;"))
        assertTrue(css.contains("--cs-theme-issue: #A15C00;"))
    }

    @Test
    fun `low contrast theme keys fall back to brightness for dark theme`() {
        val editorBg = Color(0x1E, 0x1F, 0x22)
        val labelFg = Color(187, 187, 187)
        val paleGreen = Color(50, 70, 50)
        val paleAmber = Color(60, 60, 50)

        setupBasicTheme(editorBg, labelFg)
        every { scheme.getColor(ColorKey.find("FILESTATUS_ADDED")) } returns paleGreen
        every { UIManager.getColor("Component.warningFocusColor") } returns paleAmber
        every { JBColor.isBright() } returns false

        val helper = StyleHelper()
        val css = helper.generateCssVariablesFromTheme()

        assertTrue(css.contains("--cs-theme-success: #89D185;"))
        assertTrue(css.contains("--cs-theme-warning: #CCA700;"))
        assertTrue(css.contains("--cs-theme-issue: #EE9D28;"))
    }

    @Test
    fun `try catch returns empty string on exception`() {
        every { EditorColorsManager.getInstance() } throws RuntimeException("Test exception")

        val helper = StyleHelper()
        val css = helper.generateCssVariablesFromTheme()

        assertTrue(css.isEmpty())
    }

    private fun setupBasicTheme(
        editorBg: Color,
        labelFg: Color,
    ) {
        every { scheme.defaultBackground } returns editorBg
        every { scheme.editorFontSize } returns 13
        every { scheme.editorFontName } returns "JetBrains Mono"

        val font = Font("Dialog", Font.PLAIN, 12)
        every { UIManager.getFont("Label.font") } returns font
        every { UIManager.getColor("Label.foreground") } returns labelFg
        every { UIManager.getColor("Panel.background") } returns editorBg
        every { UIManager.getColor("Hyperlink.linkColor") } returns Color(34, 34, 34)
        every { UIManager.getColor("Button.default.startBackground") } returns Color(68, 68, 68)
        every { UIManager.getColor("Button.default.endBackground") } returns Color(119, 119, 119)
        every { JBColor.namedColor("Button.default.foreground") } returns JBColor(Color(51, 51, 51), Color(51, 51, 51))
    }
}
