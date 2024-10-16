package com.codescene.jetbrains.codeInsight.codeSmells

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.CodeSmell
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class ComplexMethodCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Complex Method"
    override val id = "codeVision.codescene.complexMethod"
    override val name = "com.codescene.codeVision.complexMethod"

    override fun handleClick(editor: Editor, element: CodeSmell, event: MouseEvent?) {
        //TODO
    }
}