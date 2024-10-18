package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class OverallCodeComplexityCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Overall Code Complexity"
    override val id = "codeVision.codescene.overallCodeComplexity"
    override val name = "com.codescene.codeVision.overallCodeComplexity"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}