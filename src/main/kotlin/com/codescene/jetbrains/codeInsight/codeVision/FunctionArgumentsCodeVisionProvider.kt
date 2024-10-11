package com.codescene.jetbrains.codeInsight.codeVision

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class FunctionArgumentsCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Excess Number of Function Arguments"
    override val id = "codeVision.codescene.excessFunctionArgs"
    override val name = "com.codescene.codeVision.excessFunctionArgs"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}