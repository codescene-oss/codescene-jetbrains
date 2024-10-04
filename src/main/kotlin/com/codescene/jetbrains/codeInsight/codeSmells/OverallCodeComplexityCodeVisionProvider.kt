package com.codescene.jetbrains.codeInsight.codeSmells

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.CodeSmell
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class OverallCodeComplexityCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Overall Code Complexity"
    override val id = "codeVision.codescene.overallCodeComplexity"
    override val name = "com.codescene.codeVision.overallCodeComplexity"

    override fun handleClick(editor: Editor, element: CodeSmell, event: MouseEvent?) {
        //TODO
    }
}