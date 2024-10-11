package com.codescene.jetbrains.codeInsight.codeVision

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.intellij.openapi.editor.Editor
import java.awt.event.MouseEvent

class FunctionsInSingleModuleCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = "Number of Functions in a Single Module"
    override val id = "codeVision.codescene.functionsInSingleModule"
    override val name = "com.codescene.codeVision.functionsInSingleModule"

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}