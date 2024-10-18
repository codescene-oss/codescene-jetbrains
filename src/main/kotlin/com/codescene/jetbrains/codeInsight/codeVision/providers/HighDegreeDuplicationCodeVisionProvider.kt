package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class HighDegreeDuplicationCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "High Degree of Code Duplication"
    override val id = "codeVision.codescene.highDegreeCodeDuplication"
    override val name = "com.codescene.codeVision.highDegreeCodeDuplication"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}