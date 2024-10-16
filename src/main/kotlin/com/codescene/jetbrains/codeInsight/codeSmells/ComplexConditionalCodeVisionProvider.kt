package com.codescene.jetbrains.codeInsight.codeSmells

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.CodeSmell
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class ComplexConditionalCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Complex Conditional"
    override val id = "codeVision.codescene.complexConditional"
    override val name = "com.codescene.codeVision.complexConditional"

    override fun handleClick(editor: Editor, element: CodeSmell, event: MouseEvent?) {
        //TODO
    }
}