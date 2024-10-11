package com.codescene.jetbrains.codeInsight.codeVision

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class ComplexConditionalCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Complex Conditional"
    override val id = "codeVision.codescene.complexConditional"
    override val name = "com.codescene.codeVision.complexConditional"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}