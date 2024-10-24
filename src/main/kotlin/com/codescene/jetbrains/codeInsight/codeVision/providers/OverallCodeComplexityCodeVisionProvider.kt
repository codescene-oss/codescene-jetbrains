package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.OVERALL_CODE_COMPLEXITY
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class OverallCodeComplexityCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = OVERALL_CODE_COMPLEXITY

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}