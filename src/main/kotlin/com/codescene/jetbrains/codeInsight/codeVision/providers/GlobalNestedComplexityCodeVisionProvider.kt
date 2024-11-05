package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.DEEP_GLOBAL_NESTED_COMPLEXITY
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class GlobalNestedComplexityCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = DEEP_GLOBAL_NESTED_COMPLEXITY

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}