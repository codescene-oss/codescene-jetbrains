package com.codescene.jetbrains.codeInsight.codeSmells

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.CodeSmell
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class FunctionsInSingleModuleCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Number of Functions in a Single Module"
    override val id = "codeVision.codescene.functionsInSingleModule"
    override val name = "com.codescene.codeVision.functionsInSingleModule"

    override fun handleClick(editor: Editor, element: CodeSmell, event: MouseEvent?) {
        //TODO
    }
}