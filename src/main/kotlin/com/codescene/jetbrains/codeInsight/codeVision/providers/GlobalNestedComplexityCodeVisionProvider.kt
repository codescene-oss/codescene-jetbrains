package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class GlobalNestedComplexityCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Deep, Global Nested Complexity"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}