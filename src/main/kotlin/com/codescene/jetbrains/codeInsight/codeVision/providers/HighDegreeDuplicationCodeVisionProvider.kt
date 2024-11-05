package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.HIGH_DEGREE_OF_CODE_DUPLICATION
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class HighDegreeDuplicationCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = HIGH_DEGREE_OF_CODE_DUPLICATION

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}