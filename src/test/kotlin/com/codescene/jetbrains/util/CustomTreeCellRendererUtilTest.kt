package com.codescene.jetbrains.util

import com.codescene.jetbrains.CodeSceneIcons.METHOD_FIXED
import com.codescene.jetbrains.CodeSceneIcons.METHOD_IMPROVABLE
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthFinding
import com.codescene.jetbrains.components.codehealth.monitor.tree.NodeType
import com.intellij.icons.AllIcons
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CustomTreeCellRendererUtilTest {
    private lateinit var codeHealthFinding: CodeHealthFinding

    @Before
    fun setUp() {
        codeHealthFinding = mockk(relaxed = true)
    }

    @Test
    fun `getTooltip returns correct tooltip`() {
        every { codeHealthFinding.tooltip } returns ""
        every { codeHealthFinding.nodeType } returns NodeType.CODE_HEALTH_NEUTRAL

        val result = getTooltip(codeHealthFinding)
        assertEquals(UiLabelsBundle.message("unchangedFileHealth"), result)

        every { codeHealthFinding.nodeType } returns NodeType.CODE_HEALTH_INCREASE
        val resultIncrease = getTooltip(codeHealthFinding)
        assertEquals(UiLabelsBundle.message("increasingFileHealth"), resultIncrease)
    }

    @Test
    fun `getText returns correct text`() {
        every { codeHealthFinding.displayName } returns "MyFile"
        val result = getText(codeHealthFinding, false)
        assertEquals("MyFile", result)

        every { codeHealthFinding.additionalText } returns "10%"
        val resultWithPercentage = getText(codeHealthFinding, true)
        assertEquals(resultWithPercentage, "<html>MyFile<span style='color:gray;'> 10%</span></html>")
    }

    @Test
    fun `resolveMethodIcon returns correct icon`() {
        val resultNoTooltip = resolveMethodIcon("Function \"exampleFunction\"")
        assertEquals(AllIcons.Nodes.Method, resultNoTooltip)

        val resultDegrading = resolveMethodIcon("Function \"exampleFunction\" • 2 issues degrading code health")
        assertEquals(AllIcons.Nodes.Method, resultDegrading)

        val resultFixed = resolveMethodIcon("Function \"exampleFunction\" • 1 issue fixed")
        assertEquals(METHOD_FIXED, resultFixed)

        val resultBoth =
            resolveMethodIcon("Function \"exampleFunction\" • 1 issue fixed • 2 issues degrading code health")
        assertEquals(METHOD_IMPROVABLE, resultBoth)
    }
}
