package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class NestedComplexityCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Deep, Nested Complexity"
    override val id = "codeVision.codescene.deepNestedComplexity"
    override val name = "com.codescene.codeVision.deepNestedComplexity"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}