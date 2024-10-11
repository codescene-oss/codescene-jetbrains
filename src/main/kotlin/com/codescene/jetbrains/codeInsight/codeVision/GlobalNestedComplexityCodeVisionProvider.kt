package com.codescene.jetbrains.codeInsight.codeVision

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class GlobalNestedComplexityCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Deep, Global Nested Complexity"
    override val id = "codeVision.codescene.deepGlobalNestedComplexity"
    override val name = "com.codescene.codeVision.deepGlobalNestedComplexity"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}