package com.codescene.jetbrains.codeInsight.codeSmells

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.CodeSmell
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class HighDegreeDuplicationCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "High Degree of Code Duplication"
    override val id = "codeVision.codescene.highDegreeCodeDuplication"
    override val name = "com.codescene.codeVision.highDegreeCodeDuplication"

    override fun handleClick(editor: Editor, element: CodeSmell, event: MouseEvent?) {
        //TODO
    }
}