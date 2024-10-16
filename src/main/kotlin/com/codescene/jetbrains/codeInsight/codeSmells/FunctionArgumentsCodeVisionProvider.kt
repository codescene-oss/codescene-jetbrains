package com.codescene.jetbrains.codeInsight.codeSmells

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.CodeSmell
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class FunctionArgumentsCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Excess Number of Function Arguments"
    override val id = "codeVision.codescene.excessFunctionArgs"
    override val name = "com.codescene.codeVision.excessFunctionArgs"

    override fun handleClick(editor: Editor, element: CodeSmell, event: MouseEvent?) {
        //TODO
    }
}